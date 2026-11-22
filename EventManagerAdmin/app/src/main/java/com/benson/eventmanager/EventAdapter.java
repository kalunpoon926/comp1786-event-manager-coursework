package com.benson.eventmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnEventClickListener listener;
    

    public interface OnEventClickListener {
        void onEditClick(Event event);
        void onDeleteClick(Event event);
        void onSessionsClick(Event event);
        void onDetailsClick(Event event);
    }

    public EventAdapter(List<Event> eventList, OnEventClickListener listener, DBManager dbManager) {
        this.eventList = eventList;
        this.listener = listener;
        
    }

    public void setEventList(List<Event> eventList) {
        this.eventList = eventList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.tvName.setText(event.getName());
        holder.tvDate.setText(event.getDate());
        holder.tvLocation.setText(event.getLocation());
        holder.tvOrganiser.setText(event.getOrganiser());
        holder.tvType.setText(event.getType());
        holder.tvStatus.setText(event.getStatus());
        holder.tvSessionCount.setText("Sessions: " + event.getSessionCount());
        holder.itemView.setOnClickListener(v -> listener.onDetailsClick(event));

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(event));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(event));
        holder.btnSessions.setOnClickListener(v -> listener.onSessionsClick(event));
    }

    @Override
    public int getItemCount() {
        return eventList != null ? eventList.size() : 0;
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDate, tvLocation, tvOrganiser, tvType, tvStatus, tvSessionCount;
        Button btnEdit, btnDelete, btnSessions;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvOrganiser = itemView.findViewById(R.id.tvOrganiser);
            tvType = itemView.findViewById(R.id.tvType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvSessionCount = itemView.findViewById(R.id.tvSessionCount);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnSessions = itemView.findViewById(R.id.btnSessions);
        }
    }
}