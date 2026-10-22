package com.lecturecap.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturecap.R;
import com.lecturecap.model.LectureSession;

import java.util.ArrayList;
import java.util.List;

public class LectureHistoryAdapter extends RecyclerView.Adapter<LectureHistoryAdapter.ViewHolder> {

    public interface OnLectureClickListener {
        void onLectureClick(LectureSession session);
    }

    private List<LectureSession> sessionList = new ArrayList<>();
    private final OnLectureClickListener listener;

    public LectureHistoryAdapter(OnLectureClickListener listener) {
        this.listener = listener;
    }

    public void setSessions(List<LectureSession> sessions) {
        this.sessionList = (sessions != null) ? sessions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lecture_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LectureSession session = sessionList.get(position);

        holder.tvHistoryCourse.setText(session.getCourseName());
        holder.tvHistoryDate.setText(session.getDate());
        holder.tvHistoryTime.setText(session.getTimeRange());

        // Upload badge
        if (LectureSession.UPLOAD_STATUS_UPLOADED.equals(session.getUploadStatus())) {
            holder.tvUploadStatusBadge.setText("Uploaded ✓");
            holder.tvUploadStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_active));
            holder.tvUploadStatusBadge.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_active_bg));
        } else if (LectureSession.UPLOAD_STATUS_FAILED.equals(session.getUploadStatus())) {
            holder.tvUploadStatusBadge.setText("Upload Failed ⚠️");
            holder.tvUploadStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_recording));
            holder.tvUploadStatusBadge.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_recording_bg));
        } else {
            holder.tvUploadStatusBadge.setText("Pending ⏳");
            holder.tvUploadStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent));
            holder.tvUploadStatusBadge.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_idle_bg));
        }

        // Silence / No Speech check
        if (LectureSession.STATUS_NO_SPEECH.equals(session.getSessionStatus())) {
            holder.tvNoSpeechTag.setVisibility(View.VISIBLE);
        } else {
            holder.tvNoSpeechTag.setVisibility(View.GONE);
        }

        // Transcript snippet
        String transcript = session.getTranscript();
        if (transcript != null && !transcript.isEmpty()) {
            holder.tvTranscriptSnippet.setText(transcript);
        } else {
            holder.tvTranscriptSnippet.setText("No transcript available.");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLectureClick(session);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHistoryCourse;
        TextView tvUploadStatusBadge;
        TextView tvHistoryDate;
        TextView tvHistoryTime;
        TextView tvNoSpeechTag;
        TextView tvTranscriptSnippet;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryCourse = itemView.findViewById(R.id.tvHistoryCourse);
            tvUploadStatusBadge = itemView.findViewById(R.id.tvUploadStatusBadge);
            tvHistoryDate = itemView.findViewById(R.id.tvHistoryDate);
            tvHistoryTime = itemView.findViewById(R.id.tvHistoryTime);
            tvNoSpeechTag = itemView.findViewById(R.id.tvNoSpeechTag);
            tvTranscriptSnippet = itemView.findViewById(R.id.tvTranscriptSnippet);
        }
    }
}
