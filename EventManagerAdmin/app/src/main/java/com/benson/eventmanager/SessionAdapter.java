package com.benson.eventmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private List<Session> sessionList;
    private OnSessionClickListener listener;

    public interface OnSessionClickListener {
        void onEditClick(Session session);
        void onDeleteClick(Session session);
        void onDetailsClick(Session session);
    }

    public SessionAdapter(List<Session> sessionList, OnSessionClickListener listener) {
        this.sessionList = sessionList;
        this.listener = listener;
    }

    public void setSessionList(List<Session> sessionList) {
        this.sessionList = sessionList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        Session s = sessionList.get(position);
        holder.tvName.setText(s.getName());
        holder.tvTime.setText(s.getTime());
        holder.tvType.setText(s.getType());
        holder.tvSpeaker.setText(s.getSpeaker().isEmpty() ? "Speaker: -" : "Speaker: " + s.getSpeaker());
        holder.tvRoom.setText(s.getRoom().isEmpty() ? "Room: -" : "Room: " + s.getRoom());

        holder.itemView.setOnClickListener(v -> listener.onDetailsClick(s));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(s));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(s));
    }

    @Override
    public int getItemCount() {
        return sessionList != null ? sessionList.size() : 0;
    }

    public static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvType, tvSpeaker, tvRoom;
        Button btnEdit, btnDelete;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvType = itemView.findViewById(R.id.tvType);
            tvSpeaker = itemView.findViewById(R.id.tvSpeaker);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}