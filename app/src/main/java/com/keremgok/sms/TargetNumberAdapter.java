package com.keremgok.sms;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for Target Numbers
 * Displays target numbers with actions for management
 */
public class TargetNumberAdapter extends RecyclerView.Adapter<TargetNumberAdapter.TargetNumberViewHolder> {

    private Context context;
    private List<TargetNumber> targetNumbers;
    private OnTargetNumberActionListener listener;
    
    /**
     * Interface for handling target number actions
     */
    public interface OnTargetNumberActionListener {
        void onSetPrimary(TargetNumber targetNumber);
        void onToggleEnabled(TargetNumber targetNumber);
        void onDelete(TargetNumber targetNumber);
        void onEdit(TargetNumber targetNumber);
    }

    public TargetNumberAdapter(Context context, OnTargetNumberActionListener listener) {
        this.context = context;
        this.targetNumbers = new ArrayList<>();
        this.listener = listener;
    }

    /**
     * Update the list of target numbers
     * @param newTargetNumbers The new list of target numbers
     */
    public void updateTargetNumbers(List<TargetNumber> newTargetNumbers) {
        this.targetNumbers.clear();
        if (newTargetNumbers != null) {
            this.targetNumbers.addAll(newTargetNumbers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TargetNumberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_target_number, parent, false);
        return new TargetNumberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TargetNumberViewHolder holder, int position) {
        TargetNumber targetNumber = targetNumbers.get(position);
        holder.bind(targetNumber);
    }

    @Override
    public int getItemCount() {
        return targetNumbers.size();
    }

    /**
     * ViewHolder for Target Number items
     */
    class TargetNumberViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDisplayName;
        private TextView tvPhoneNumber;
        private TextView tvLastUsed;
        private TextView tvPrimaryBadge;
        private TextView tvDisabledBadge;
        private TextView tvSimBadge;
        private Button btnSetPrimary;
        private Button btnToggleEnabled;
        private Button btnEdit;
        private Button btnDelete;

        public TargetNumberViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvDisplayName = itemView.findViewById(R.id.tvDisplayName);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            tvLastUsed = itemView.findViewById(R.id.tvLastUsed);
            tvPrimaryBadge = itemView.findViewById(R.id.tvPrimaryBadge);
            tvDisabledBadge = itemView.findViewById(R.id.tvDisabledBadge);
            tvSimBadge = itemView.findViewById(R.id.tvSimBadge);
            btnSetPrimary = itemView.findViewById(R.id.btnSetPrimary);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnToggleEnabled = itemView.findViewById(R.id.btnToggleEnabled);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(TargetNumber targetNumber) {
            // Display name or phone number if no display name
            String displayName = targetNumber.getDisplayName();
            if (TextUtils.isEmpty(displayName)) {
                tvDisplayName.setText(targetNumber.getPhoneNumber());
            } else {
                tvDisplayName.setText(displayName);
            }
            
            // Phone number
            tvPhoneNumber.setText(targetNumber.getPhoneNumber());
            
            // Last used information
            if (targetNumber.getLastUsedTimestamp() > 0) {
                String lastUsedText = formatLastUsed(targetNumber.getLastUsedTimestamp());
                tvLastUsed.setText(context.getString(R.string.target_last_used_prefix) + lastUsedText);
                tvLastUsed.setVisibility(View.VISIBLE);
            } else {
                tvLastUsed.setText(context.getString(R.string.target_not_used_yet));
                tvLastUsed.setVisibility(View.VISIBLE);
            }
            
            // Primary badge
            if (targetNumber.isPrimary()) {
                tvPrimaryBadge.setVisibility(View.VISIBLE);
                btnSetPrimary.setVisibility(View.GONE);
            } else {
                tvPrimaryBadge.setVisibility(View.GONE);
                btnSetPrimary.setVisibility(View.VISIBLE);
            }
            
            // Disabled badge
            if (!targetNumber.isEnabled()) {
                tvDisabledBadge.setVisibility(View.VISIBLE);
                btnToggleEnabled.setText(context.getString(R.string.target_enable));
            } else {
                tvDisabledBadge.setVisibility(View.GONE);
                btnToggleEnabled.setText(context.getString(R.string.target_disable));
            }
            
            // SIM selection badge
            updateSimBadge(targetNumber);
            
            // Button click listeners
            btnSetPrimary.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSetPrimary(targetNumber);
                }
            });
            
            btnToggleEnabled.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToggleEnabled(targetNumber);
                }
            });
            
            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(targetNumber);
                }
            });
            
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(targetNumber);
                }
            });
        }
        
        /**
         * Update SIM selection badge display
         * @param targetNumber The target number with SIM configuration
         */
        private void updateSimBadge(TargetNumber targetNumber) {
            try {
                // Check if dual SIM is supported
                if (!SimManager.isDualSimSupported(context)) {
                    tvSimBadge.setVisibility(View.GONE);
                    return;
                }
                
                String simSelectionMode = targetNumber.getSimSelectionMode();
                if (TextUtils.isEmpty(simSelectionMode)) {
                    simSelectionMode = "auto";
                }
                
                String badgeText = "";
                switch (simSelectionMode.toLowerCase()) {
                    case "auto":
                        badgeText = "AUTO";
                        break;
                    case "source_sim":
                        badgeText = "SOURCE";
                        break;
                    case "specific_sim":
                        int simSlot = targetNumber.getPreferredSimSlot();
                        if (simSlot >= 0) {
                            badgeText = context.getString(R.string.sim_slot_format, simSlot + 1);
                        } else {
                            badgeText = "SIM";
                        }
                        break;
                    default:
                        badgeText = "AUTO";
                        break;
                }
                
                if (!TextUtils.isEmpty(badgeText)) {
                    tvSimBadge.setText(badgeText);
                    tvSimBadge.setVisibility(View.VISIBLE);
                } else {
                    tvSimBadge.setVisibility(View.GONE);
                }
                
            } catch (Exception e) {
                // Hide badge on error
                tvSimBadge.setVisibility(View.GONE);
            }
        }
        
        /**
         * Format last used timestamp for display
         * @param timestamp The timestamp to format
         * @return Formatted string
         */
        private String formatLastUsed(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            
            // Less than 1 minute
            if (diff < 60 * 1000) {
                return "Az önce";
            }
            
            // Less than 1 hour
            if (diff < 60 * 60 * 1000) {
                int minutes = (int) (diff / (60 * 1000));
                return minutes + " dakika önce";
            }
            
            // Less than 1 day
            if (diff < 24 * 60 * 60 * 1000) {
                int hours = (int) (diff / (60 * 60 * 1000));
                return hours + " saat önce";
            }
            
            // Less than 1 week
            if (diff < 7 * 24 * 60 * 60 * 1000) {
                int days = (int) (diff / (24 * 60 * 60 * 1000));
                return days + " gün önce";
            }
            
            // More than 1 week - show date
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}