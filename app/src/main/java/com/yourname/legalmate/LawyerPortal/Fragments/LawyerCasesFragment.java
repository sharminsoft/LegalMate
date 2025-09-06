package com.yourname.legalmate.LawyerPortal.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.yourname.legalmate.LawyerPortal.Activities.AddCaseInfoActivity;
import com.yourname.legalmate.LawyerPortal.Activities.CaseStatusActivity;
import com.yourname.legalmate.LawyerPortal.Adapters.CasesAdapter;
import com.yourname.legalmate.LawyerPortal.Models.Case;
import com.yourname.legalmate.R;

import java.util.ArrayList;
import java.util.List;

public class LawyerCasesFragment extends Fragment {

    private static final String TAG = "LawyerCasesFragment";

    private FloatingActionButton fabAddCase;
    private RecyclerView recyclerViewCases;
    private EditText etSearchCases;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private CasesAdapter casesAdapter;
    private List<Case> caseList;
    private List<Case> filteredCaseList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lawyer_cases, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Check if user is authenticated
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Initialize views
        initViews(view);

        // Setup RecyclerView
        setupRecyclerView();

        // Setup search functionality
        setupSearchFunctionality();

        // Load cases from Firestore
        loadCasesFromFirestore();

        return view;
    }

    private void initViews(View view) {
        fabAddCase = view.findViewById(R.id.fabAddCase);
        recyclerViewCases = view.findViewById(R.id.recyclerViewCases);
        etSearchCases = view.findViewById(R.id.etSearchCases);

        fabAddCase.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddCaseInfoActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        caseList = new ArrayList<>();
        filteredCaseList = new ArrayList<>();

        // Check if context is available
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot setup RecyclerView");
            return;
        }

        casesAdapter = new CasesAdapter(getContext(), filteredCaseList);

        recyclerViewCases.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCases.setAdapter(casesAdapter);

        // Set click listener for case items
        casesAdapter.setOnCaseClickListener(caseItem -> {
            // Handle case item click - navigate to case details
            if (getContext() != null) {

                Intent intent = new Intent(getContext(), CaseStatusActivity.class);
                intent.putExtra("caseId", caseItem.getCaseId());
                startActivity(intent);
            }
            // You can add navigation to case details activity here
        });
    }

    private void setupSearchFunctionality() {
        etSearchCases.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Empty implementation
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCases(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Empty implementation
            }
        });
    }

    private void filterCases(String searchText) {
        filteredCaseList.clear();

        if (searchText.isEmpty()) {
            filteredCaseList.addAll(caseList);
        } else {
            String searchLower = searchText.toLowerCase().trim();
            for (Case caseItem : caseList) {
                // Null safety checks
                String clientName = caseItem.getClientName() != null ?
                        caseItem.getClientName().toLowerCase() : "";
                String caseStatus = caseItem.getCaseStatus() != null ?
                        caseItem.getCaseStatus().toLowerCase() : "";
                String caseTitle = caseItem.getCaseTitle() != null ?
                        caseItem.getCaseTitle().toLowerCase() : "";

                if (clientName.contains(searchLower) ||
                        caseStatus.contains(searchLower) ||
                        caseTitle.contains(searchLower)) {
                    filteredCaseList.add(caseItem);
                }
            }
        }

        if (casesAdapter != null) {
            casesAdapter.notifyDataSetChanged();
        }
    }

    private void loadCasesFromFirestore() {
        if (currentUserId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Log.d(TAG, "Loading cases for user: " + currentUserId);

        db.collection("All Cases")
                .document(currentUserId)
                .collection("Cases")
                .get()
                .addOnCompleteListener(task -> {
                    // Check if fragment is still attached
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "Fragment not attached, skipping case loading");
                        return;
                    }

                    if (task.isSuccessful() && task.getResult() != null) {
                        caseList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Case caseItem = new Case();
                                caseItem.setCaseId(document.getId());

                                // Get data from Firestore document with null safety
                                String clientName = document.getString("clientName");
                                caseItem.setClientName(clientName != null ? clientName : "Unknown Client");

                                String caseStatus = document.getString("caseStatus");
                                caseItem.setCaseStatus(caseStatus != null ? caseStatus : "Unknown Status");

                                caseItem.setCaseTitle(document.getString("caseTitle"));
                                caseItem.setCaseDescription(document.getString("caseDescription"));
                                caseItem.setDateCreated(document.getString("dateCreated"));

                                caseList.add(caseItem);

                                Log.d(TAG, "Loaded case: " + caseItem.getClientName() +
                                        " - " + caseItem.getCaseStatus());

                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing case document: " + document.getId(), e);
                            }
                        }

                        // Update filtered list and notify adapter
                        filteredCaseList.clear();
                        filteredCaseList.addAll(caseList);

                        if (casesAdapter != null) {
                            casesAdapter.notifyDataSetChanged();
                        }

                        Log.d(TAG, "Total cases loaded: " + caseList.size());

                        if (caseList.isEmpty()) {
                            Toast.makeText(getContext(), "No cases found", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Log.e(TAG, "Error getting cases: ", task.getException());
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(getContext(), "Failed to load cases: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload cases when fragment becomes visible (e.g., after adding a new case)
        if (currentUserId != null) {
            loadCasesFromFirestore();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up references to avoid memory leaks
        casesAdapter = null;
        if (caseList != null) {
            caseList.clear();
        }
        if (filteredCaseList != null) {
            filteredCaseList.clear();
        }
    }
}