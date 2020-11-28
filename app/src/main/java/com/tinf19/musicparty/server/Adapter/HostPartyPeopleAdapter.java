package com.tinf19.musicparty.server.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.PartyPeople;

import java.util.List;

public class HostPartyPeopleAdapter extends RecyclerView.Adapter<HostPartyPeopleAdapter.ViewHolder> {

    private static final String TAG = HostPartyPeopleAdapter.class.getName();
    private List<PartyPeople> mDataset;

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView partyPeopleNameTextView;
        public TextView partyPeopleDurationTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            partyPeopleNameTextView = (TextView) itemView.findViewById(R.id.partyPeopleNameTextView);
            partyPeopleDurationTextView = (TextView) itemView.findViewById(R.id.partyPeopleDurationTextView);
        }
    }

    public HostPartyPeopleAdapter(List<PartyPeople> partyPeopleList) {
        this.mDataset = partyPeopleList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View textView = inflater.inflate(R.layout.party_people_recyc_view_row, parent, false);
        return new ViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = mDataset.get(position).getUsername();
        long duration = mDataset.get(position).getDuration();
        TextView nameTV = holder.partyPeopleNameTextView;
        if(nameTV != null)
            nameTV.setText(name);
        TextView durationTV = holder.partyPeopleDurationTextView;
        if(durationTV != null) {
            String durationTime = (int)((duration / 1000)/60) + " Minuten";
            durationTV.setText(durationTime);
        }
    }

    @Override
    public int getItemCount() { return mDataset.size(); }

    public void setDataset(List<PartyPeople> mDataset) {
        this.mDataset = mDataset;
    }
}
