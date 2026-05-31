package com.example.eco_front;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class ForestGuideActivity extends AppCompatActivity {

    private RecyclerView rvForests;
    private EditText etSearch;
    private ImageView ivClearSearch, ivBackBtn;
    private TextView filterAll, filterHighRisk, filterMediumRisk;
    private LinearLayout emptyState;

    private List<Forest> forestList;
    private List<Forest> filteredList;
    private ForestAdapter forestAdapter;

    private String currentFilter = "all";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forest_guide);

        initViews();
        setupForestData();
        setupListeners();
        displayForests();
    }

    private void initViews() {
        rvForests = findViewById(R.id.rv_forests);
        etSearch = findViewById(R.id.et_search);
        ivClearSearch = findViewById(R.id.iv_clear_search);
        ivBackBtn = findViewById(R.id.iv_back_btn);
        filterAll = findViewById(R.id.filter_all);
        filterHighRisk = findViewById(R.id.filter_high_risk);
        filterMediumRisk = findViewById(R.id.filter_medium_risk);
        emptyState = findViewById(R.id.empty_state);

        rvForests.setLayoutManager(new LinearLayoutManager(this));
        forestList = new ArrayList<>();
        filteredList = new ArrayList<>();
    }

    private void setupForestData() {
        forestList.add(new Forest("Margalla Hills", "Islamabad", "High", "forest_margalla",
                "Chir Pine, Broadleaf trees", "Identity of Islamabad, famous for hiking trails",
                "April to June", "Do not throw cigarettes, avoid picnics in summer"));

        forestList.add(new Forest("Murree Forest", "Murree, Punjab", "High", "forest_murree",
                "Chir Pine, Deodar", "Famous tourist hill station of Pakistan",
                "May to June", "Do not light campfires, extinguish cigarettes properly"));

        forestList.add(new Forest("Kotli Sattian", "Rawalpindi, Punjab", "High", "forest_kotli",
                "Chir Pine, scrub forest", "Mountain forest area of Rawalpindi",
                "May to June", "Do not set fire to dry leaves"));

        forestList.add(new Forest("Chakwal Forest", "Chakwal, Punjab", "Medium", "forest_chakwal",
                "Phulai, Kau, Sanatta trees", "Located in the Salt Range mountains",
                "May to June", "Avoid burning crop residue"));

        forestList.add(new Forest("Tilla Jogian", "Jhelum, Punjab", "Medium", "forest_tilla",
                "Subtropical dry forest", "Historical site, part of Salt Range",
                "May to June", "Do not set fire in grazing areas"));

        forestList.add(new Forest("Kala Chak", "Gujrat, Punjab", "Medium", "forest_kalachak",
                "Subtropical dry scrub", "Forest area of Gujrat",
                "May to June", "Educate local farmers about fire safety"));

        forestList.add(new Forest("Swat Forest", "Swat, KPK", "High", "forest_swat",
                "Chir Pine, Deodar, Fir", "Highest fire incidents in Malakand division",
                "April to June", "Be careful of dry pine needles, they fuel the fire"));

        forestList.add(new Forest("Sherani Forest", "Sherani, Balochistan", "High", "forest_sherani",
                "Chilgoza Pine", "World's largest Chilgoza forest",
                "May to June", "Those collecting pine nuts should be careful of fire"));

        forestList.add(new Forest("Ziarat Forest", "Ziarat, Balochistan", "High", "forest_ziarat",
                "Juniper trees", "World's second oldest juniper forest",
                "May to June", "These are very old trees, need to protect them"));

        forestList.add(new Forest("Abbottabad Forest", "Abbottabad, KPK", "High", "forest_abbottabad",
                "Chir Pine, Deodar", "Forest area of Hazara division",
                "April to June", "Do not set fire near maize crops"));
    }

    private void setupListeners() {
        ivBackBtn.setOnClickListener(v -> finish());

        TextView tvPrivacyPolicy = findViewById(R.id.tv_privacy_policy);
        tvPrivacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(ForestGuideActivity.this, PrivacyPolicyActivity.class);
            intent.putExtra(PrivacyPolicyActivity.EXTRA_REQUIRE_ACCEPTANCE, false);
            startActivity(intent);
        });

        // Apply premium bouncy interaction effects
        UiUtils.setupPremiumBouncyCard(ivBackBtn, tvPrivacyPolicy, filterAll, filterHighRisk, filterMediumRisk);

        filterAll.setOnClickListener(v -> {
            setActiveFilter(filterAll);
            currentFilter = "all";
            applyFilters();
        });

        filterHighRisk.setOnClickListener(v -> {
            setActiveFilter(filterHighRisk);
            currentFilter = "high";
            applyFilters();
        });

        filterMediumRisk.setOnClickListener(v -> {
            setActiveFilter(filterMediumRisk);
            currentFilter = "medium";
            applyFilters();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                if (currentSearchQuery.isEmpty()) {
                    ivClearSearch.setVisibility(View.GONE);
                } else {
                    ivClearSearch.setVisibility(View.VISIBLE);
                }
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            ivClearSearch.setVisibility(View.GONE);
        });
    }

    private void setActiveFilter(TextView activeView) {
        filterAll.setBackgroundResource(R.drawable.chip_filter_inactive);
        filterHighRisk.setBackgroundResource(R.drawable.chip_filter_inactive);
        filterMediumRisk.setBackgroundResource(R.drawable.chip_filter_inactive);

        filterAll.setTextColor(getColor(R.color.pastel_text_secondary));
        filterHighRisk.setTextColor(getColor(R.color.pastel_text_secondary));
        filterMediumRisk.setTextColor(getColor(R.color.pastel_text_secondary));

        activeView.setBackgroundResource(R.drawable.chip_filter_active);
        activeView.setTextColor(getColor(R.color.white));
    }

    private void applyFilters() {
        filteredList.clear();

        for (Forest forest : forestList) {
            boolean matchesFilter = true;
            boolean matchesSearch = true;

            if (currentFilter.equals("high") && !forest.risk.equals("High")) {
                matchesFilter = false;
            } else if (currentFilter.equals("medium") && !forest.risk.equals("Medium")) {
                matchesFilter = false;
            }

            if (matchesFilter && !currentSearchQuery.isEmpty()) {
                if (!forest.name.toLowerCase().contains(currentSearchQuery.toLowerCase())) {
                    matchesSearch = false;
                }
            }

            if (matchesFilter && matchesSearch) {
                filteredList.add(forest);
            }
        }

        displayForests();
    }

    private void displayForests() {
        if (filteredList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvForests.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvForests.setVisibility(View.VISIBLE);
            forestAdapter = new ForestAdapter(filteredList, this::showForestDetail);
            rvForests.setAdapter(forestAdapter);
        }
    }

    private void showForestDetail(Forest forest) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_forest_detail, null);
        bottomSheetDialog.setContentView(sheetView);

        ImageView ivDetailImage = sheetView.findViewById(R.id.iv_detail_image);
        TextView tvDetailName = sheetView.findViewById(R.id.tv_detail_name);
        TextView tvDetailRisk = sheetView.findViewById(R.id.tv_detail_risk);
        LinearLayout detailRiskBadge = sheetView.findViewById(R.id.detail_risk_badge);
        TextView tvDetailLocation = sheetView.findViewById(R.id.tv_detail_location);
        TextView tvDetailTrees = sheetView.findViewById(R.id.tv_detail_trees);
        TextView tvDetailSpeciality = sheetView.findViewById(R.id.tv_detail_speciality);
        TextView tvDetailSeason = sheetView.findViewById(R.id.tv_detail_season);
        TextView tvDetailSafety = sheetView.findViewById(R.id.tv_detail_safety);
        LinearLayout btnClose = sheetView.findViewById(R.id.btn_close);

        int imageResId = getResources().getIdentifier(forest.imageName, "drawable", getPackageName());
        if (imageResId != 0) {
            ivDetailImage.setImageResource(imageResId);
        } else {
            ivDetailImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        tvDetailName.setText(forest.name);
        tvDetailRisk.setText(forest.risk + " Risk");
        tvDetailLocation.setText(forest.location);
        tvDetailTrees.setText(forest.treesType);
        tvDetailSpeciality.setText(forest.speciality);
        tvDetailSeason.setText(forest.fireSeason);
        tvDetailSafety.setText(forest.safetyNote);

        if (forest.risk.equals("High")) {
            detailRiskBadge.setBackgroundResource(R.drawable.badge_high_risk);
        } else {
            detailRiskBadge.setBackgroundResource(R.drawable.badge_medium_risk);
        }

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }

    // Forest Model Class
    public static class Forest {
        String name;
        String location;
        String risk;
        String imageName;
        String treesType;
        String speciality;
        String fireSeason;
        String safetyNote;

        public Forest(String name, String location, String risk, String imageName,
                      String treesType, String speciality, String fireSeason, String safetyNote) {
            this.name = name;
            this.location = location;
            this.risk = risk;
            this.imageName = imageName;
            this.treesType = treesType;
            this.speciality = speciality;
            this.fireSeason = fireSeason;
            this.safetyNote = safetyNote;
        }
    }
}