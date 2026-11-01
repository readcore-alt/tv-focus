package com.custom.tvfocus;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(0xFF121212);
        layout.setPadding(40, 40, 40, 40);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🎯 TV Focus");
        tvTitle.setTextSize(32);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setGravity(Gravity.CENTER);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("\nPresiona el botón para activar el servicio en Accesibilidad.\nUna vez activado, purgará automáticamente la RAM al abrir tu navegador.\n");
        tvDesc.setTextSize(18);
        tvDesc.setTextColor(0xFFAAAAAA);
        tvDesc.setGravity(Gravity.CENTER);

        Button btnSettings = new Button(this);
        btnSettings.setText("ACTIVAR ACCESIBILIDAD");
        btnSettings.setTextSize(18);
        btnSettings.setBackgroundColor(0xFF2196F3);
        btnSettings.setTextColor(0xFFFFFFFF);
        btnSettings.setFocusable(true);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        layout.addView(tvTitle);
        layout.addView(tvDesc);
        layout.addView(btnSettings);

        setContentView(layout);
    }
}
