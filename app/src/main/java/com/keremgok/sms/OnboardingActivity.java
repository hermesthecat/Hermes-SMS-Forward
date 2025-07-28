package com.keremgok.sms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.keremgok.sms.onboarding.WelcomeFragment;
import com.keremgok.sms.onboarding.PermissionExplanationFragment;
import com.keremgok.sms.onboarding.TargetNumberSetupFragment;
import com.keremgok.sms.onboarding.FilterIntroFragment;
import com.keremgok.sms.onboarding.CompletionFragment;

public class OnboardingActivity extends AppCompatActivity {
    
    private static final String PREF_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final int NUM_PAGES = 5;
    
    private ViewPager2 viewPager;
    private TextView stepIndicator;
    private LinearProgressIndicator progressIndicator;
    private MaterialButton btnSkip, btnBack, btnNext;
    
    private OnboardingPagerAdapter pagerAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        
        initViews();
        setupViewPager();
        setupClickListeners();
        updateUI(0);
    }
    
    private void initViews() {
        viewPager = findViewById(R.id.viewpager_onboarding);
        stepIndicator = findViewById(R.id.tv_step_indicator);
        progressIndicator = findViewById(R.id.progress_indicator);
        btnSkip = findViewById(R.id.btn_skip);
        btnBack = findViewById(R.id.btn_back);
        btnNext = findViewById(R.id.btn_next);
    }
    
    private void setupViewPager() {
        pagerAdapter = new OnboardingPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateUI(position);
            }
        });
    }
    
    private void setupClickListeners() {
        btnSkip.setOnClickListener(v -> finishOnboarding());
        
        btnBack.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem > 0) {
                viewPager.setCurrentItem(currentItem - 1);
            }
        });
        
        btnNext.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            
            // Save data from current fragment before proceeding
            if (currentItem == 2) { // TargetNumberSetupFragment is at position 2
                saveCurrentFragmentData();
            }
            
            if (currentItem < NUM_PAGES - 1) {
                viewPager.setCurrentItem(currentItem + 1);
            } else {
                finishOnboarding();
            }
        });
    }
    
    private void updateUI(int position) {
        // Update step indicator
        stepIndicator.setText(getString(R.string.step_indicator, position + 1, NUM_PAGES));
        
        // Update progress
        int progress = ((position + 1) * 100) / NUM_PAGES;
        progressIndicator.setProgress(progress);
        
        // Update button visibility and text
        btnBack.setVisibility(position > 0 ? View.VISIBLE : View.GONE);
        
        if (position == NUM_PAGES - 1) {
            btnNext.setText(getString(R.string.get_started));
            btnNext.setIcon(null);
            btnSkip.setVisibility(View.GONE);
        } else {
            btnNext.setText(getString(R.string.next));
            btnNext.setIcon(getDrawable(R.drawable.ic_arrow_forward));
            btnSkip.setVisibility(View.VISIBLE);
        }
    }
    
    private void finishOnboarding() {
        // Mark onboarding as completed
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_ONBOARDING_COMPLETED, true).apply();
        
        // Navigate to MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void saveCurrentFragmentData() {
        // Get current fragment from adapter
        Fragment currentFragment = pagerAdapter.getCurrentFragment();
        if (currentFragment instanceof TargetNumberSetupFragment) {
            ((TargetNumberSetupFragment) currentFragment).saveTargetNumber();
        }
    }
    
    public static boolean isOnboardingCompleted(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_ONBOARDING_COMPLETED, false);
    }
    
    private static class OnboardingPagerAdapter extends FragmentStateAdapter {
        
        private FragmentActivity fragmentActivity;
        
        public OnboardingPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
            this.fragmentActivity = fragmentActivity;
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new WelcomeFragment();
                case 1:
                    return new PermissionExplanationFragment();
                case 2:
                    return new TargetNumberSetupFragment();
                case 3:
                    return new FilterIntroFragment();
                case 4:
                    return new CompletionFragment();
                default:
                    return new WelcomeFragment();
            }
        }
        
        public Fragment getCurrentFragment() {
            // ViewPager2 uses this tag format for fragments
            String fragmentTag = "f" + ((OnboardingActivity) fragmentActivity).viewPager.getCurrentItem();
            return fragmentActivity.getSupportFragmentManager().findFragmentByTag(fragmentTag);
        }
        
        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }
}