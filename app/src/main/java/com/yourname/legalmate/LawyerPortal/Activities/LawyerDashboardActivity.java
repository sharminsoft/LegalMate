package com.yourname.legalmate.LawyerPortal.Activities;

import android.os.Build;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.yourname.legalmate.LawyerPortal.Fragments.LawyerCasesFragment;
import com.yourname.legalmate.LawyerPortal.Fragments.LawyerHomeFragment;
import com.yourname.legalmate.LawyerPortal.Fragments.LawyerChatListFragment;
import com.yourname.legalmate.LawyerPortal.Fragments.LawyerProfileFragment;
import com.yourname.legalmate.R;

public class LawyerDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fix: Correct API level check (UPSIDE_DOWN_CAKE is API 34)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            EdgeToEdge.enable(this);
        }

        setContentView(R.layout.activity_lawyer_dashboard);

        // Apply window insets for edge-to-edge if supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }



        initViews();
        setupBottomNavigation();

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            loadFragment(new LawyerHomeFragment());
        }
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new LawyerHomeFragment();
            } else if (itemId == R.id.nav_cases) {
                selectedFragment = new LawyerCasesFragment();
            } else if (itemId == R.id.nav_messages) {
                selectedFragment = new LawyerChatListFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new LawyerProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}