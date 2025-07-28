package com.keremgok.sms;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Activity for managing SMS filter rules
 * Allows adding, editing, and removing filter rules for SMS filtering
 */
public class FilterRulesActivity extends AppCompatActivity implements FilterRulesAdapter.OnFilterRuleActionListener {

    // UI Components
    private EditText etFilterName;
    private Spinner spinnerFilterType;
    private EditText etFilterPattern;
    private RadioGroup rgFilterAction;
    private RadioButton rbActionAllow;
    private RadioButton rbActionBlock;
    private CheckBox cbCaseSensitive;
    private CheckBox cbRegex;
    private CheckBox cbEnabled;
    private TextView tvValidationMessage;
    private Button btnAddFilter;
    private RecyclerView rvFilterRules;
    private TextView tvEmptyState;
    
    // Data and Adapters
    private FilterRulesAdapter adapter;
    private AppDatabase database;
    private SmsFilterDao filterDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_rules);
        
        // Enable up button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.filter_rules_title);
        }
        
        initDatabase();
        initViews();
        setupRecyclerView();
        setupSpinners();
        setupValidation();
        loadFilterRules();
        updateUI();
    }
    
    /**
     * Initialize database components
     */
    private void initDatabase() {
        database = AppDatabase.getInstance(this);
        filterDao = database.smsFilterDao();
    }
    
    /**
     * Initialize UI components
     */
    private void initViews() {
        etFilterName = findViewById(R.id.etFilterName);
        spinnerFilterType = findViewById(R.id.spinnerFilterType);
        etFilterPattern = findViewById(R.id.etFilterPattern);
        rgFilterAction = findViewById(R.id.rgFilterAction);
        rbActionAllow = findViewById(R.id.rbActionAllow);
        rbActionBlock = findViewById(R.id.rbActionBlock);
        cbCaseSensitive = findViewById(R.id.cbCaseSensitive);
        cbRegex = findViewById(R.id.cbRegex);
        cbEnabled = findViewById(R.id.cbEnabled);
        tvValidationMessage = findViewById(R.id.tvValidationMessage);
        btnAddFilter = findViewById(R.id.btnAddFilter);
        rvFilterRules = findViewById(R.id.rvFilterRules);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        
        btnAddFilter.setOnClickListener(v -> addFilter());
    }
    
    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        adapter = new FilterRulesAdapter(this, this);
        rvFilterRules.setLayoutManager(new LinearLayoutManager(this));
        rvFilterRules.setAdapter(adapter);
    }
    
    /**
     * Setup spinners with data
     */
    private void setupSpinners() {
        // Filter Type Spinner
        ArrayAdapter<CharSequence> filterTypeAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.filter_type_entries,
            android.R.layout.simple_spinner_item
        );
        filterTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterType.setAdapter(filterTypeAdapter);
    }
    
    /**
     * Setup real-time validation for form inputs
     */
    private void setupValidation() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                validateInput();
            }
        };
        
        etFilterName.addTextChangedListener(validationWatcher);
        etFilterPattern.addTextChangedListener(validationWatcher);
    }
    
    /**
     * Validate form input and update UI
     */
    private void validateInput() {
        String filterName = etFilterName.getText().toString().trim();
        String pattern = etFilterPattern.getText().toString().trim();
        
        if (TextUtils.isEmpty(filterName)) {
            showValidationError(getString(R.string.filter_name_required));
            btnAddFilter.setEnabled(false);
            return;
        }
        
        if (TextUtils.isEmpty(pattern)) {
            showValidationError(getString(R.string.filter_pattern_required));
            btnAddFilter.setEnabled(false);
            return;
        }
        
        // Check for duplicate filter name
        ThreadManager.getInstance().executeBackground(() -> {
            boolean exists = filterDao.isFilterNameExists(filterName);
            runOnUiThread(() -> {
                if (exists) {
                    showValidationError(getString(R.string.filter_name_exists));
                    btnAddFilter.setEnabled(false);
                } else {
                    // Validate regex if enabled
                    if (cbRegex.isChecked()) {
                        if (isValidRegex(pattern)) {
                            showValidationSuccess();
                            btnAddFilter.setEnabled(true);
                        } else {
                            showValidationError(getString(R.string.filter_invalid_regex));
                            btnAddFilter.setEnabled(false);
                        }
                    } else {
                        showValidationSuccess();
                        btnAddFilter.setEnabled(true);
                    }
                }
            });
        });
    }
    
    /**
     * Check if regex pattern is valid
     */
    private boolean isValidRegex(String pattern) {
        try {
            Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }
    
    /**
     * Show validation success message
     */
    private void showValidationSuccess() {
        tvValidationMessage.setText(getString(R.string.filter_validation_passed));
        tvValidationMessage.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        tvValidationMessage.setVisibility(View.VISIBLE);
    }
    
    /**
     * Show validation error message
     */
    private void showValidationError(String message) {
        tvValidationMessage.setText(message);
        tvValidationMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        tvValidationMessage.setVisibility(View.VISIBLE);
    }
    
    /**
     * Add a new filter
     */
    private void addFilter() {
        String filterName = etFilterName.getText().toString().trim();
        String pattern = etFilterPattern.getText().toString().trim();
        
        // Get filter type from spinner
        String[] filterTypeValues = getResources().getStringArray(R.array.filter_type_values);
        int selectedTypeIndex = spinnerFilterType.getSelectedItemPosition();
        String filterType = filterTypeValues[selectedTypeIndex];
        
        // Get action from radio group
        String action = rbActionAllow.isChecked() ? SmsFilter.ACTION_ALLOW : SmsFilter.ACTION_BLOCK;
        
        boolean isEnabled = cbEnabled.isChecked();
        
        // Create filter object
        SmsFilter filter = new SmsFilter(filterName, filterType, pattern, action, isEnabled);
        filter.setCaseSensitive(cbCaseSensitive.isChecked());
        filter.setRegex(cbRegex.isChecked());
        filter.setPriority(0); // Default priority
        
        // Save to database
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                filterDao.insert(filter);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.filter_add_success, Toast.LENGTH_SHORT).show();
                    clearForm();
                    loadFilterRules();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.filter_add_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    
    /**
     * Clear the add filter form
     */
    private void clearForm() {
        etFilterName.setText("");
        etFilterPattern.setText("");
        spinnerFilterType.setSelection(0);
        rbActionAllow.setChecked(true);
        cbCaseSensitive.setChecked(false);
        cbRegex.setChecked(false);
        cbEnabled.setChecked(true);
        tvValidationMessage.setVisibility(View.GONE);
        btnAddFilter.setEnabled(false);
    }
    
    /**
     * Load filter rules from database and update UI
     */
    private void loadFilterRules() {
        ThreadManager.getInstance().executeDatabase(() -> {
            List<SmsFilter> filters = filterDao.getAllFilters();
            android.util.Log.d("FilterRulesActivity", "Loaded " + (filters != null ? filters.size() : 0) + " filters from database");
            runOnUiThread(() -> {
                adapter.updateFilterRules(filters);
                updateUI();
                android.util.Log.d("FilterRulesActivity", "Adapter item count: " + adapter.getItemCount());
            });
        });
    }
    
    /**
     * Update UI based on current state
     */
    private void updateUI() {
        boolean hasFilters = adapter.getItemCount() > 0;
        tvEmptyState.setVisibility(hasFilters ? View.GONE : View.VISIBLE);
        rvFilterRules.setVisibility(hasFilters ? View.VISIBLE : View.GONE);
    }
    
    @Override
    public void onTestFilter(SmsFilter filter) {
        // Show test dialog
        showFilterTestDialog(filter);
    }
    
    @Override
    public void onToggleEnabled(SmsFilter filter) {
        boolean newEnabledState = !filter.isEnabled();
        
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                filterDao.setEnabledStatus(filter.getId(), newEnabledState, System.currentTimeMillis());
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.filter_update_success, Toast.LENGTH_SHORT).show();
                    loadFilterRules();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.filter_update_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    @Override
    public void onEditFilter(SmsFilter filter) {
        // For now, just show a toast. Full edit functionality would require a separate dialog/activity
        Toast.makeText(this, getString(R.string.edit_functionality_coming_soon), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onDeleteFilter(SmsFilter filter) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.filter_delete_title)
            .setMessage(R.string.filter_delete_message)
            .setPositiveButton(R.string.filter_delete_confirm, (dialog, which) -> {
                deleteFilter(filter);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Delete filter from database
     */
    private void deleteFilter(SmsFilter filter) {
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                filterDao.delete(filter);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.filter_delete_success, Toast.LENGTH_SHORT).show();
                    loadFilterRules();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.filter_delete_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * Show filter test dialog
     */
    private void showFilterTestDialog(SmsFilter filter) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_test, null);
        
        EditText etTestMessage = dialogView.findViewById(R.id.etTestMessage);
        EditText etTestSender = dialogView.findViewById(R.id.etTestSender);
        TextView tvTestResult = dialogView.findViewById(R.id.tvTestResult);
        Button btnRunTest = dialogView.findViewById(R.id.btnRunTest);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.filter_test_title)
            .setView(dialogView)
            .setNegativeButton(R.string.cancel, null)
            .create();
        
        btnRunTest.setOnClickListener(v -> {
            String testMessage = etTestMessage.getText().toString();
            String testSender = etTestSender.getText().toString();
            
            // Test filter using FilterEngine
            FilterEngine filterEngine = new FilterEngine(this);
            FilterEngine.FilterResult result = filterEngine.applyFilters(testSender, testMessage, System.currentTimeMillis());
            
            if (result.getMatchedFilter() != null && result.getMatchedFilter().getId() == filter.getId()) {
                String resultText = getString(R.string.filter_test_result_match, result.getMatchedFilter().getAction());
                tvTestResult.setText(resultText);
                tvTestResult.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvTestResult.setText(R.string.filter_test_result_no_match);
                tvTestResult.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
            tvTestResult.setVisibility(View.VISIBLE);
        });
        
        dialog.show();
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