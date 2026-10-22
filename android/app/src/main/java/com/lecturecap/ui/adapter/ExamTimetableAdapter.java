package com.lecturecap.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturecap.R;
import com.lecturecap.model.ExamScheduleItem;

import java.util.ArrayList;
import java.util.List;

public class ExamTimetableAdapter extends RecyclerView.Adapter<ExamTimetableAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(ExamScheduleItem item);
    }

    private List<ExamScheduleItem> items = new ArrayList<>();
    private final OnDeleteClickListener deleteListener;

    public ExamTimetableAdapter(OnDeleteClickListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setItems(List<ExamScheduleItem> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exam_timetable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExamScheduleItem item = items.get(position);
        holder.tvExamName.setText(item.getExamName());
        holder.tvExamDate.setText(item.getDate());
        holder.tvExamTimeRange.setText(item.getTimeRange());
        holder.tvExamRoom.setText("Venue: " + item.getRoom());

        holder.btnDeleteExam.setOnClickListener(v -> {
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
        TextView tvExamName;
        TextView tvExamDate;
        TextView tvExamTimeRange;
        TextView tvExamRoom;
        ImageButton btnDeleteExam;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExamName = itemView.findViewById(R.id.tvExamName);
            tvExamDate = itemView.findViewById(R.id.tvExamDate);
            tvExamTimeRange = itemView.findViewById(R.id.tvExamTimeRange);
            tvExamRoom = itemView.findViewById(R.id.tvExamRoom);
            btnDeleteExam = itemView.findViewById(R.id.btnDeleteExam);
        }
    }
}
