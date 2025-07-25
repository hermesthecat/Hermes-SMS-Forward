package com.keremgok.sms;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    
    private static final int SMS_PERMISSION_REQUEST_CODE = 123;
    private static final String PREFS_NAME = "HermesPrefs";
    private static final String KEY_TARGET_NUMBER = "target_number";
    
    private EditText etTargetNumber;
    private Button btnSave;
    private TextView tvStatus;
    private TextView tvPermissions;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupPreferences();
        checkPermissions();
        loadSavedNumber();
        updateUI();
        
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTargetNumber();
            }
        });
    }
    
    private void initViews() {
        etTargetNumber = findViewById(R.id.etTargetNumber);
        btnSave = findViewById(R.id.btnSave);
        tvStatus = findViewById(R.id.tvStatus);
        tvPermissions = findViewById(R.id.tvPermissions);
    }
    
    private void setupPreferences() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }
    
    private void loadSavedNumber() {
        String savedNumber = prefs.getString(KEY_TARGET_NUMBER, "");
        etTargetNumber.setText(savedNumber);
    }
    
    private void saveTargetNumber() {
        String targetNumber = etTargetNumber.getText().toString().trim();
        
        if (TextUtils.isEmpty(targetNumber)) {
            Toast.makeText(this, R.string.error_empty_number, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!hasRequiredPermissions()) {
            requestPermissions();
            return;
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TARGET_NUMBER, targetNumber);
        editor.apply();
        
        Toast.makeText(this, R.string.success_saved, Toast.LENGTH_SHORT).show();
        updateUI();
    }
    
    private boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS},
                SMS_PERMISSION_REQUEST_CODE);
    }
    
    private void checkPermissions() {
        if (!hasRequiredPermissions()) {
            requestPermissions();
        }
    }
    
    private void updateUI() {
        String savedNumber = prefs.getString(KEY_TARGET_NUMBER, "");
        boolean hasPermissions = hasRequiredPermissions();
        
        if (hasPermissions) {
            tvPermissions.setText(R.string.permissions_granted);
            tvPermissions.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvPermissions.setText(R.string.permissions_required);
            tvPermissions.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        }
        
        if (!TextUtils.isEmpty(savedNumber) && hasPermissions) {
            tvStatus.setText(R.string.status_configured);
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvStatus.setText(R.string.status_not_configured);
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            
            if (allPermissionsGranted) {
                Toast.makeText(this, "İzinler verildi. Şimdi hedef numarayı kaydedebilirsiniz.", Toast.LENGTH_LONG).show();
                saveTargetNumber();
            } else {
                Toast.makeText(this, "SMS izinleri gerekli!", Toast.LENGTH_LONG).show();
            }
            
            updateUI();
        }
    }
}