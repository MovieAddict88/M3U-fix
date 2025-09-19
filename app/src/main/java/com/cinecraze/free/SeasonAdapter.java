package com.cinecraze.free;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cinecraze.free.models.Season;
import com.cinecraze.free.R;

import java.util.List;

public class SeasonAdapter extends RecyclerView.Adapter<SeasonAdapter.ViewHolder> {

    private Context context;
    private List<Season> seasons;
    private OnSeasonClickListener listener;
    private int selectedSeasonPosition = 0;

    public interface OnSeasonClickListener {
        void onSeasonClick(Season season, int position);
    }

    public SeasonAdapter(Context context, List<Season> seasons, OnSeasonClickListener listener) {
        this.context = context;
        this.seasons = seasons;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_season, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Season season = seasons.get(position);
        
        holder.seasonNumber.setText("Season " + season.getSeason());
        
        if (season.getSeasonPoster() != null && !season.getSeasonPoster().isEmpty()) {
            Glide.with(context).load(season.getSeasonPoster()).into(holder.seasonPoster);
        }
        
        // Highlight selected season
        holder.itemView.setSelected(position == selectedSeasonPosition);
        
        holder.itemView.setOnClickListener(v -> {
            selectedSeasonPosition = position;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onSeasonClick(season, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return seasons != null ? seasons.size() : 0;
    }

    public void setSelectedSeason(int position) {
        selectedSeasonPosition = position;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView seasonPoster;
        TextView seasonNumber;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            seasonPoster = itemView.findViewById(R.id.season_poster);
            seasonNumber = itemView.findViewById(R.id.season_number);
        }
    }
}