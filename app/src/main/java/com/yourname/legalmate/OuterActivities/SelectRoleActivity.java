package com.yourname.legalmate.OuterActivities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.yourname.legalmate.GeneralPersonPortal.AccountCreation.GenPerCreateAccountActivity;
import com.yourname.legalmate.LawyerPortal.AccountCreation.LawyerCreateAccountActivity;
import com.yourname.legalmate.MainActivity;
import com.yourname.legalmate.R;

public class SelectRoleActivity extends AppCompatActivity {

    private Spinner roleSpinner;
    Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_select_role);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        nextButton = findViewById(R.id.nextButton);


        // Set up Spinner
        roleSpinner = findViewById(R.id.roleSpinner);
        String[] roles = {"Select Role", "I am a Lawyer", "I am a Client (General Person)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        nextButton.setOnClickListener(v -> {
            // Handle the next button click here


            if (roleSpinner.getSelectedItemPosition() == 1) {

                Intent lawyerIntent = new Intent(SelectRoleActivity.this, LawyerCreateAccountActivity.class);
                lawyerIntent.putExtra("role", "Lawyer");
                startActivity(lawyerIntent);

            } else if (roleSpinner.getSelectedItemPosition() == 2) {

                Intent genPerIntent = new Intent(SelectRoleActivity.this, GenPerCreateAccountActivity.class);
                genPerIntent.putExtra("role", "General Person");
                startActivity(genPerIntent);
            }


        });



    }
}