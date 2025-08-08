package com.keremgok.sms;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import androidx.collection.LruCache;

/**
 * RecyclerView Adapter for SMS History
 * Displays SMS forwarding history with date grouping and status indicators
 */
public class SmsHistoryAdapter extends RecyclerView.Adapter<SmsHistoryAdapter.HistoryViewHolder> {
    
    private Context context;
    private List<SmsHistory> historyList;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat dateComparisonFormat;
    
    // Performance optimization: Cached resources and objects
    private Drawable successIconDrawable;
    private Drawable errorIconDrawable;
    private int successColor;
    private int errorColor;
    private String successStatusText;
    private String failedStatusText;
    private String todayText;
    private String yesterdayText;
    
    // Phone number masking cache - LruCache for better memory management
    private LruCache<String, String> phoneNumberCache;
    
    // Date formatting cache - LruCache for better memory management
    private LruCache<Long, String> dateStringCache;
    private LruCache<Long, String> dateGroupTextCache;
    
    public SmsHistoryAdapter(Context context, List<SmsHistory> historyList) {
        this.context = context;
        this.historyList = historyList;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        this.dateComparisonFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Initialize performance optimization caches
        initializeCaches();
    }
    
    /**
     * Initialize performance optimization caches
     */
    private void initializeCaches() {
        // Cache frequently used resources
        successIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_check_circle);
        errorIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_error_circle);
        successColor = ContextCompat.getColor(context, R.color.success_color);
        errorColor = ContextCompat.getColor(context, R.color.error_color);
        successStatusText = context.getString(R.string.history_status_success);
        failedStatusText = context.getString(R.string.history_status_failed);
        todayText = context.getString(R.string.history_date_today);
        yesterdayText = context.getString(R.string.history_date_yesterday);
        
        // Initialize LruCache with reasonable maximum capacities for better memory management
        phoneNumberCache = new LruCache<>(50); // Cache up to 50 phone numbers
        dateStringCache = new LruCache<>(30);   // Cache up to 30 date strings
        dateGroupTextCache = new LruCache<>(10); // Cache up to 10 date group texts
    }
    
    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sms_history, parent, false);
        return new HistoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SmsHistory history = historyList.get(position);
        
        // Bind data to views
        bindHistoryData(holder, history);
        
        // Handle date grouping
        handleDateGrouping(holder, history, position);
        
        // Set click listener for item expansion
        holder.itemView.setOnClickListener(v -> toggleMessageExpansion(holder));
    }
    
    /**
     * Bind SMS history data to view holder (optimized with caching)
     */
    private void bindHistoryData(HistoryViewHolder holder, SmsHistory history) {
        // Set sender number (masked for privacy) - use cache
        String maskedSender = getCachedMaskedPhoneNumber(history.getSenderNumber());
        holder.tvSenderNumber.setText(maskedSender);
        
        // Set timestamp - optimize date formatting
        holder.tvTimestamp.setText(timeFormat.format(history.getTimestamp()));
        
        // Set original message
        holder.tvOriginalMessage.setText(history.getOriginalMessage());
        
        // Set target number (masked for privacy) - use cache
        String maskedTarget = getCachedMaskedPhoneNumber(history.getTargetNumber());
        holder.tvTargetNumber.setText(maskedTarget);
        
        // Set status icon and text based on success/failure - use cached resources
        if (history.isSuccess()) {
            // Success status - use cached resources
            holder.ivStatusIcon.setImageDrawable(successIconDrawable);
            holder.ivStatusIcon.setColorFilter(successColor);
            holder.tvStatusText.setText(successStatusText);
            holder.tvStatusText.setTextColor(successColor);
            holder.tvStatusText.setBackgroundResource(R.drawable.success_status_background);
            
            // Hide error container
            holder.llErrorContainer.setVisibility(View.GONE);
            
        } else {
            // Failed status - use cached resources
            holder.ivStatusIcon.setImageDrawable(errorIconDrawable);
            holder.ivStatusIcon.setColorFilter(errorColor);
            holder.tvStatusText.setText(failedStatusText);
            holder.tvStatusText.setTextColor(errorColor);
            holder.tvStatusText.setBackgroundResource(R.drawable.error_status_background);
            
            // Show error message if available
            String errorMessage = history.getErrorMessage();
            if (errorMessage != null && !errorMessage.trim().isEmpty()) {
                holder.llErrorContainer.setVisibility(View.VISIBLE);
                holder.tvErrorMessage.setText(errorMessage);
            } else {
                holder.llErrorContainer.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Get cached masked phone number for performance optimization
     */
    private String getCachedMaskedPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "***";
        }
        
        String cached = phoneNumberCache.get(phoneNumber);
        if (cached != null) {
            return cached;
        }
        
        String masked = PhoneNumberValidator.maskPhoneNumber(phoneNumber);
        phoneNumberCache.put(phoneNumber, masked);
        
        return masked;
    }
    
    /**
     * Handle date-based grouping for SMS history
     */
    private void handleDateGrouping(HistoryViewHolder holder, SmsHistory history, int position) {
        boolean showDateDivider = false;
        String dateText = "";
        
        if (position == 0) {
            // Always show date divider for first item
            showDateDivider = true;
            dateText = getCachedDateGroupText(history.getTimestamp());
        } else {
            // Show date divider if this item is from a different date than previous item
            SmsHistory previousHistory = historyList.get(position - 1);
            String currentDate = getCachedDateString(history.getTimestamp());
            String previousDate = getCachedDateString(previousHistory.getTimestamp());
            
            if (!currentDate.equals(previousDate)) {
                showDateDivider = true;
                dateText = getCachedDateGroupText(history.getTimestamp());
            }
        }
        
        if (showDateDivider) {
            holder.llDateDivider.setVisibility(View.VISIBLE);
            holder.tvDateDivider.setText(dateText);
        } else {
            holder.llDateDivider.setVisibility(View.GONE);
        }
    }
    
    /**
     * Get cached date string for comparison (YYYY-MM-DD format) - optimized
     */
    private String getCachedDateString(long timestamp) {
        Long key = timestamp;
        String cached = dateStringCache.get(key);
        if (cached != null) {
            return cached;
        }
        
        String dateString = dateComparisonFormat.format(new Date(timestamp));
        dateStringCache.put(key, dateString);
        
        return dateString;
    }
    
    /**
     * Get cached user-friendly date group text (Today, Yesterday, or formatted date) - optimized
     */
    private String getCachedDateGroupText(long timestamp) {
        Long key = timestamp;
        String cached = dateGroupTextCache.get(key);
        if (cached != null) {
            return cached;
        }
        
        String dateGroupText = getDateGroupText(timestamp);
        dateGroupTextCache.put(key, dateGroupText);
        
        return dateGroupText;
    }
    
    /**
     * Get date string for comparison (YYYY-MM-DD format)
     */
    private String getDateString(long timestamp) {
        return dateComparisonFormat.format(new Date(timestamp));
    }
    
    /**
     * Get user-friendly date group text (Today, Yesterday, or formatted date)
     */
    private String getDateGroupText(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        
        if (isSameDay(calendar, today)) {
            return todayText;
        } else if (isSameDay(calendar, yesterday)) {
            return yesterdayText;
        } else {
            return dateFormat.format(new Date(timestamp));
        }
    }
    
    /**
     * Check if two calendars represent the same day
     */
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    /**
     * Toggle message expansion for long messages
     */
    private void toggleMessageExpansion(HistoryViewHolder holder) {
        TextView messageView = holder.tvOriginalMessage;
        
        if (messageView.getMaxLines() == 3) {
            // Expand message
            messageView.setMaxLines(Integer.MAX_VALUE);
        } else {
            // Collapse message
            messageView.setMaxLines(3);
        }
    }
    
    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }
    
    /**
     * Update adapter data and refresh view
     */
    public void updateData(List<SmsHistory> newHistoryList) {
        this.historyList = newHistoryList;
        notifyDataSetChanged();
    }
    
    /**
     * ViewHolder class for SMS History items
     */
    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        
        ImageView ivStatusIcon;
        TextView tvSenderNumber;
        TextView tvTimestamp;
        TextView tvOriginalMessage;
        TextView tvTargetNumber;
        TextView tvStatusText;
        LinearLayout llErrorContainer;
        TextView tvErrorMessage;
        LinearLayout llDateDivider;
        TextView tvDateDivider;
        
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            
            // Initialize views
            ivStatusIcon = itemView.findViewById(R.id.iv_status_icon);
            tvSenderNumber = itemView.findViewById(R.id.tv_sender_number);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvOriginalMessage = itemView.findViewById(R.id.tv_original_message);
            tvTargetNumber = itemView.findViewById(R.id.tv_target_number);
            tvStatusText = itemView.findViewById(R.id.tv_status_text);
            llErrorContainer = itemView.findViewById(R.id.ll_error_container);
            tvErrorMessage = itemView.findViewById(R.id.tv_error_message);
            llDateDivider = itemView.findViewById(R.id.ll_date_divider);
            tvDateDivider = itemView.findViewById(R.id.tv_date_divider);
        }
    }
}