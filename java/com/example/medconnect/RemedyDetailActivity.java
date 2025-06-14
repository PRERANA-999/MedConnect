package com.example.medconnect;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class RemedyDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SYMPTOM_NAME = "symptom_name"; // Key for Intent extra

    private TextView textViewSymptomTitle;
    private TextView textViewRemediesContent;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remedy_detail);

        toolbar = findViewById(R.id.toolbarRemedy);
        setSupportActionBar(toolbar);

        // Enable the Up button (back arrow)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        textViewSymptomTitle = findViewById(R.id.textViewSymptomTitle);
        textViewRemediesContent = findViewById(R.id.textViewRemediesContent);

        // Get the symptom name from the Intent
        String symptomName = getIntent().getStringExtra(EXTRA_SYMPTOM_NAME);

        if (symptomName != null && !symptomName.isEmpty()) {
            toolbar.setTitle("Remedies"); // Set generic toolbar title
            textViewSymptomTitle.setText("Remedies for " + symptomName);
            displayRemedies(symptomName);
        } else {
            toolbar.setTitle("Remedies");
            textViewSymptomTitle.setText("Remedies");
            textViewRemediesContent.setText("No symptom provided. Please go back and select a symptom.");
        }
    }

    // Handle the Up button (back arrow) click
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // --- Method to display remedies based on symptom ---
    private void displayRemedies(String symptom) {
        String remedies = "";
        switch (symptom.toLowerCase()) {
            case "cough":
                remedies = "1. Drink warm fluids (tea with honey).\n" +
                        "2. Use a humidifier to moisten the air.\n" +
                        "3. Gargle with salt water.\n" +
                        "4. Avoid irritants like smoke and dust.\n" +
                        "5. Get plenty of rest.";
                break;
            case "cold":
                remedies = "1. Stay hydrated with water, juice, and broth.\n" +
                        "2. Get adequate rest.\n" +
                        "3. Use saline nasal sprays or drops.\n" +
                        "4. Gargle with warm salt water for sore throat.\n" +
                        "5. Consider over-the-counter cold medicines (follow directions).";
                break;
            case "fever":
                remedies = "1. Rest and avoid strenuous activity.\n" +
                        "2. Drink plenty of fluids to prevent dehydration.\n" +
                        "3. Take acetaminophen (Tylenol) or ibuprofen (Advil) as directed.\n" +
                        "4. Sponge bathe with lukewarm water.\n" +
                        "5. Wear light clothing and keep the room cool.";
                break;
            case "headache":
                remedies = "1. Rest in a quiet, dark room.\n" +
                        "2. Apply a cold compress to your forehead.\n" +
                        "3. Drink water to stay hydrated.\n" +
                        "4. Take over-the-counter pain relievers (e.g., ibuprofen, aspirin).\n" +
                        "5. Try caffeine (in moderation) if you're used to it.";
                break;
            case "body pain":
                remedies = "1. Apply hot or cold compresses to the affected area.\n" +
                        "2. Take over-the-counter pain relievers like ibuprofen.\n" +
                        "3. Gently stretch or massage the painful area.\n" +
                        "4. Ensure proper posture.\n" +
                        "5. Get enough rest and try to reduce stress.";
                break;
            default:
                remedies = "No specific remedies found for this symptom. Please consult a doctor for personalized advice.";
                break;
        }
        textViewRemediesContent.setText(remedies);
    }
}