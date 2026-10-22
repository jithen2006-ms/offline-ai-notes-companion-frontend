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
import com.lecturecap.model.TemporaryScheduleChange;

import java.util.ArrayList;
import java.util.List;

public class TemporaryChangesAdapter extends RecyclerView.Adapter<TemporaryChangesAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(TemporaryScheduleChange item);
    }

    private List<TemporaryScheduleChange> items = new ArrayList<>();
    private final OnDeleteClickListener deleteListener;

    public TemporaryChangesAdapter(OnDeleteClickListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setItems(List<TemporaryScheduleChange> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_temporary_change, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TemporaryScheduleChange item = items.get(position);

        String title = (item.getCourseName() != null && !item.getCourseName().isEmpty())
                ? item.getCourseName() : "All Classes / Day Notice";
        holder.tvTempCourse.setText(title);
        holder.tvTempDate.setText(item.getDate());
        holder.tvTempTimeRange.setText(item.getTimeRange());
        holder.tvTempReason.setText("Reason: " + item.getReason());

        if (TemporaryScheduleChange.TYPE_CLASS_CANCELLED.equalsIgnoreCase(item.getChangeType())
                || TemporaryScheduleChange.TYPE_DATE_HOLIDAY.equalsIgnoreCase(item.getChangeType())) {
            holder.tvTempTypeBadge.setText(item.getChangeType().replace("_", " "));
            holder.tvTempTypeBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_recording));
            holder.tvTempTypeBadge.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_recording_bg));
        } else {
            holder.tvTempTypeBadge.setText(item.getChangeType().replace("_", " "));
            holder.tvTempTypeBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_active));
            holder.tvTempTypeBadge.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_active_bg));
        }

        holder.btnDeleteTemp.setOnClickListener(v -> {
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
        TextView tvTempCourse;
        TextView tvTempTypeBadge;
        TextView tvTempDate;
        TextView tvTempTimeRange;
        TextView tvTempReason;
        ImageButton btnDeleteTemp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTempCourse = itemView.findViewById(R.id.tvTempCourse);
            tvTempTypeBadge = itemView.findViewById(R.id.tvTempTypeBadge);
            tvTempDate = itemView.findViewById(R.id.tvTempDate);
            tvTempTimeRange = itemView.findViewById(R.id.tvTempTimeRange);
            tvTempReason = itemView.findViewById(R.id.tvTempReason);
            btnDeleteTemp = itemView.findViewById(R.id.btnDeleteTemp);
        }
    }
}
