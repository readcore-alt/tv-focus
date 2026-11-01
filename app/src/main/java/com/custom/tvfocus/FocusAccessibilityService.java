package com.custom.tvfocus;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class FocusAccessibilityService extends AccessibilityService {

    // 1. LA APP PROTEGIDA (Cuando abras esta, salta la limpieza)
    private static final String APP_PROTEGIDA = "com.custom.tvbrowser";

    // 2. EL LAUNCHER INTACTO (Nunca tocar)
    private static final String LAUNCHER_TV = "com.google.android.apps.tv.launcherx";

    // 3. LA LISTA NEGRA (Los que se cerrarán)
    private static final String[] APPS_A_CERRAR = {
        "com.android.tv.settings",   // Ajustes (~92 MB)
        "com.android.vending",       // Play Store (~87 MB)
        "com.google.android.katniss", // Assistant (~64 MB)
        "com.example.tvexplorer"     // Servidor (~36 MB)
    };

    private boolean isCleaning = false;
    private int currentTargetIndex = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        CharSequence pkgName = event.getPackageName();
        if (pkgName == null) return;
        String currentPkg = pkgName.toString();

        // Si entraste a tu navegador y no estamos limpiando: ¡INICIAR PURGA!
        if (APP_PROTEGIDA.equals(currentPkg) && !isCleaning) {
            isCleaning = true;
            currentTargetIndex = 0;
            // Barrido rápido inicial de procesos en caché
            purgeBackgroundCaches();
            // Ejecutar cierre forzado del primer objetivo
            killNextTarget();
            return;
        }

        // Si estamos en la pantalla de Ajustes automatizando el clic de "Forzar detención"
        if (isCleaning && "com.android.tv.settings".equals(currentPkg)) {
            handleSettingsClick();
        }
    }

    private void purgeBackgroundCaches() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            for (String pkg : APPS_A_CERRAR) {
                am.killBackgroundProcesses(pkg);
            }
        }
    }

    private void killNextTarget() {
        if (currentTargetIndex >= APPS_A_CERRAR.length) {
            // Ya terminamos la lista: regresar al navegador
            isCleaning = false;
            Intent intent = getPackageManager().getLaunchIntentForPackage(APP_PROTEGIDA);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            return;
        }

        String target = APPS_A_CERRAR[currentTargetIndex];
        currentTargetIndex++;

        // Abrir la ficha de detalles de la app en Ajustes
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + target));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void handleSettingsClick() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        // 1. Buscar y hacer clic en "Forzar detención" (Español o Inglés)
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

        // 2. Si saltó el cartel de confirmación, hacer clic en "Aceptar" / "OK"
        List<AccessibilityNodeInfo> okButtons = root.findAccessibilityNodeInfosByText("Aceptar");
        if (okButtons.isEmpty()) {
            okButtons = root.findAccessibilityNodeInfosByText("OK");
        }

        for (AccessibilityNodeInfo node : okButtons) {
            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                // Pasar al siguiente objetivo
                killNextTarget();
                return;
            }
        }

        // Si no encontró botones activos (ej: ya estaba detenida), pasar al siguiente
        killNextTarget();
    }

    @Override
    public void onInterrupt() {
        isCleaning = false;
    }
}
