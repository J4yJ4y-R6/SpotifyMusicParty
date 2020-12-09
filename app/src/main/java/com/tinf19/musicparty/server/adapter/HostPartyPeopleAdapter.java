package com.tinf19.musicparty.server.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.PartyPerson;

import java.util.List;

/**
 * HostPartyPeopleAdapter
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class HostPartyPeopleAdapter extends RecyclerView.Adapter<HostPartyPeopleAdapter.ViewHolder> {

    private List<PartyPerson> mDataset;

    /**
     * This ViewHolder is assigning the objects from row_host_party_people.xml to the global
     * view-variables
     * @see TextView
     */
    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView partyPeopleNameTextView;
        public TextView partyPeopleDurationTextView;

        /**
         * Constructor to assign the parent view of each row
         * @param itemView parent view from row_host_party_people.xml
         */
        public ViewHolder(View itemView) {
            super(itemView);
            partyPeopleNameTextView = (TextView) itemView.findViewById(R.id.partyPeopleNameTextView);
            partyPeopleDurationTextView = (TextView) itemView.findViewById(R.id.partyPeopleDurationTextView);
        }
    }

    /**
     * Constructor to set the current {@link List} of all connected clients to the local dataset
     * @param partyPersonList {@link List} of type {@link PartyPerson} of all connected clients
     */
    public HostPartyPeopleAdapter(List<PartyPerson> partyPersonList) { this.mDataset = partyPersonList; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View textView = inflater.inflate(R.layout.row_host_party_people, parent, false);
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
            String durationTime = (int)((duration / 1000) / 60) + " " +  holder.itemView.getContext().getString(R.string.text_minutes);
            durationTV.setText(durationTime);
        }
    }

    @Override
    public int getItemCount() { return mDataset.size(); }

    /**
     * Constructor to set the current {@link List} of type {@link PartyPerson} of all connected clients to the local dataset
     * @param mDataset {@link List} of all connected clients
     */
    public void setDataset(List<PartyPerson> mDataset) {
        this.mDataset = mDataset;
    }
}
