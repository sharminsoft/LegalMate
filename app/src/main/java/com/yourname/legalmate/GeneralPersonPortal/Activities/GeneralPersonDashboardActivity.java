package com.yourname.legalmate.GeneralPersonPortal.Activities;

import android.content.Intent;
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
import com.yourname.legalmate.GeneralPersonPortal.Fragments.GeneralHomeFragment;
import com.yourname.legalmate.GeneralPersonPortal.Fragments.GeneralChatListFragment;
import com.yourname.legalmate.GeneralPersonPortal.Fragments.GeneralProfileFragment;
import com.yourname.legalmate.R;

public class GeneralPersonDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fix: Correct API level check (UPSIDE_DOWN_CAKE is API 34)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            EdgeToEdge.enable(this);
        }

        setContentView(R.layout.activity_general_person_dashboard);

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
            loadFragment(new GeneralHomeFragment());
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
            if (itemId == R.id.nav_general_home) {
                selectedFragment = new GeneralHomeFragment();
            } else if (itemId == R.id.nav_general_find_lawyer) {

                Intent intent = new Intent(GeneralPersonDashboardActivity.this, FindLawyerActivity.class);
                startActivity(intent);

            } else if (itemId == R.id.nav_general_messages) {
                selectedFragment = new GeneralChatListFragment();
            } else if (itemId == R.id.nav_general_profile) {
                selectedFragment = new GeneralProfileFragment();
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