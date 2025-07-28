package com.keremgok.sms;

import android.content.Context;
import android.graphics.Color;
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
 * RecyclerView Adapter for SMS Filter Rules
 * Displays filter rules with actions for management
 */
public class FilterRulesAdapter extends RecyclerView.Adapter<FilterRulesAdapter.FilterRuleViewHolder> {

    private Context context;
    private List<SmsFilter> filterRules;
    private OnFilterRuleActionListener listener;
    
    /**
     * Interface for handling filter rule actions
     */
    public interface OnFilterRuleActionListener {
        void onTestFilter(SmsFilter filter);
        void onToggleEnabled(SmsFilter filter);
        void onEditFilter(SmsFilter filter);
        void onDeleteFilter(SmsFilter filter);
    }

    public FilterRulesAdapter(Context context, OnFilterRuleActionListener listener) {
        this.context = context;
        this.filterRules = new ArrayList<>();
        this.listener = listener;
    }

    /**
     * Update the list of filter rules
     * @param newFilterRules The new list of filter rules
     */
    public void updateFilterRules(List<SmsFilter> newFilterRules) {
        this.filterRules.clear();
        if (newFilterRules != null) {
            this.filterRules.addAll(newFilterRules);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FilterRuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_filter_rule, parent, false);
        return new FilterRuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterRuleViewHolder holder, int position) {
        SmsFilter filter = filterRules.get(position);
        holder.bind(filter);
    }

    @Override
    public int getItemCount() {
        return filterRules.size();
    }

    /**
     * ViewHolder for Filter Rule items
     */
    class FilterRuleViewHolder extends RecyclerView.ViewHolder {
        private TextView tvFilterName;
        private TextView tvFilterType;
        private TextView tvFilterAction;
        private TextView tvFilterPattern;
        private TextView tvPriority;
        private TextView tvMatchCount;
        private TextView tvLastMatched;
        private TextView tvEnabledBadge;
        private TextView tvDisabledBadge;
        private Button btnTestFilter;
        private Button btnToggleEnabled;
        private Button btnEditFilter;
        private Button btnDeleteFilter;

        public FilterRuleViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvFilterName = itemView.findViewById(R.id.tvFilterName);
            tvFilterType = itemView.findViewById(R.id.tvFilterType);
            tvFilterAction = itemView.findViewById(R.id.tvFilterAction);
            tvFilterPattern = itemView.findViewById(R.id.tvFilterPattern);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvMatchCount = itemView.findViewById(R.id.tvMatchCount);
            tvLastMatched = itemView.findViewById(R.id.tvLastMatched);
            tvEnabledBadge = itemView.findViewById(R.id.tvEnabledBadge);
            tvDisabledBadge = itemView.findViewById(R.id.tvDisabledBadge);
            btnTestFilter = itemView.findViewById(R.id.btnTestFilter);
            btnToggleEnabled = itemView.findViewById(R.id.btnToggleEnabled);
            btnEditFilter = itemView.findViewById(R.id.btnEditFilter);
            btnDeleteFilter = itemView.findViewById(R.id.btnDeleteFilter);
        }

        public void bind(SmsFilter filter) {
            // Filter name
            tvFilterName.setText(filter.getFilterName());
            
            // Filter type with readable format
            String filterType = getReadableFilterType(filter.getFilterType());
            tvFilterType.setText(filterType);
            
            // Filter action with color coding
            String action = filter.getAction();
            tvFilterAction.setText(action);
            if (SmsFilter.ACTION_ALLOW.equals(action)) {
                tvFilterAction.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
            } else {
                tvFilterAction.setBackgroundColor(Color.parseColor("#F44336")); // Red
            }
            
            // Pattern with formatting
            String pattern = filter.getPattern();
            if (!TextUtils.isEmpty(pattern)) {
                String patternText = context.getString(R.string.filter_pattern_prefix) + pattern;
                if (filter.isRegex()) {
                    patternText += context.getString(R.string.filter_regex_suffix);
                }
                if (filter.isCaseSensitive()) {
                    patternText += context.getString(R.string.filter_case_sensitive_suffix);
                }
                tvFilterPattern.setText(patternText);
            } else {
                tvFilterPattern.setText(context.getString(R.string.no_pattern_specified));
            }
            
            // Priority
            tvPriority.setText(String.valueOf(filter.getPriority()));
            
            // Match count
            tvMatchCount.setText(String.valueOf(filter.getMatchCount()));
            
            // Last matched
            if (filter.getLastMatched() > 0) {
                String lastMatchedText = formatLastMatched(filter.getLastMatched());
                tvLastMatched.setText(context.getString(R.string.filter_last_prefix) + lastMatchedText);
            } else {
                tvLastMatched.setText(context.getString(R.string.filter_last_never_text));
            }
            
            // Enabled/Disabled badge
            if (filter.isEnabled()) {
                tvEnabledBadge.setVisibility(View.VISIBLE);
                tvDisabledBadge.setVisibility(View.GONE);
                btnToggleEnabled.setText(context.getString(R.string.disable_button));
            } else {
                tvEnabledBadge.setVisibility(View.GONE);
                tvDisabledBadge.setVisibility(View.VISIBLE);
                btnToggleEnabled.setText(context.getString(R.string.enable_button));
            }
            
            // Button click listeners
            btnTestFilter.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTestFilter(filter);
                }
            });
            
            btnToggleEnabled.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToggleEnabled(filter);
                }
            });
            
            btnEditFilter.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditFilter(filter);
                }
            });
            
            btnDeleteFilter.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteFilter(filter);
                }
            });
        }
        
        /**
         * Convert filter type to readable format
         */
        private String getReadableFilterType(String filterType) {
            switch (filterType) {
                case SmsFilter.TYPE_KEYWORD:
                    return "Keyword Filter";
                case SmsFilter.TYPE_SENDER_NUMBER:
                    return "Sender Number Filter";
                case SmsFilter.TYPE_TIME_BASED:
                    return "Time-based Filter";
                case SmsFilter.TYPE_WHITELIST:
                    return "Whitelist";
                case SmsFilter.TYPE_BLACKLIST:
                    return "Blacklist";
                case SmsFilter.TYPE_SPAM_DETECTION:
                    return "Spam Detection";
                default:
                    return filterType;
            }
        }
        
        /**
         * Format last matched timestamp for display
         */
        private String formatLastMatched(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            
            // Less than 1 minute
            if (diff < 60 * 1000) {
                return "Just now";
            }
            
            // Less than 1 hour
            if (diff < 60 * 60 * 1000) {
                int minutes = (int) (diff / (60 * 1000));
                return minutes + "m ago";
            }
            
            // Less than 1 day
            if (diff < 24 * 60 * 60 * 1000) {
                int hours = (int) (diff / (60 * 60 * 1000));
                return hours + "h ago";
            }
            
            // Less than 1 week
            if (diff < 7 * 24 * 60 * 60 * 1000) {
                int days = (int) (diff / (24 * 60 * 60 * 1000));
                return days + "d ago";
            }
            
            // More than 1 week - show date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}