package com.keremgok.sms;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing multiple target numbers
 * Allows adding, editing, and removing target phone numbers for SMS forwarding
 */
public class TargetNumbersActivity extends AppCompatActivity implements TargetNumberAdapter.OnTargetNumberActionListener {

    private static final String PREFS_NAME = "HermesPrefs";
    private static final String KEY_SENDING_MODE = "sending_mode";
    
    // UI Components
    private FloatingActionButton fabAddTarget;
    private RecyclerView rvTargetNumbers;
    private TextView tvEmptyState;
    private Spinner spinnerSendingMode;
    
    // Dialog components (initialized when needed)
    private EditText etNewPhoneNumber;
    private EditText etNewDisplayName;
    private CheckBox cbSetAsPrimary;
    private CheckBox cbEnabled;
    private TextView tvValidationMessage;
    private Button btnAddTarget;
    
    private Button dialogPositiveButton;
    // SIM selection components
    private RadioGroup radioGroupSimMode;
    private RadioButton radioAuto;
    private RadioButton radioSource;
    private RadioButton radioSpecific;
    private Spinner spinnerSimSelection;
    private List<SimManager.SimInfo> availableSims;
    
    // Cache to prevent repeated SIM checks during dialog
    private Boolean cachedDualSimSupported = null;
    
    // Data and Adapters
    private TargetNumberAdapter adapter;
    private AppDatabase database;
    private TargetNumberDao targetNumberDao;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target_numbers);
        
        // Enable up button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.target_numbers_title);
        }
        
        initDatabase();
        initViews();
        setupRecyclerView();
        setupSendingModeSpinner();
        loadTargetNumbers();
        updateUI();
    }
    
    /**
     * Initialize database components
     */
    private void initDatabase() {
        database = AppDatabase.getInstance(this);
        if (database == null) {
            Toast.makeText(this, R.string.error_database_init_failed, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        targetNumberDao = database.targetNumberDao();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }
    
    /**
     * Initialize UI components
     */
    private void initViews() {
        fabAddTarget = findViewById(R.id.fabAddTarget);
        rvTargetNumbers = findViewById(R.id.rvTargetNumbers);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        spinnerSendingMode = findViewById(R.id.spinnerSendingMode);
        
        fabAddTarget.setOnClickListener(v -> showAddTargetDialog());
    }
    
    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        adapter = new TargetNumberAdapter(this, this);
        rvTargetNumbers.setLayoutManager(new LinearLayoutManager(this));
        rvTargetNumbers.setAdapter(adapter);
    }
    
    /**
     * Show add target number dialog
     */
    private void showAddTargetDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_target_number, null);
        
        // Initialize dialog components
        etNewPhoneNumber = dialogView.findViewById(R.id.etNewPhoneNumber);
        etNewDisplayName = dialogView.findViewById(R.id.etNewDisplayName);
        cbSetAsPrimary = dialogView.findViewById(R.id.cbSetAsPrimary);
        cbEnabled = dialogView.findViewById(R.id.cbEnabled);
        tvValidationMessage = dialogView.findViewById(R.id.tvValidationMessage);
        
        // Initialize SIM selection components
        radioGroupSimMode = dialogView.findViewById(R.id.radio_group_sim_mode);
        radioAuto = dialogView.findViewById(R.id.radio_auto);
        radioSource = dialogView.findViewById(R.id.radio_source);
        radioSpecific = dialogView.findViewById(R.id.radio_specific);
        spinnerSimSelection = dialogView.findViewById(R.id.spinner_sim_selection);
        
        // Setup SIM selection
        setupSimSelectionUI();
        
        // Setup validation
        setupDialogValidation();
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.add_target_number, null)
            .setNegativeButton(R.string.cancel, null)
            .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            dialogPositiveButton = positiveButton;
            positiveButton.setEnabled(false);
            positiveButton.setOnClickListener(v -> {
                if (addTargetNumber()) {
                    dialog.dismiss();
                }
            });
        });
        
        dialog.show();
    }
    
    /**
     * Setup SIM selection UI components
     */
    private void setupSimSelectionUI() {
        // Setup radio button listeners first
        radioGroupSimMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_specific) {
                spinnerSimSelection.setVisibility(View.VISIBLE);
            } else {
                spinnerSimSelection.setVisibility(View.GONE);
            }
        });
        
        // Default to hiding SIM selection until we confirm dual SIM support
        radioGroupSimMode.setVisibility(View.GONE);
        spinnerSimSelection.setVisibility(View.GONE);
        
        // Check dual SIM support in background thread
        ThreadManager.getInstance().executeBackground(() -> {
            try {
                // Load available SIMs
                List<SimManager.SimInfo> sims = SimManager.getActiveSimCards(this);
                boolean isDualSim = sims.size() >= 2;
                
                runOnUiThread(() -> {
                    availableSims = sims;
                    cachedDualSimSupported = isDualSim;
                    
                    if (isDualSim && availableSims.size() > 1) {
                        // Show SIM selection UI for dual SIM devices
                        radioGroupSimMode.setVisibility(View.VISIBLE);
                        setupSimSpinner();
                    } else {
                        // Keep hidden for single SIM devices
                        radioGroupSimMode.setVisibility(View.GONE);
                        spinnerSimSelection.setVisibility(View.GONE);
                        
                        // Show single SIM info message
                        showSingleSimInfo();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    // On error, hide SIM selection and show single SIM info
                    availableSims = new ArrayList<>();
                    cachedDualSimSupported = false;
                    radioGroupSimMode.setVisibility(View.GONE);
                    spinnerSimSelection.setVisibility(View.GONE);
                    showSingleSimInfo();
                });
            }
        });
    }
    
    /**
     * Load available SIM cards for selection
     * This method is now handled in setupSimSelectionUI() background thread
     */
    private void loadAvailableSims() {
        // This method is deprecated - SIM loading now happens in setupSimSelectionUI()
        if (availableSims == null) {
            availableSims = new ArrayList<>();
        }
    }
    
    /**
     * Setup SIM selection spinner
     */
    private void setupSimSpinner() {
        if (availableSims == null || availableSims.isEmpty()) {
            // No SIMs available
            List<String> simNames = new ArrayList<>();
            simNames.add(getString(R.string.sim_not_available));
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, simNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSimSelection.setAdapter(adapter);
            spinnerSimSelection.setEnabled(false);
            return;
        }
        
        // Create SIM display names
        List<String> simNames = new ArrayList<>();
        for (SimManager.SimInfo simInfo : availableSims) {
            String displayName = String.format(getString(R.string.sim_card_format), 
                simInfo.displayName != null ? simInfo.displayName : getString(R.string.sim_slot_format, simInfo.slotIndex + 1),
                simInfo.carrierName != null ? simInfo.carrierName : "");
            simNames.add(displayName);
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, simNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSimSelection.setAdapter(adapter);
        spinnerSimSelection.setEnabled(true);
    }
    
    /**
     * Setup real-time validation for dialog inputs
     */
    private void setupDialogValidation() {
        etNewPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                validateInput();
            }
        });
    }
    
    /**
     * Setup sending mode spinner
     */
    private void setupSendingModeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this,
            R.array.sending_mode_entries,
            android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSendingMode.setAdapter(adapter);
        
        // Load saved sending mode
        String savedMode = prefs.getString(KEY_SENDING_MODE, "sequential");
        if ("parallel".equals(savedMode)) {
            spinnerSendingMode.setSelection(1);
        } else {
            spinnerSendingMode.setSelection(0);
        }
    }
    
    /**
     * Validate phone number input and update UI
     */
    private void validateInput() {
        String phoneNumber = etNewPhoneNumber.getText().toString().trim();
        
        if (TextUtils.isEmpty(phoneNumber)) {
            tvValidationMessage.setVisibility(View.GONE);
            enableAddButton(false);
            return;
        }
        
        // Validate phone number
        PhoneNumberValidator.ValidationResult result = PhoneNumberValidator.validate(phoneNumber);
        
        if (result.isValid()) {
            // Check for duplicates
            ThreadManager.getInstance().executeBackground(() -> {
                boolean exists = targetNumberDao.isPhoneNumberExists(phoneNumber);
                runOnUiThread(() -> {
                    if (exists) {
                        showValidationError(getString(R.string.target_duplicate_number));
                        enableAddButton(false);
                    } else {
                        showValidationSuccess();
                        enableAddButton(true);
                    }
                });
            });
        } else {
            String errorMessage = getValidationMessage(result.getErrorCode());
            showValidationError(errorMessage);
            enableAddButton(false);
        }
    }
    
    /**
     * Show validation success message
     */
    private void showValidationSuccess() {
        tvValidationMessage.setText(R.string.validation_success);
        tvValidationMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        tvValidationMessage.setVisibility(View.VISIBLE);
    }
    
    /**
     * Show validation error message
     * @param message The error message to display
     */
    private void showValidationError(String message) {
        tvValidationMessage.setText(message);
        tvValidationMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        tvValidationMessage.setVisibility(View.VISIBLE);
    }
    
    /**
     * Get localized validation message
     * @param errorCode The error code from validation
     * @return Localized error message
     */
    private String getValidationMessage(String errorCode) {
        if (errorCode == null) return "";
        
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
     * Helper method to enable/disable add button in dialog
     */
    private void enableAddButton(boolean enabled) {
        if (dialogPositiveButton != null) {
            dialogPositiveButton.setEnabled(enabled);
        }
    }
    
    /**
     * Add a new target number
     * @return true if target was added successfully, false otherwise
     */
    private boolean addTargetNumber() {
        String phoneNumber = etNewPhoneNumber.getText().toString().trim();
        String displayName = etNewDisplayName.getText().toString().trim();
        boolean isPrimary = cbSetAsPrimary.isChecked();
        boolean isEnabled = cbEnabled.isChecked();
        
        // Validate phone number
        PhoneNumberValidator.ValidationResult result = PhoneNumberValidator.validate(phoneNumber);
        if (!result.isValid()) {
            Toast.makeText(this, getValidationMessage(result.getErrorCode()), Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Use formatted number if available
        String formattedNumber = result.getFormattedNumber() != null ? 
                                result.getFormattedNumber() : phoneNumber;
        
        // Get SIM selection settings
        String simSelectionMode = "auto"; // default
        int preferredSimSlot = -1; // default
        
        if (cachedDualSimSupported != null && cachedDualSimSupported && availableSims != null && availableSims.size() > 1) {
            // Determine SIM selection mode based on radio button selection
            int checkedRadioButtonId = radioGroupSimMode.getCheckedRadioButtonId();
            if (checkedRadioButtonId == R.id.radio_auto) {
                simSelectionMode = "auto";
            } else if (checkedRadioButtonId == R.id.radio_source) {
                simSelectionMode = "source_sim";
            } else if (checkedRadioButtonId == R.id.radio_specific) {
                simSelectionMode = "specific_sim";
                // Get selected SIM slot from spinner
                int selectedIndex = spinnerSimSelection.getSelectedItemPosition();
                if (selectedIndex >= 0 && selectedIndex < availableSims.size()) {
                    preferredSimSlot = availableSims.get(selectedIndex).slotIndex;
                }
            }
        }
        
        // Create target number object with SIM preferences
        TargetNumber targetNumber = new TargetNumber(formattedNumber, displayName, isPrimary, isEnabled, preferredSimSlot, simSelectionMode);
        
        // Save to database
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                // If setting as primary, unset other primary targets
                if (isPrimary) {
                    targetNumberDao.setPrimaryTargetNumber(-1); // Unset all
                }
                
                long id = targetNumberDao.insert(targetNumber);
                
                // If this is the first target, set as primary automatically
                List<TargetNumber> allTargets = targetNumberDao.getAllTargetNumbers();
                if (allTargets.size() == 1) {
                    targetNumberDao.setPrimaryTargetNumber((int) id);
                } else if (isPrimary) {
                    targetNumberDao.setPrimaryTargetNumber((int) id);
                }
                
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.target_add_success, Toast.LENGTH_SHORT).show();
                    loadTargetNumbers();
                    saveSendingMode();
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.target_add_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        return true;
    }
    
    
    /**
     * Load target numbers from database and update UI
     */
    private void loadTargetNumbers() {
        ThreadManager.getInstance().executeDatabase(() -> {
            List<TargetNumber> targets = targetNumberDao.getAllTargetNumbers();
            runOnUiThread(() -> {
                adapter.updateTargetNumbers(targets);
                updateUI();
            });
        });
    }
    
    /**
     * Update UI based on current state
     */
    private void updateUI() {
        boolean hasTargets = adapter.getItemCount() > 0;
        tvEmptyState.setVisibility(hasTargets ? View.GONE : View.VISIBLE);
        rvTargetNumbers.setVisibility(hasTargets ? View.VISIBLE : View.GONE);
    }
    
    /**
     * Save sending mode preference
     */
    private void saveSendingMode() {
        String[] values = getResources().getStringArray(R.array.sending_mode_values);
        int selectedIndex = spinnerSendingMode.getSelectedItemPosition();
        if (selectedIndex >= 0 && selectedIndex < values.length) {
            prefs.edit().putString(KEY_SENDING_MODE, values[selectedIndex]).apply();
        }
    }
    
    @Override
    public void onSetPrimary(TargetNumber targetNumber) {
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                targetNumberDao.setPrimaryTargetNumber(targetNumber.getId());
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.target_update_success, Toast.LENGTH_SHORT).show();
                    loadTargetNumbers();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.target_update_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    @Override
    public void onToggleEnabled(TargetNumber targetNumber) {
        boolean newEnabledState = !targetNumber.isEnabled();
        
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                targetNumberDao.setEnabledStatus(targetNumber.getId(), newEnabledState);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.target_update_success, Toast.LENGTH_SHORT).show();
                    loadTargetNumbers();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.target_update_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    @Override
    public void onDelete(TargetNumber targetNumber) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.target_delete_title)
            .setMessage(R.string.target_delete_message)
            .setPositiveButton(R.string.target_delete_confirm, (dialog, which) -> {
                deleteTargetNumber(targetNumber);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Delete target number from database
     * @param targetNumber The target number to delete
     */
    private void deleteTargetNumber(TargetNumber targetNumber) {
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                targetNumberDao.delete(targetNumber);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.target_delete_success, Toast.LENGTH_SHORT).show();
                    loadTargetNumbers();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.target_delete_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    @Override
    public void onEdit(TargetNumber targetNumber) {
        showEditTargetDialog(targetNumber);
    }

    /**
     * Show dialog to edit an existing target number
     * Reuses the same layout as add dialog but pre-fills with existing values
     */
    private void showEditTargetDialog(TargetNumber targetNumber) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_target_number, null);

        // Get all form fields
        EditText etPhoneNumber = dialogView.findViewById(R.id.etNewPhoneNumber);
        EditText etDisplayName = dialogView.findViewById(R.id.etNewDisplayName);
        CheckBox cbSetPrimary = dialogView.findViewById(R.id.cbSetAsPrimary);
        CheckBox cbEnabledCheck = dialogView.findViewById(R.id.cbEnabled);
        RadioGroup radioGroupSimMode = dialogView.findViewById(R.id.radio_group_sim_mode);
        RadioButton rbSimAuto = dialogView.findViewById(R.id.radio_auto);
        RadioButton rbSimSource = dialogView.findViewById(R.id.radio_source);
        RadioButton rbSimSpecific = dialogView.findViewById(R.id.radio_specific);
        Spinner spinnerSimSelection = dialogView.findViewById(R.id.spinner_sim_selection);
        TextView tvValidationMessage = dialogView.findViewById(R.id.tvValidationMessage);

        // Pre-fill existing values
        etPhoneNumber.setText(targetNumber.getPhoneNumber());
        etDisplayName.setText(targetNumber.getDisplayName());
        cbSetPrimary.setChecked(targetNumber.isPrimary());
        cbEnabledCheck.setChecked(targetNumber.isEnabled());

        // Pre-select SIM mode
        String simMode = targetNumber.getSimSelectionMode();
        if (simMode == null || simMode.isEmpty()) {
            simMode = "auto";
        }

        switch (simMode.toLowerCase()) {
            case "source_sim":
                rbSimSource.setChecked(true);
                spinnerSimSelection.setVisibility(View.GONE);
                break;
            case "specific_sim":
                rbSimSpecific.setChecked(true);
                spinnerSimSelection.setVisibility(View.VISIBLE);
                // Set selected SIM slot
                int preferredSlot = targetNumber.getPreferredSimSlot();
                if (preferredSlot >= 0 && preferredSlot < spinnerSimSelection.getCount()) {
                    spinnerSimSelection.setSelection(preferredSlot);
                }
                break;
            default: // "auto"
                rbSimAuto.setChecked(true);
                spinnerSimSelection.setVisibility(View.GONE);
                break;
        }

        // Setup radio button listeners
        radioGroupSimMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_specific) {
                spinnerSimSelection.setVisibility(View.VISIBLE);
            } else {
                spinnerSimSelection.setVisibility(View.GONE);
            }
        });

        // Check if dual SIM is supported
        boolean isDualSim = cachedDualSimSupported != null ? cachedDualSimSupported : SimManager.isDualSimSupported(this);
        if (!isDualSim) {
            radioGroupSimMode.setVisibility(View.GONE);
            spinnerSimSelection.setVisibility(View.GONE);
        } else {
            radioGroupSimMode.setVisibility(View.VISIBLE);
        }

        // Setup real-time validation for edit dialog
        setupEditDialogValidation(etPhoneNumber, tvValidationMessage, targetNumber);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setTitle(R.string.target_edit)
                .setPositiveButton(R.string.target_update, null)
                .setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            // Enable button initially since we have valid existing data
            positiveButton.setEnabled(true);

            // Store button reference for validation updates
            final Button[] updateButton = {positiveButton};

            positiveButton.setOnClickListener(view -> {
                String phoneNumber = etPhoneNumber.getText().toString().trim();
                String displayName = etDisplayName.getText().toString().trim();
                boolean isPrimary = cbSetPrimary.isChecked();
                boolean isEnabled = cbEnabledCheck.isChecked();

                String simSelectionMode = "auto";
                int preferredSimSlot = -1;

                int checkedRadioId = radioGroupSimMode.getCheckedRadioButtonId();
                if (checkedRadioId == R.id.radio_source) {
                    simSelectionMode = "source_sim";
                } else if (checkedRadioId == R.id.radio_specific) {
                    simSelectionMode = "specific_sim";
                    preferredSimSlot = spinnerSimSelection.getSelectedItemPosition();
                }

                if (updateTargetNumber(targetNumber, phoneNumber, displayName, isPrimary, isEnabled, simSelectionMode, preferredSimSlot)) {
                    dialog.dismiss();
                }
            });

            // Update validation with button reference
            etPhoneNumber.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    validateEditInput(s.toString().trim(), tvValidationMessage, updateButton[0], targetNumber);
                }
            });
        });

        dialog.show();
    }

    /**
     * Setup real-time validation for edit dialog
     */
    private void setupEditDialogValidation(EditText etPhoneNumber, TextView tvValidationMessage, TargetNumber targetNumber) {
        // Initial validation is done after dialog is shown (in onShowListener)
        // This method can be used for additional setup if needed
    }

    /**
     * Validate phone number input for edit dialog
     */
    private void validateEditInput(String phoneNumber, TextView tvValidationMessage, Button updateButton, TargetNumber targetNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            tvValidationMessage.setVisibility(View.GONE);
            updateButton.setEnabled(false);
            return;
        }

        // Validate phone number format
        PhoneNumberValidator.ValidationResult result = PhoneNumberValidator.validate(phoneNumber);

        if (result.isValid()) {
            // Check for duplicates (excluding current target)
            ThreadManager.getInstance().executeBackground(() -> {
                boolean exists = targetNumberDao.isPhoneNumberExistsExcept(phoneNumber, targetNumber.getId());
                runOnUiThread(() -> {
                    if (exists) {
                        showEditValidationError(getString(R.string.target_duplicate_number), tvValidationMessage);
                        updateButton.setEnabled(false);
                    } else {
                        showEditValidationSuccess(tvValidationMessage);
                        updateButton.setEnabled(true);
                    }
                });
            });
        } else {
            String errorMessage = getValidationMessage(result.getErrorCode());
            showEditValidationError(errorMessage, tvValidationMessage);
            updateButton.setEnabled(false);
        }
    }

    /**
     * Show validation success message for edit dialog
     */
    private void showEditValidationSuccess(TextView tvValidationMessage) {
        tvValidationMessage.setText(R.string.validation_success);
        tvValidationMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        tvValidationMessage.setVisibility(View.VISIBLE);
    }

    /**
     * Show validation error message for edit dialog
     */
    private void showEditValidationError(String message, TextView tvValidationMessage) {
        tvValidationMessage.setText(message);
        tvValidationMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        tvValidationMessage.setVisibility(View.VISIBLE);
    }

    /**
     * Update an existing target number with new values
     * @return true if update initiated successfully, false if validation failed
     */
    private boolean updateTargetNumber(TargetNumber targetNumber, String phoneNumber, String displayName,
                                       boolean isPrimary, boolean isEnabled, String simSelectionMode, int preferredSimSlot) {
        // Validate phone number
        PhoneNumberValidator.ValidationResult result = PhoneNumberValidator.validate(phoneNumber);
        if (!result.isValid()) {
            String errorMessage = getValidationMessage(result.getErrorCode());
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check for duplicate phone number (excluding current target)
        ThreadManager.getInstance().executeDatabase(() -> {
            boolean duplicateExists = targetNumberDao.isPhoneNumberExistsExcept(phoneNumber, targetNumber.getId());

            runOnUiThread(() -> {
                if (duplicateExists) {
                    Toast.makeText(this, R.string.target_duplicate_number, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update the target number object with new values
                targetNumber.setPhoneNumber(phoneNumber);
                targetNumber.setDisplayName(displayName);
                targetNumber.setPrimary(isPrimary);
                targetNumber.setEnabled(isEnabled);
                targetNumber.setSimSelectionMode(simSelectionMode);
                targetNumber.setPreferredSimSlot(preferredSimSlot);
                targetNumber.setModifiedTimestamp(System.currentTimeMillis());

                // Perform update in database
                ThreadManager.getInstance().executeDatabase(() -> {
                    // If setting as primary, unset all others first
                    if (isPrimary) {
                        targetNumberDao.setPrimaryTargetNumber(-1);
                        targetNumberDao.update(targetNumber);
                        targetNumberDao.setPrimaryTargetNumber(targetNumber.getId());
                    } else {
                        targetNumberDao.update(targetNumber);
                    }

                    runOnUiThread(() -> {
                        loadTargetNumbers();
                        Toast.makeText(this, R.string.target_update_success, Toast.LENGTH_SHORT).show();
                    });
                });
            });
        });

        return true;
    }

    /**
     * Show single SIM device information in dialog
     */
    private void showSingleSimInfo() {
        // Find a parent view where we can add the info text
        if (radioGroupSimMode != null && radioGroupSimMode.getParent() instanceof android.view.ViewGroup) {
            android.view.ViewGroup parent = (android.view.ViewGroup) radioGroupSimMode.getParent();
            
            // Check if info text already exists
            android.view.View existingInfo = parent.findViewWithTag("single_sim_info");
            if (existingInfo == null) {
                // Create info text view
                TextView simInfoText = new TextView(this);
                simInfoText.setTag("single_sim_info");
                simInfoText.setText(R.string.single_sim_device_info);
                simInfoText.setTextSize(14);
                simInfoText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                simInfoText.setPadding(16, 8, 16, 8);
                simInfoText.setGravity(android.view.Gravity.CENTER);
                
                // Find the index where SIM selection would be
                int insertIndex = parent.indexOfChild(radioGroupSimMode);
                if (insertIndex >= 0) {
                    parent.addView(simInfoText, insertIndex);
                }
            }
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        saveSendingMode();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}