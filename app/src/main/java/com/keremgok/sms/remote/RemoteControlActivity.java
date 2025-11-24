package com.keremgok.sms.remote;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.keremgok.sms.AppDatabase;
import com.keremgok.sms.PhoneNumberValidator;
import com.keremgok.sms.R;
import com.keremgok.sms.ThreadManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for managing remote SMS control
 */
public class RemoteControlActivity extends AppCompatActivity {
    
    private SwitchMaterial switchEnabled;
    private TextView tvAuthorizedCount;
    private TextView tvHistoryStats;
    private RecyclerView recyclerAuthorizedNumbers;
    private RecyclerView recyclerCommandHistory;
    private MaterialButton btnAddAuthorizedNumber;
    
    private AuthorizedNumberAdapter authorizedAdapter;
    private CommandHistoryAdapter historyAdapter;
    
    private AppDatabase database;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_control);
        
        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.remote_control_title);
        }
        
        database = AppDatabase.getInstance(this);
        
        initializeViews();
        setupListeners();
        loadData();
    }
    
    private void initializeViews() {
        switchEnabled = findViewById(R.id.switchRemoteControlEnabled);
        tvAuthorizedCount = findViewById(R.id.tvAuthorizedCount);
        tvHistoryStats = findViewById(R.id.tvHistoryStats);
        recyclerAuthorizedNumbers = findViewById(R.id.recyclerViewAuthorizedNumbers);
        recyclerCommandHistory = findViewById(R.id.recyclerViewCommandHistory);
        btnAddAuthorizedNumber = findViewById(R.id.btnAddAuthorizedNumber);
        
        // Setup RecyclerViews
        recyclerAuthorizedNumbers.setLayoutManager(new LinearLayoutManager(this));
        authorizedAdapter = new AuthorizedNumberAdapter(new ArrayList<>(), this::onDeleteAuthorizedNumber);
        recyclerAuthorizedNumbers.setAdapter(authorizedAdapter);
        
        recyclerCommandHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new CommandHistoryAdapter(new ArrayList<>());
        recyclerCommandHistory.setAdapter(historyAdapter);
    }
    
    private void setupListeners() {
        // Enable/disable toggle
        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            RemoteCommandValidator.setEnabled(this, isChecked);
            Toast.makeText(this, 
                isChecked ? R.string.remote_control_enabled : R.string.remote_control_disabled, 
                Toast.LENGTH_SHORT).show();
        });
        
        // Add authorized number button
        btnAddAuthorizedNumber.setOnClickListener(v -> showAddAuthorizedNumberDialog());
    }
    
    private void loadData() {
        // Load enabled state
        switchEnabled.setChecked(RemoteCommandValidator.isEnabled(this));
        
        // Load authorized numbers
        ThreadManager.getInstance().executeDatabase(() -> {
            if (database != null && database.authorizedNumberDao() != null) {
                List<AuthorizedNumber> numbers = database.authorizedNumberDao().getAll();
                runOnUiThread(() -> {
                    authorizedAdapter.updateData(numbers);
                    tvAuthorizedCount.setText(
                        getString(R.string.authorized_numbers_count_value, numbers.size())
                    );
                });
            }
        });
        
        // Load command history (recent 20)
        ThreadManager.getInstance().executeDatabase(() -> {
            if (database != null && database.remoteCommandHistoryDao() != null) {
                List<RemoteCommandHistory> history = database.remoteCommandHistoryDao().getRecent(20);
                int total = database.remoteCommandHistoryDao().count();
                int successful = database.remoteCommandHistoryDao().countSuccessful();
                int failed = database.remoteCommandHistoryDao().countFailed();
                
                runOnUiThread(() -> {
                    historyAdapter.updateData(history);
                    tvHistoryStats.setText(
                        getString(R.string.command_history_stats_value, total, successful, failed)
                    );
                });
            }
        });
    }
    
    private void showAddAuthorizedNumberDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_authorized_number);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        final EditText inputPhone = new EditText(this);
        inputPhone.setHint(R.string.phone_number_hint);
        inputPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        layout.addView(inputPhone);
        
        final EditText inputName = new EditText(this);
        inputName.setHint(R.string.display_name_hint);
        inputName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        layout.addView(inputName);
        
        builder.setView(layout);
        
        builder.setPositiveButton(R.string.add, (dialog, which) -> {
            String phone = inputPhone.getText().toString().trim();
            String name = inputName.getText().toString().trim();
            
            if (phone.isEmpty()) {
                Toast.makeText(this, R.string.phone_number_required, Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!PhoneNumberValidator.isValid(phone)) {
                Toast.makeText(this, R.string.invalid_phone_number, Toast.LENGTH_SHORT).show();
                return;
            }
            
            addAuthorizedNumber(phone, name.isEmpty() ? phone : name);
        });
        
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    
    private void addAuthorizedNumber(String phoneNumber, String displayName) {
        ThreadManager.getInstance().executeDatabase(() -> {
            if (database != null && database.authorizedNumberDao() != null) {
                // Check if already exists
                AuthorizedNumber existing = database.authorizedNumberDao().getByPhoneNumber(phoneNumber);
                if (existing != null) {
                    runOnUiThread(() -> 
                        Toast.makeText(this, R.string.number_already_authorized, Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
                
                // Create new authorized number
                AuthorizedNumber newNumber = AuthorizedNumber.create(phoneNumber, displayName);
                database.authorizedNumberDao().insert(newNumber);
                
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.authorized_number_added, Toast.LENGTH_SHORT).show();
                    loadData(); // Refresh list
                });
            }
        });
    }
    
    private void onDeleteAuthorizedNumber(AuthorizedNumber number) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_authorized_number)
            .setMessage(getString(R.string.delete_authorized_number_confirm, number.getPhoneNumber()))
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                ThreadManager.getInstance().executeDatabase(() -> {
                    if (database != null && database.authorizedNumberDao() != null) {
                        database.authorizedNumberDao().delete(number);
                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.authorized_number_deleted, Toast.LENGTH_SHORT).show();
                            loadData(); // Refresh list
                        });
                    }
                });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // Simple adapters
    
    private static class AuthorizedNumberAdapter extends RecyclerView.Adapter<AuthorizedNumberAdapter.ViewHolder> {
        private List<AuthorizedNumber> numbers;
        private final OnDeleteClickListener deleteListener;
        
        interface OnDeleteClickListener {
            void onDelete(AuthorizedNumber number);
        }
        
        AuthorizedNumberAdapter(List<AuthorizedNumber> numbers, OnDeleteClickListener deleteListener) {
            this.numbers = numbers;
            this.deleteListener = deleteListener;
        }
        
        void updateData(List<AuthorizedNumber> newNumbers) {
            this.numbers = newNumbers;
            notifyDataSetChanged();
        }
        
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_authorized_number, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            AuthorizedNumber number = numbers.get(position);
            holder.tvPhoneNumber.setText(number.getPhoneNumber());
            holder.tvDisplayName.setText(number.getDisplayName());
            holder.tvCommandCount.setText(
                holder.itemView.getContext().getString(
                    R.string.commands_sent_count, 
                    number.getTotalCommandsSent()
                )
            );
            holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(number));
        }
        
        @Override
        public int getItemCount() {
            return numbers.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPhoneNumber;
            TextView tvDisplayName;
            TextView tvCommandCount;
            View btnDelete;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
                tvDisplayName = itemView.findViewById(R.id.tvDisplayName);
                tvCommandCount = itemView.findViewById(R.id.tvCommandCount);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
    
    private static class CommandHistoryAdapter extends RecyclerView.Adapter<CommandHistoryAdapter.ViewHolder> {
        private List<RemoteCommandHistory> history;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        
        CommandHistoryAdapter(List<RemoteCommandHistory> history) {
            this.history = history;
        }
        
        void updateData(List<RemoteCommandHistory> newHistory) {
            this.history = newHistory;
            notifyDataSetChanged();
        }
        
        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_command_history, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            RemoteCommandHistory cmd = history.get(position);
            
            // Status icon
            if (cmd.isSuccess()) {
                holder.tvStatus.setText("✅");
            } else if (RemoteCommandHistory.Status.UNAUTHORIZED.name().equals(cmd.getExecutionStatus())) {
                holder.tvStatus.setText("🔒");
            } else if (RemoteCommandHistory.Status.FAILED.name().equals(cmd.getExecutionStatus())) {
                holder.tvStatus.setText("❌");
            } else {
                holder.tvStatus.setText("⏳");
            }
            
            // Sender
            holder.tvSender.setText("From: " + maskNumber(cmd.getSenderNumber()));
            
            // Target
            if (cmd.getParsedTarget() != null) {
                holder.tvTarget.setText(
                    "To: " + maskNumber(cmd.getParsedTarget()) + 
                    " via " + cmd.getParsedSim()
                );
            } else {
                holder.tvTarget.setText(cmd.getResultMessage());
            }
            
            // Timestamp
            holder.tvTimestamp.setText(dateFormat.format(new Date(cmd.getReceivedTimestamp())));
        }
        
        @Override
        public int getItemCount() {
            return history.size();
        }
        
        private String maskNumber(String number) {
            if (number == null || number.length() < 4) return "***";
            return number.substring(0, 4) + "***" + (number.length() > 7 ? number.substring(number.length() - 3) : "");
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStatus;
            TextView tvSender;
            TextView tvTarget;
            TextView tvTimestamp;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvSender = itemView.findViewById(R.id.tvSender);
                tvTarget = itemView.findViewById(R.id.tvTarget);
                tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            }
        }
    }
}
