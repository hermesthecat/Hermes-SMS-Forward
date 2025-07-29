package com.keremgok.sms;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * SMS History Activity
 * Displays forwarded SMS history with search and filtering capabilities
 */
public class HistoryActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private SmsHistoryAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AppDatabase database;
    private List<SmsHistory> allHistory;
    private List<SmsHistory> filteredHistory;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        loadHistoryData();
        
        // Perform auto cleanup on history view
        AppDatabase.performAutoCleanup(this);
    }
    
    /**
     * Initialize all views
     */
    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view_history);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        database = AppDatabase.getInstance(this);
        
        allHistory = new ArrayList<>();
        filteredHistory = new ArrayList<>();
    }
    
    /**
     * Setup toolbar with back navigation
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.history_title));
        }
    }
    
    /**
     * Setup RecyclerView with adapter and layout manager
     */
    private void setupRecyclerView() {
        adapter = new SmsHistoryAdapter(this, filteredHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    /**
     * Setup SwipeRefreshLayout for pull-to-refresh functionality
     */
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadHistoryData);
        swipeRefreshLayout.setColorSchemeResources(
            R.color.hermes_primary,
            R.color.hermes_accent,
            R.color.hermes_secondary
        );
    }
    
    /**
     * Load SMS history data from database in optimized background thread
     */
    private void loadHistoryData() {
        swipeRefreshLayout.setRefreshing(true);
        
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                List<SmsHistory> history = database.smsHistoryDao().getAllHistory();
                
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    allHistory.clear();
                    allHistory.addAll(history);
                    
                    filteredHistory.clear();
                    filteredHistory.addAll(history);
                    
                    adapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);
                    
                    if (history.isEmpty()) {
                        Toast.makeText(this, getString(R.string.history_empty_message), Toast.LENGTH_SHORT).show();
                    }
                });
                
            } catch (Exception e) {
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, getString(R.string.history_load_error), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * Filter history based on search query
     * @param query Search query string
     */
    private void filterHistory(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Show all history when search is empty
            filteredHistory.clear();
            filteredHistory.addAll(allHistory);
        } else {
            // Filter history based on query using optimized background thread
            ThreadManager.getInstance().executeDatabase(() -> {
                try {
                    List<SmsHistory> searchResults = database.smsHistoryDao().searchHistory(query.trim());
                    
                    ThreadManager.getInstance().executeOnMainThread(() -> {
                        filteredHistory.clear();
                        filteredHistory.addAll(searchResults);
                        adapter.notifyDataSetChanged();
                        
                        if (searchResults.isEmpty()) {
                            Toast.makeText(this, getString(R.string.history_search_no_results), Toast.LENGTH_SHORT).show();
                        }
                    });
                    
                } catch (Exception e) {
                    ThreadManager.getInstance().executeOnMainThread(() -> {
                        Toast.makeText(this, getString(R.string.history_search_error), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        
        // Setup search functionality
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.history_search_hint));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterHistory(query);
                    return true;
                }
                
                @Override
                public boolean onQueryTextChange(String newText) {
                    filterHistory(newText);
                    return true;
                }
            });
            
            // Handle search view collapse
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }
                
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    // Reset to show all history when search is collapsed
                    filterHistory("");
                    return true;
                }
            });
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            // Handle back button
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_refresh) {
            // Manual refresh
            loadHistoryData();
            return true;
        } else if (itemId == R.id.action_clear_history) {
            // Clear all history with confirmation
            showClearHistoryConfirmation();
            return true;
        } else if (itemId == R.id.action_filter_success) {
            // Show only successful forwards
            showSuccessfulHistory();
            return true;
        } else if (itemId == R.id.action_filter_failed) {
            // Show only failed forwards
            showFailedHistory();
            return true;
        } else if (itemId == R.id.action_show_all) {
            // Show all history
            loadHistoryData();
            return true;
        } else if (itemId == R.id.action_filter_sim1) {
            // Show only SIM 1 history
            showHistoryByForwardingSim(0);
            return true;
        } else if (itemId == R.id.action_filter_sim2) {
            // Show only SIM 2 history
            showHistoryByForwardingSim(1);
            return true;
        } else if (itemId == R.id.action_filter_by_source_sim) {
            // Show SIM selection dialog for source SIM filtering
            showSourceSimFilterDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Show only successful forwarded SMS
     */
    private void showSuccessfulHistory() {
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                List<SmsHistory> successHistory = database.smsHistoryDao().getSuccessfulHistory();
                
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    filteredHistory.clear();
                    filteredHistory.addAll(successHistory);
                    adapter.notifyDataSetChanged();
                    
                    Toast.makeText(this, getString(R.string.history_showing_successful), Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    Toast.makeText(this, getString(R.string.history_filter_error), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * Show only failed forwarded SMS
     */
    private void showFailedHistory() {
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                List<SmsHistory> failedHistory = database.smsHistoryDao().getFailedHistory();
                
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    filteredHistory.clear();
                    filteredHistory.addAll(failedHistory);
                    adapter.notifyDataSetChanged();
                    
                    Toast.makeText(this, getString(R.string.history_showing_failed), Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    Toast.makeText(this, getString(R.string.history_filter_error), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * Show confirmation dialog for clearing all history
     */
    private void showClearHistoryConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.history_clear_title))
            .setMessage(getString(R.string.history_clear_message))
            .setPositiveButton(getString(R.string.history_clear_confirm), (dialog, which) -> clearAllHistory())
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }
    
    /**
     * Show history filtered by forwarding SIM
     * @param simSlot SIM slot (0 for SIM 1, 1 for SIM 2)
     */
    private void showHistoryByForwardingSim(int simSlot) {
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                List<SmsHistory> simHistory = database.smsHistoryDao().getHistoryByForwardingSim(simSlot);
                
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    filteredHistory.clear();
                    filteredHistory.addAll(simHistory);
                    adapter.notifyDataSetChanged();
                    
                    String simName = simSlot == 0 ? getString(R.string.sim_1) : getString(R.string.sim_2);
                    String message = getString(R.string.history_showing_sim_filter, simName);
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    Toast.makeText(this, getString(R.string.history_filter_error), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * Show source SIM filter dialog
     */
    private void showSourceSimFilterDialog() {
        // Get available SIMs for filtering
        List<SimManager.SimInfo> availableSims = SimManager.getActiveSimCards(this);
        
        if (availableSims == null || availableSims.isEmpty()) {
            Toast.makeText(this, getString(R.string.dual_sim_not_supported), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create dialog items
        String[] simNames = new String[availableSims.size() + 1];
        simNames[0] = getString(R.string.history_show_all);
        
        for (int i = 0; i < availableSims.size(); i++) {
            SimManager.SimInfo sim = availableSims.get(i);
            simNames[i + 1] = String.format("%s (%s)", sim.displayName, sim.carrierName);
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.history_filter_by_source_sim_title))
            .setItems(simNames, (dialog, which) -> {
                if (which == 0) {
                    // Show all history
                    loadHistoryData();
                } else {
                    // Filter by selected source SIM
                    SimManager.SimInfo selectedSim = availableSims.get(which - 1);
                    showHistoryBySourceSubscription(selectedSim.subscriptionId, selectedSim.displayName);
                }
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }
    
    /**
     * Show history filtered by source SIM subscription
     * @param sourceSubscriptionId Source SIM subscription ID
     * @param simDisplayName SIM display name for toast message
     */
    private void showHistoryBySourceSubscription(int sourceSubscriptionId, String simDisplayName) {
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                List<SmsHistory> sourceSimHistory = database.smsHistoryDao().getHistoryBySourceSubscription(sourceSubscriptionId);
                
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    filteredHistory.clear();
                    filteredHistory.addAll(sourceSimHistory);
                    adapter.notifyDataSetChanged();
                    
                    String message = getString(R.string.history_showing_source_sim_filter, simDisplayName);
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    Toast.makeText(this, getString(R.string.history_filter_error), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * Clear all SMS history from database
     */
    private void clearAllHistory() {
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                database.smsHistoryDao().deleteAllHistory();
                
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    allHistory.clear();
                    filteredHistory.clear();
                    adapter.notifyDataSetChanged();
                    
                    Toast.makeText(this, getString(R.string.history_cleared_success), Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    Toast.makeText(this, getString(R.string.history_clear_error), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to activity
        loadHistoryData();
    }
}