package com.keremgok.sms.onboarding;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.keremgok.sms.AppDatabase;
import com.keremgok.sms.PhoneNumberValidator;
import com.keremgok.sms.R;
import com.keremgok.sms.TargetNumber;
import com.keremgok.sms.TargetNumberDao;
import com.keremgok.sms.ThreadManager;

public class TargetNumberSetupFragment extends Fragment {
    
    private TextInputEditText etTargetNumber;
    private TextInputLayout tilTargetNumber;
    private AppDatabase database;
    private TargetNumberDao targetNumberDao;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_target_number_setup, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initDatabase();
        initViews(view);
        setupValidation();
    }
    
    private void initDatabase() {
        database = AppDatabase.getInstance(requireContext());
        targetNumberDao = database.targetNumberDao();
    }
    
    private void initViews(View view) {
        etTargetNumber = view.findViewById(R.id.et_target_number);
        tilTargetNumber = (TextInputLayout) etTargetNumber.getParent().getParent();
    }
    
    private void setupValidation() {
        etTargetNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                validatePhoneNumber(s.toString().trim());
            }
        });
    }
    
    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber.isEmpty()) {
            tilTargetNumber.setError(null);
            tilTargetNumber.setHelperText(getString(R.string.target_number_helper));
            return;
        }
        
        PhoneNumberValidator.ValidationResult result = PhoneNumberValidator.validate(phoneNumber);
        if (result.isValid()) {
            tilTargetNumber.setError(null);
            tilTargetNumber.setHelperText(getString(R.string.validation_success));
        } else {
            tilTargetNumber.setError(getValidationMessage(result.getErrorCode()));
            tilTargetNumber.setHelperText(null);
        }
    }
    
    private String getValidationMessage(String errorCode) {
        if (errorCode == null) {
            return getString(R.string.validation_invalid_format);
        }
        
        switch (errorCode) {
            case "EMPTY_NUMBER":
                return getString(R.string.validation_empty_number);
            case "MISSING_COUNTRY_CODE":
                return getString(R.string.validation_missing_country_code);
            case "TOO_SHORT":
                return getString(R.string.validation_too_short);
            case "TOO_LONG":
                return getString(R.string.validation_too_long);
            case "INVALID_FORMAT":
                return getString(R.string.validation_invalid_format);
            default:
                return getString(R.string.validation_invalid_format);
        }
    }
    
    /**
     * Called by OnboardingActivity when user proceeds to next step
     * Saves the entered phone number if valid
     */
    public void saveTargetNumber() {
        String phoneNumber = etTargetNumber.getText().toString().trim();
        
        if (phoneNumber.isEmpty()) {
            // Allow empty - user can skip target number setup
            return;
        }
        
        PhoneNumberValidator.ValidationResult result = PhoneNumberValidator.validate(phoneNumber);
        if (!result.isValid()) {
            Toast.makeText(getContext(), getValidationMessage(result.getErrorCode()), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Use formatted number if available
        String formattedNumber = result.getFormattedNumber() != null ? 
                                result.getFormattedNumber() : phoneNumber;
        
        // Create new target number (first one is primary and enabled by default)
        TargetNumber targetNumber = new TargetNumber(
            formattedNumber,
            getString(R.string.primary_target_name), // Default display name
            true, // isPrimary - first target is primary
            true  // isEnabled
        );
        
        // Save to database in background thread
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                long id = targetNumberDao.insert(targetNumber);
                
                // Show success message on UI thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (id > 0) {
                            Toast.makeText(getContext(), 
                                getString(R.string.target_number_saved_successfully), 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                // Handle error on UI thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), 
                            getString(R.string.error_saving_target_number), 
                            Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}