package com.example.eco_front;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ForestAdapter extends RecyclerView.Adapter<ForestAdapter.ViewHolder> {

    private final List<ForestGuideActivity.Forest> forests;
    private final OnForestClickListener listener;

    public interface OnForestClickListener {
        void onItemClick(ForestGuideActivity.Forest forest);
    }

    public ForestAdapter(List<ForestGuideActivity.Forest> forests, OnForestClickListener listener) {
        this.forests = forests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.forest_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ForestGuideActivity.Forest forest = forests.get(position);

        holder.tvForestName.setText(forest.name);
        holder.tvForestLocation.setText(forest.location);
        holder.tvRisk.setText(forest.risk + " Risk");

        int imageResId = holder.itemView.getContext().getResources()
                .getIdentifier(forest.imageName, "drawable", holder.itemView.getContext().getPackageName());
        if (imageResId != 0) {
            holder.ivForestImage.setImageResource(imageResId);
        } else {
            holder.ivForestImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        if (forest.risk.equals("High")) {
            holder.itemContainer.setBackgroundResource(R.drawable.pastel_card_orange_elevated);
            holder.riskBadge.setBackgroundResource(R.drawable.badge_high_risk);
        } else {
            holder.itemContainer.setBackgroundResource(R.drawable.pastel_card_teal_elevated);
            holder.riskBadge.setBackgroundResource(R.drawable.badge_medium_risk);
        }

        holder.itemContainer.setOnClickListener(v -> listener.onItemClick(forest));
        UiUtils.setupPremiumBouncyCard(holder.itemContainer);
    }

    @Override
    public int getItemCount() {
        return forests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout itemContainer;
        ImageView ivForestImage;
        TextView tvForestName;
        TextView tvForestLocation;
        LinearLayout riskBadge;
        TextView tvRisk;

        ViewHolder(View itemView) {
            super(itemView);
            itemContainer = itemView.findViewById(R.id.item_container);
            ivForestImage = itemView.findViewById(R.id.iv_forest_image);
            tvForestName = itemView.findViewById(R.id.tv_forest_name);
            tvForestLocation = itemView.findViewById(R.id.tv_forest_location);
            riskBadge = itemView.findViewById(R.id.risk_badge);
            tvRisk = itemView.findViewById(R.id.tv_risk);
        }
    }
}