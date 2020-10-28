package com.example.musicparty;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.util.List;


public class PartyAcRecycAdapter extends RecyclerView.Adapter<PartyAcRecycAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView nameTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = (TextView) itemView.findViewById(R.id.tvAnimalName);
        }
    }

    private List<String> mDataset;
    public PartyAcRecycAdapter(List<String> test) {
        mDataset = test;
    }

    @NonNull
    @Override
    public PartyAcRecycAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View textView = inflater.inflate(R.layout.partyacrecyclerview_row, parent, false);

        ViewHolder viewHolder = new ViewHolder(textView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = mDataset.get(position);
        TextView textView = holder.nameTextView;
        textView.setText(name);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
