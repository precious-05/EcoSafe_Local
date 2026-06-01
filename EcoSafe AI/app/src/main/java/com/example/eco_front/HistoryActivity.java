package com.example.eco_front;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";
    private static final String BASE_URL = "http://192.168.106.56:8000/";

    private RecyclerView rvIncidents;
    private IncidentAdapter incidentAdapter;
    private List<Incident> incidentList;
    private List<Incident> filteredList;

    private TextView tvFilter7Days, tvFilter30Days, tvFilterAll;
    private EditText etSearch;
    private ImageView ivClearSearch, ivFilter, ivBackBtn;
    private LinearLayout emptyState;
    private FloatingActionButton fabNewDetection;

    private SQLiteDatabase database;
    private String currentFilter = "all";
    private String currentSearchQuery = "";
    /** Bumped on clear-all so an in-flight backend sync cannot re-insert old rows. */
    private volatile long historyClearGeneration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initViews();
        setupDatabase();
        setupListeners();
        loadIncidents();
        syncWithBackend();
        setupSwipeToDelete();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_all) {
            showClearAllDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All History")
                .setMessage("This permanently removes all incidents from this phone. "
                        + "Old entries will not come back from the server.\n\n"
                        + "Also clear incidents on the backend server (for testing)?")
                .setPositiveButton("Clear on phone", (dialog, which) -> clearAllIncidents(false))
                .setNeutralButton("Phone + server", (dialog, which) -> clearAllIncidents(true))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllIncidents(boolean alsoClearServer) {
        historyClearGeneration++;
        HistoryPrefs.markHistoryCleared(this);

        try {
            if (database != null && database.isOpen()) {
                database.execSQL("DELETE FROM incidents");
            }
            incidentList.clear();
            filteredList.clear();
            incidentAdapter.notifyDataSetChanged();
            emptyState.setVisibility(View.VISIBLE);
            rvIncidents.setVisibility(View.GONE);

            Toast.makeText(this, "History cleared on this device", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "All local incidents cleared (generation=" + historyClearGeneration + ")");

            if (alsoClearServer) {
                clearServerIncidents();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing incidents", e);
            Toast.makeText(this, "Error clearing incidents", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearServerIncidents() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url(BASE_URL + "incidents?admin_key=EcoSafe_Admin_2024")
                        .delete()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(HistoryActivity.this,
                                    "Phone and server history cleared",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(HistoryActivity.this,
                                    "Phone cleared; server clear failed (" + response.code() + ")",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error clearing server incidents", e);
                runOnUiThread(() -> Toast.makeText(HistoryActivity.this,
                        "Phone cleared; server unreachable",
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void initViews() {
        rvIncidents = findViewById(R.id.rv_incidents);
        tvFilter7Days = findViewById(R.id.filter_7days);
        tvFilter30Days = findViewById(R.id.filter_30days);
        tvFilterAll = findViewById(R.id.filter_all);
        etSearch = findViewById(R.id.et_search);
        ivClearSearch = findViewById(R.id.iv_clear_search);
        ivFilter = findViewById(R.id.iv_filter);
        ivBackBtn = findViewById(R.id.iv_back_btn);
        emptyState = findViewById(R.id.empty_state);
        fabNewDetection = findViewById(R.id.fab_new_detection);

        rvIncidents.setLayoutManager(new LinearLayoutManager(this));
        incidentList = new ArrayList<>();
        filteredList = new ArrayList<>();
        incidentAdapter = new IncidentAdapter(filteredList, this::onIncidentClick);
        rvIncidents.setAdapter(incidentAdapter);
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position >= 0 && position < filteredList.size()) {
                    Incident incidentToDelete = filteredList.get(position);

                    new AlertDialog.Builder(HistoryActivity.this)
                            .setTitle("Delete Incident")
                            .setMessage("Delete this incident from history?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                // Delete from database
                                database.delete("incidents", "id = ?", new String[]{String.valueOf(incidentToDelete.id)});
                                // Remove from lists
                                incidentList.remove(incidentToDelete);
                                filteredList.remove(position);
                                incidentAdapter.notifyItemRemoved(position);
                                Toast.makeText(HistoryActivity.this, "Incident deleted", Toast.LENGTH_SHORT).show();

                                // Show empty state if no items
                                if (filteredList.isEmpty()) {
                                    emptyState.setVisibility(View.VISIBLE);
                                    rvIncidents.setVisibility(View.GONE);
                                }
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                incidentAdapter.notifyItemChanged(position);
                            })
                            .show();
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(rvIncidents);
    }

    private void setupDatabase() {
        try {
            File dbFile = new File(getFilesDir(), "incidents.db");
            database = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

            // Create table with all required columns
            database.execSQL("CREATE TABLE IF NOT EXISTS incidents (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "incident_id INTEGER," +
                    "image_path TEXT," +
                    "confidence REAL," +
                    "latitude REAL," +
                    "longitude REAL," +
                    "timestamp TEXT," +
                    "status TEXT," +
                    "is_fire INTEGER," +
                    "alert_sent INTEGER DEFAULT 0," +
                    "synced INTEGER DEFAULT 0)");

            Log.d(TAG, "Database setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up database", e);
        }
    }

    private void setupListeners() {
        ivBackBtn.setOnClickListener(v -> finish());

        ivFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Filter options", Toast.LENGTH_SHORT).show();
        });

        fabNewDetection.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        // ✅ NEW: Clear All Button Listener
        LinearLayout btnClearAll = findViewById(R.id.btn_clear_all);
        btnClearAll.setOnClickListener(v -> showClearAllDialog());

        // Apply premium bouncy interaction effects
        UiUtils.setupPremiumBouncyCard(ivBackBtn, ivFilter, fabNewDetection, btnClearAll,
                tvFilter7Days, tvFilter30Days, tvFilterAll);

        tvFilter7Days.setOnClickListener(v -> {
            setActiveFilter(tvFilter7Days);
            currentFilter = "7days";
            applyFilters();
        });

        tvFilter30Days.setOnClickListener(v -> {
            setActiveFilter(tvFilter30Days);
            currentFilter = "30days";
            applyFilters();
        });

        tvFilterAll.setOnClickListener(v -> {
            setActiveFilter(tvFilterAll);
            currentFilter = "all";
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
        tvFilter7Days.setBackgroundResource(R.drawable.chip_filter_inactive);
        tvFilter30Days.setBackgroundResource(R.drawable.chip_filter_inactive);
        tvFilterAll.setBackgroundResource(R.drawable.chip_filter_inactive);

        tvFilter7Days.setTextColor(getColor(R.color.pastel_text_secondary));
        tvFilter30Days.setTextColor(getColor(R.color.pastel_text_secondary));
        tvFilterAll.setTextColor(getColor(R.color.pastel_text_secondary));

        activeView.setBackgroundResource(R.drawable.chip_filter_active);
        activeView.setTextColor(getColor(R.color.white));
    }

    private void loadIncidents() {
        incidentList.clear();

        Cursor cursor = database.rawQuery("SELECT * FROM incidents ORDER BY timestamp DESC", null);

        while (cursor.moveToNext()) {
            Incident incident = new Incident();
            incident.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            incident.incidentId = cursor.getInt(cursor.getColumnIndexOrThrow("incident_id"));
            incident.imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
            incident.confidence = cursor.getDouble(cursor.getColumnIndexOrThrow("confidence"));
            incident.latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
            incident.longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
            incident.timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
            incident.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
            incident.isFire = cursor.getInt(cursor.getColumnIndexOrThrow("is_fire")) == 1;
            incident.alertSent = cursor.getInt(cursor.getColumnIndexOrThrow("alert_sent")) == 1;
            incident.synced = cursor.getInt(cursor.getColumnIndexOrThrow("synced")) == 1;

            incidentList.add(incident);
            Log.d(TAG, "Loaded incident: id=" + incident.id + ", is_fire=" + incident.isFire +
                    ", lat=" + incident.latitude + ", lon=" + incident.longitude);
        }
        cursor.close();

        applyFilters();
    }

    private void applyFilters() {
        filteredList.clear();

        long startDate = 0;
        if (currentFilter.equals("7days")) {
            startDate = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        } else if (currentFilter.equals("30days")) {
            startDate = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        }

        for (Incident incident : incidentList) {
            boolean matchesFilter = true;
            boolean matchesSearch = true;

            if (startDate > 0) {
                try {
                    Date incidentDate = parseTimestamp(incident.timestamp);
                    if (incidentDate == null || incidentDate.getTime() < startDate) {
                        matchesFilter = false;
                    }
                } catch (Exception e) {
                    matchesFilter = false;
                }
            }

            if (matchesFilter && !currentSearchQuery.isEmpty()) {
                String locationStr = String.format(Locale.getDefault(),
                        "%.4f, %.4f", incident.latitude, incident.longitude);
                if (!locationStr.contains(currentSearchQuery)) {
                    matchesSearch = false;
                }
            }

            if (matchesFilter && matchesSearch) {
                filteredList.add(incident);
            }
        }

        incidentAdapter.notifyDataSetChanged();

        if (filteredList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvIncidents.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvIncidents.setVisibility(View.VISIBLE);
        }
    }

    private void syncWithBackend() {
        final long syncGeneration = historyClearGeneration;

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url(BASE_URL + "incidents")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (syncGeneration != historyClearGeneration) {
                        Log.d(TAG, "Sync aborted — history was cleared during fetch");
                        return;
                    }

                    if (response.isSuccessful() && response.body() != null) {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);

                        if (jsonObject.has("incidents")) {
                            JSONArray incidentsArray = jsonObject.getJSONArray("incidents");
                            int imported = 0;
                            int skipped = 0;

                            for (int i = 0; i < incidentsArray.length(); i++) {
                                if (syncGeneration != historyClearGeneration) {
                                    Log.d(TAG, "Sync aborted mid-import — history cleared");
                                    return;
                                }

                                JSONObject incidentJson = incidentsArray.getJSONObject(i);

                                int incidentId = incidentJson.getInt("id");
                                double confidence = incidentJson.getDouble("confidence");
                                double latitude = incidentJson.getDouble("latitude");
                                double longitude = incidentJson.getDouble("longitude");
                                String timestamp = incidentJson.getString("timestamp");
                                String status = incidentJson.getString("status");
                                String imagePath = incidentJson.optString("image_path", "");
                                boolean isFireIncident = !"safe".equalsIgnoreCase(status);

                                Date incidentDate = parseTimestamp(timestamp);
                                long incidentTimeMs = incidentDate != null ? incidentDate.getTime() : 0L;
                                if (HistoryPrefs.shouldSkipServerIncident(HistoryActivity.this, incidentTimeMs)) {
                                    skipped++;
                                    continue;
                                }

                                Cursor cursor = database.rawQuery(
                                        "SELECT id FROM incidents WHERE incident_id = ?",
                                        new String[]{String.valueOf(incidentId)});

                                boolean alreadyLocal = cursor.moveToFirst();
                                cursor.close();

                                if (!alreadyLocal) {
                                    ContentValues values = new ContentValues();
                                    values.put("incident_id", incidentId);
                                    values.put("image_path", imagePath);
                                    values.put("confidence", confidence);
                                    values.put("latitude", latitude);
                                    values.put("longitude", longitude);
                                    values.put("timestamp", timestamp);
                                    values.put("status", status);
                                    values.put("is_fire", isFireIncident ? 1 : 0);
                                    values.put("synced", 1);
                                    database.insert("incidents", null, values);
                                    imported++;
                                }
                            }

                            Log.d(TAG, "Backend sync: imported=" + imported + ", skipped(old)=" + skipped);

                            if (syncGeneration == historyClearGeneration) {
                                runOnUiThread(() -> loadIncidents());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing with backend", e);
            }
        }).start();
    }

    private Date parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return null;
        }

        String[] formats = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"
        };

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                sdf.setLenient(false);
                return sdf.parse(timestamp);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private void onIncidentClick(Incident incident) {
        Intent intent = new Intent(this, IncidentDetailActivity.class);
        intent.putExtra("incident_id", incident.id);
        intent.putExtra("incident_id_server", incident.incidentId);
        intent.putExtra("image_path", incident.imagePath);
        intent.putExtra("confidence", incident.confidence);
        intent.putExtra("latitude", incident.latitude);
        intent.putExtra("longitude", incident.longitude);
        intent.putExtra("timestamp", incident.timestamp);
        intent.putExtra("status", incident.status);
        intent.putExtra("is_fire", incident.isFire);
        intent.putExtra("alert_sent", incident.alertSent);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null && database.isOpen()) {
            database.close();
        }
    }

    public static class Incident {
        public int id;
        public int incidentId;
        public String imagePath;
        public double confidence;
        public double latitude;
        double longitude;
        public String timestamp;
        public String status;
        public boolean isFire;
        public boolean alertSent;
        public boolean synced;
    }
}