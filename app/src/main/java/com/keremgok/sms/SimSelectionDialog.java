package com.keremgok.sms;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for SIM card selection and configuration
 * Shows available SIM cards with their status and allows selection
 */
public class SimSelectionDialog {
    
    private static final String TAG = "SimSelectionDialog";
    private Context context;
    private OnSimSelectedListener listener;
    private List<SimManager.SimInfo> availableSims;
    
    /**
     * Interface for SIM selection callback
     */
    public interface OnSimSelectedListener {
        void onSimSelected(int subscriptionId, int simSlot, String displayName);
        void onDialogCancelled();
    }
    
    /**
     * Constructor
     * @param context The context to create dialog in
     * @param listener Callback for SIM selection events
     */
    public SimSelectionDialog(Context context, OnSimSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        // Don't load SIMs in constructor to avoid UI thread I/O
        this.availableSims = new ArrayList<>();
    }
    
    /**
     * Load available SIM cards in background thread
     * @param callback Callback to run on UI thread after loading
     */
    private void loadAvailableSims(Runnable callback) {
        ThreadManager.getInstance().executeBackground(() -> {
            try {
                List<SimManager.SimInfo> sims = SimManager.getActiveSimCards(context);
                
                // Update on UI thread
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        this.availableSims = sims;
                        if (callback != null) {
                            callback.run();
                        }
                    });
                } else {
                    // Fallback for non-Activity contexts
                    this.availableSims = sims;
                    if (callback != null) {
                        callback.run();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading available SIMs: " + e.getMessage(), e);
                
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        this.availableSims = new ArrayList<>();
                        if (callback != null) {
                            callback.run();
                        }
                    });
                } else {
                    this.availableSims = new ArrayList<>();
                    if (callback != null) {
                        callback.run();
                    }
                }
            }
        });
    }
    
    /**
     * Show the SIM selection dialog
     */
    public void show() {
        Log.d(TAG, "SimSelectionDialog.show() called");
        
        // Check dual SIM support in background thread to prevent ANR
        ThreadManager.getInstance().executeBackground(() -> {
            boolean isDualSimSupported = SimManager.isDualSimSupported(context);
            
            // Update UI on main thread
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    if (!isDualSimSupported) {
                        Log.d(TAG, "Dual SIM not supported");
                        Toast.makeText(context, R.string.dual_sim_not_supported, Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onDialogCancelled();
                        }
                        return;
                    }
                    
                    // Load SIMs and show dialog
                    showLoadingAndDialog();
                });
            } else {
                // Fallback for non-Activity contexts
                if (!isDualSimSupported) {
                    Log.d(TAG, "Dual SIM not supported");
                    Toast.makeText(context, R.string.dual_sim_not_supported, Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onDialogCancelled();
                    }
                    return;
                }
                
                showLoadingAndDialog();
            }
        });
    }
    
    /**
     * Show loading indicator and then the actual dialog
     */
    private void showLoadingAndDialog() {
        // Load available SIMs in background
        loadAvailableSims(() -> {
            if (availableSims == null || availableSims.isEmpty()) {
                Log.d(TAG, "No available SIMs found");
                Toast.makeText(context, R.string.sim_not_available, Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onDialogCancelled();
                }
                return;
            }
            
            Log.d(TAG, "Found " + availableSims.size() + " available SIMs");
            
            // Create custom layout
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sim_selection, null);
            
            // Setup ListView with SIM cards
            ListView listView = dialogView.findViewById(R.id.listview_sims);
            
            // Create dialog first to have reference for dismissing
            AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.sim_selection_title)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel_button, (dialogInterface, which) -> {
                    if (listener != null) {
                        listener.onDialogCancelled();
                    }
                })
                .create();
            
            // Create adapter with dialog reference for dismissal
            SimListAdapter adapter = new SimListAdapter(dialog);
            listView.setAdapter(adapter);
                
            dialog.show();
        });
    }
    
    /**
     * Custom adapter for SIM card list
     */
    private class SimListAdapter extends ArrayAdapter<SimManager.SimInfo> {
        
        private AlertDialog dialog;
        
        public SimListAdapter(AlertDialog dialog) {
            super(context, R.layout.item_sim_selection, availableSims);
            this.dialog = dialog;
        }
        
        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.item_sim_selection, parent, false);
            }
            
            SimManager.SimInfo simInfo = availableSims.get(position);
            
            // Add manual click listener to the view itself
            final int finalPosition = position;
            view.setOnClickListener(v -> {
                Log.d(TAG, "Manual click listener triggered for position: " + finalPosition);
                if (finalPosition >= 0 && finalPosition < availableSims.size()) {
                    SimManager.SimInfo selectedSim = availableSims.get(finalPosition);
                    Log.d(TAG, "Selected SIM: " + selectedSim.displayName + " (slot=" + selectedSim.slotIndex + ")");
                    if (listener != null) {
                        Log.d(TAG, "Calling onSimSelected callback");
                        listener.onSimSelected(selectedSim.subscriptionId, selectedSim.slotIndex, selectedSim.displayName);
                    }
                    // Dismiss dialog after selection
                    Log.d(TAG, "Manual click - dismissing dialog");
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            });
            
            // SIM display name
            TextView tvDisplayName = view.findViewById(R.id.tv_sim_display_name);
            String displayName = simInfo.displayName;
            if (TextUtils.isEmpty(displayName)) {
                displayName = context.getString(R.string.sim_slot_format, simInfo.slotIndex + 1);
            }
            tvDisplayName.setText(displayName);
            
            // Carrier name
            TextView tvCarrierName = view.findViewById(R.id.tv_carrier_name);
            if (!TextUtils.isEmpty(simInfo.carrierName)) {
                tvCarrierName.setText(simInfo.carrierName);
                tvCarrierName.setVisibility(View.VISIBLE);
            } else {
                tvCarrierName.setVisibility(View.GONE);
            }
            
            // Phone number (masked)
            TextView tvPhoneNumber = view.findViewById(R.id.tv_phone_number);
            if (!TextUtils.isEmpty(simInfo.phoneNumber)) {
                String maskedNumber = maskPhoneNumber(simInfo.phoneNumber);
                tvPhoneNumber.setText(maskedNumber);
                tvPhoneNumber.setVisibility(View.VISIBLE);
            } else {
                tvPhoneNumber.setVisibility(View.GONE);
            }
            
            // SIM status
            TextView tvStatus = view.findViewById(R.id.tv_sim_status);
            if (simInfo.isActive) {
                tvStatus.setText(R.string.sim_status_active);
                tvStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.holo_green_dark));
            } else {
                tvStatus.setText(R.string.sim_status_inactive);
                tvStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.holo_red_dark));
            }
            
            // Slot indicator
            TextView tvSlotIndicator = view.findViewById(R.id.tv_slot_indicator);
            tvSlotIndicator.setText(context.getString(R.string.sim_slot_format, simInfo.slotIndex + 1));
            
            return view;
        }
    }
    
    /**
     * Mask phone number for display
     * @param phoneNumber The phone number to mask
     * @return Masked phone number
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() < 8) {
            return "***";
        }
        
        String prefix = phoneNumber.substring(0, Math.min(5, phoneNumber.length() - 4));
        String suffix = phoneNumber.substring(phoneNumber.length() - 4);
        return prefix + "***" + suffix;
    }
}