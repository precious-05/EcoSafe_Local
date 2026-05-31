package com.example.eco_front;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

public class PrivacyPolicyActivity extends AppCompatActivity {

    public static final String EXTRA_REQUIRE_ACCEPTANCE = "require_acceptance";

    private WebView webView;
    private CheckBox cbAcceptPolicy;
    private MaterialButton btnAccept;
    private LinearLayout acceptancePanel;
    private Toolbar toolbar;
    private boolean requireAcceptance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        requireAcceptance = getIntent().getBooleanExtra(EXTRA_REQUIRE_ACCEPTANCE, true);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(!requireAcceptance);
        }

        webView = findViewById(R.id.web_privacy_policy);
        cbAcceptPolicy = findViewById(R.id.cb_accept_policy);
        btnAccept = findViewById(R.id.btn_accept);
        acceptancePanel = findViewById(R.id.acceptance_panel);
        MaterialButton btnDecline = findViewById(R.id.btn_decline);

        setupWebView();
        setupAcceptanceFlow(btnDecline);
        setupBackNavigation();
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/privacy_policy.html");
    }

    private void setupAcceptanceFlow(MaterialButton btnDecline) {
        if (!requireAcceptance) {
            acceptancePanel.setVisibility(android.view.View.GONE);
            return;
        }

        cbAcceptPolicy.setOnCheckedChangeListener((buttonView, isChecked) ->
                btnAccept.setEnabled(isChecked));

        btnAccept.setOnClickListener(v -> {
            PrivacyPrefs.setAccepted(this, true);
            Toast.makeText(this, "Privacy Policy accepted", Toast.LENGTH_SHORT).show();
            openHomeAndFinish();
        });

        btnDecline.setOnClickListener(v -> showDeclineDialog());
    }

    private void setupBackNavigation() {
        toolbar.setNavigationOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (requireAcceptance) {
                    showDeclineDialog();
                } else {
                    finish();
                }
            }
        });
    }

    private void showDeclineDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Acceptance Required")
                .setMessage("You must accept the Privacy Policy to use EcoSafe-AI.")
                .setPositiveButton("Review Policy", null)
                .setNegativeButton("Exit App", (dialog, which) -> finishAffinity())
                .show();
    }

    private void openHomeAndFinish() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
