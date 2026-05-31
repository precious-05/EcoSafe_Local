package com.example.eco_front;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.ViewHolder> {

    private final List<HistoryActivity.Incident> incidents;
    private final OnIncidentClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
    private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public interface OnIncidentClickListener {
        void onItemClick(HistoryActivity.Incident incident);
    }

    public IncidentAdapter(List<HistoryActivity.Incident> incidents, OnIncidentClickListener listener) {
        this.incidents = incidents;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.incident_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryActivity.Incident incident = incidents.get(position);

        // Set date and time
        try {
            long time = inputFormat.parse(incident.timestamp).getTime();
            holder.tvDate.setText(dateFormat.format(time));
        } catch (Exception e) {
            holder.tvDate.setText(incident.timestamp);
        }

        // Set location
        if (incident.latitude != 0.0 || incident.longitude != 0.0) {
            holder.tvLocation.setText(String.format(Locale.getDefault(),
                    "Lat: %.4f, Lon: %.4f", incident.latitude, incident.longitude));
        } else {
            holder.tvLocation.setText("Location not captured");
        }

        // Set confidence
        holder.tvConfidence.setText(String.format(Locale.getDefault(),
                "Confidence: %.1f%%", incident.confidence));

        // Load thumbnail image
        loadThumbnail(holder.ivThumbnail, incident.imagePath);

        // Dynamic card background color based on status and premium spring touch animation
        if (incident.isFire) {
            holder.itemContainer.setBackgroundResource(R.drawable.pastel_card_orange_elevated);
            holder.statusBadge.setBackgroundResource(R.drawable.status_badge_fire);
            holder.tvStatus.setText("Fire Detected");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.pastel_orange));
        } else {
            holder.itemContainer.setBackgroundResource(R.drawable.pastel_card_green_elevated);
            holder.statusBadge.setBackgroundResource(R.drawable.status_badge_safe);
            holder.tvStatus.setText("No Fire");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.pastel_green));
        }

        // Set click listener
        holder.itemContainer.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(incident);
            }
        });

        UiUtils.setupPremiumBouncyCard(holder.itemContainer);
    }

    private void loadThumbnail(ImageView imageView, String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    return;
                }
            }
        }
        // Default placeholder
        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
    }

    @Override
    public int getItemCount() {
        return incidents.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout itemContainer;
        ImageView ivThumbnail;
        TextView tvDate;
        TextView tvLocation;
        TextView tvConfidence;
        LinearLayout statusBadge;
        TextView tvStatus;
        ImageView ivArrow;

        ViewHolder(View itemView) {
            super(itemView);
            itemContainer = itemView.findViewById(R.id.item_container);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvConfidence = itemView.findViewById(R.id.tv_confidence);
            statusBadge = itemView.findViewById(R.id.status_badge);
            tvStatus = itemView.findViewById(R.id.tv_status);
            ivArrow = itemView.findViewById(R.id.iv_arrow);
        }
    }
}