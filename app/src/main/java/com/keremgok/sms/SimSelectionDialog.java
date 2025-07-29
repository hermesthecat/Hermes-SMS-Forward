package com.keremgok.sms;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
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
        loadAvailableSims();
    }
    
    /**
     * Load available SIM cards
     */
    private void loadAvailableSims() {
        try {
            availableSims = SimManager.getActiveSimCards(context);
        } catch (Exception e) {
            availableSims = new ArrayList<>();
        }
    }
    
    /**
     * Show the SIM selection dialog
     */
    public void show() {
        if (!SimManager.isDualSimSupported(context)) {
            Toast.makeText(context, R.string.dual_sim_not_supported, Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onDialogCancelled();
            }
            return;
        }
        
        if (availableSims == null || availableSims.isEmpty()) {
            Toast.makeText(context, R.string.sim_not_available, Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onDialogCancelled();
            }
            return;
        }
        
        // Create custom layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sim_selection, null);
        
        // Setup ListView with SIM cards
        ListView listView = dialogView.findViewById(R.id.listview_sims);
        SimListAdapter adapter = new SimListAdapter();
        listView.setAdapter(adapter);
        
        // Setup item click listener
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < availableSims.size()) {
                SimManager.SimInfo selectedSim = availableSims.get(position);
                if (listener != null) {
                    listener.onSimSelected(selectedSim.subscriptionId, selectedSim.slotIndex, selectedSim.displayName);
                }
            }
        });
        
        // Create and show dialog
        AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle(R.string.sim_selection_title)
            .setView(dialogView)
            .setNegativeButton(R.string.cancel_button, (dialogInterface, which) -> {
                if (listener != null) {
                    listener.onDialogCancelled();
                }
            })
            .create();
            
        dialog.show();
    }
    
    /**
     * Custom adapter for SIM card list
     */
    private class SimListAdapter extends ArrayAdapter<SimManager.SimInfo> {
        
        public SimListAdapter() {
            super(context, R.layout.item_sim_selection, availableSims);
        }
        
        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.item_sim_selection, parent, false);
            }
            
            SimManager.SimInfo simInfo = availableSims.get(position);
            
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
                tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvStatus.setText(R.string.sim_status_inactive);
                tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
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