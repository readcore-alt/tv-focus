package com.custom.tvfocus;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class FocusAccessibilityService extends AccessibilityService {

    private static final String APP_PROTEGIDA = "com.custom.tvbrowser";

    // Apps reales a forzar detención (sin meter a Settings para evitar el botón gris)
    private static final String[] APPS_A_CERRAR = {
        "com.universal.manager",     // Gestor pesado (~82 MB)
        "com.android.vending",       // Play Store (~87 MB)
        "com.google.android.katniss", // Assistant (~64 MB)
        "com.example.tvexplorer"     // Servidor (~36 MB)
    };

    private boolean isCleaning = false;
    private int currentTargetIndex = 0;
    private long lastCleanTimestamp = 0;
    private static final long COOLDOWN_MS = 60000; // 1 minuto de descanso para evitar bucles

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        CharSequence pkgName = event.getPackageName();
        if (pkgName == null) return;
        String currentPkg = pkgName.toString();

        long now = System.currentTimeMillis();

        // 1. Detectar entrada al navegador (con protección anti-bucle de 60 segundos)
        if (APP_PROTEGIDA.equals(currentPkg)) {
            if (!isCleaning && (now - lastCleanTimestamp > COOLDOWN_MS)) {
                isCleaning = true;
                lastCleanTimestamp = now;
                currentTargetIndex = 0;

                // Limpieza silenciosa de memoria inicial
                purgeCaches();

                // Arrancar la purga con un respiro de 300ms
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        killNextTarget();
                    }
                }, 300);
            }
            return;
        }

        // 2. Automatizar los clics dentro de la pantalla de Ajustes
        if (isCleaning && "com.android.tv.settings".equals(currentPkg)) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    handleSettingsClick();
                }
            }, 250);
        }
    }

    private void purgeCaches() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            am.killBackgroundProcesses("com.android.tv.settings");
            for (String pkg : APPS_A_CERRAR) {
                am.killBackgroundProcesses(pkg);
            }
        }
    }

    private void killNextTarget() {
        if (currentTargetIndex >= APPS_A_CERRAR.length) {
            // FIN DE LA LIMPIEZA: Volver al navegador limpiamente
            isCleaning = false;
            Intent intent = getPackageManager().getLaunchIntentForPackage(APP_PROTEGIDA);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(intent);
            }
            return;
        }

        String target = APPS_A_CERRAR[currentTargetIndex];
        currentTargetIndex++;

        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + target));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void handleSettingsClick() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        // 1. Buscar "Forzar detención"
        List<AccessibilityNodeInfo> forceStopButtons = root.findAccessibilityNodeInfosByText("Forzar detención");
        if (forceStopButtons.isEmpty()) {
            forceStopButtons = root.findAccessibilityNodeInfosByText("Force stop");
        }

        for (AccessibilityNodeInfo node : forceStopButtons) {
            if (node.isClickable() && node.isEnabled()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return;
            }
        }

        // 2. Buscar "Aceptar" / "OK" en el diálogo emergente
        List<AccessibilityNodeInfo> okButtons = root.findAccessibilityNodeInfosByText("Aceptar");
        if (okButtons.isEmpty()) {
            okButtons = root.findAccessibilityNodeInfosByText("OK");
        }

        for (AccessibilityNodeInfo node : okButtons) {
            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        killNextTarget();
                    }
                }, 200);
                return;
            }
        }

        // Si el botón estaba desactivado (ya detenida), pasar a la siguiente de inmediato
        killNextTarget();
    }

    @Override
    public void onInterrupt() {
        isCleaning = false;
    }
}
