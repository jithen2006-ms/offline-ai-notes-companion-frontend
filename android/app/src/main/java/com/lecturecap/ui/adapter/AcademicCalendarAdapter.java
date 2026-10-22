package com.lecturecap.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturecap.R;
import com.lecturecap.model.AcademicCalendarItem;

import java.util.ArrayList;
import java.util.List;

public class AcademicCalendarAdapter extends RecyclerView.Adapter<AcademicCalendarAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(AcademicCalendarItem item);
    }

    private List<AcademicCalendarItem> items = new ArrayList<>();
    private final OnDeleteClickListener deleteListener;

    public AcademicCalendarAdapter(OnDeleteClickListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setItems(List<AcademicCalendarItem> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_academic_calendar, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AcademicCalendarItem item = items.get(position);
        holder.tvAcadDescription.setText(item.getDescription());
        holder.tvAcadDate.setText("Date: " + item.getDate());

        if (item.isHoliday()) {
            holder.tvAcadTypeBadge.setText("HOLIDAY");
            holder.tvAcadTypeBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_recording));
            holder.tvAcadTypeBadge.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_recording_bg));
            holder.tvAcadEffectiveDay.setVisibility(View.GONE);
        } else if (item.isSpecialWorkingDay()) {
            holder.tvAcadTypeBadge.setText("SPECIAL WORKING DAY");
            holder.tvAcadTypeBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
            holder.tvAcadTypeBadge.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_active_bg));
            holder.tvAcadEffectiveDay.setVisibility(View.VISIBLE);
            holder.tvAcadEffectiveDay.setText("Follows " + item.getEffectiveDay() + " Timetable");
        } else {
            holder.tvAcadTypeBadge.setText("WORKING DAY");
            holder.tvAcadTypeBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_active));
            holder.tvAcadTypeBadge.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_active_bg));
            holder.tvAcadEffectiveDay.setVisibility(View.GONE);
        }

        holder.btnDeleteAcad.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAcadDescription;
        TextView tvAcadTypeBadge;
        TextView tvAcadDate;
        TextView tvAcadEffectiveDay;
        ImageButton btnDeleteAcad;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAcadDescription = itemView.findViewById(R.id.tvAcadDescription);
            tvAcadTypeBadge = itemView.findViewById(R.id.tvAcadTypeBadge);
            tvAcadDate = itemView.findViewById(R.id.tvAcadDate);
            tvAcadEffectiveDay = itemView.findViewById(R.id.tvAcadEffectiveDay);
            btnDeleteAcad = itemView.findViewById(R.id.btnDeleteAcad);
        }
    }
}
