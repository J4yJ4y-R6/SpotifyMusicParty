package com.tinf19.musicparty.server.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.PartyPerson;
import com.tinf19.musicparty.server.adapter.HostPartyPeopleAdapter;

import java.util.ArrayList;

/**
 * Fragment where the host can see who is connected to his party.
 * He sees the username and the time the client is at the party.
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class HostPartyPeopleFragment extends Fragment {

    private HostPartyPeopleAdapter hostPartyPeopleAdapter;
    private HostPartyPeopleCallback hostPartyPeopleCallback;

    public interface HostPartyPeopleCallback {
        ArrayList<PartyPerson> getPartyPeopleList();
    }

    /**
     * Constructor to set the callback
     * @param hostPartyPeopleCallback Communication callback for
     *                                {@link com.tinf19.musicparty.server.HostActivity}
     */
    public HostPartyPeopleFragment(HostPartyPeopleCallback hostPartyPeopleCallback) { this.hostPartyPeopleCallback = hostPartyPeopleCallback; }

    /**
     * Empty-Constructor which is necessary in fragments
     */
    public HostPartyPeopleFragment() { }



    //Android lifecycle methods

    @Override
    public void onStart() {
        super.onStart();
        hostPartyPeopleAdapter.setDataset(hostPartyPeopleCallback.getPartyPeopleList());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof HostPartyPeopleCallback)
            hostPartyPeopleCallback = (HostPartyPeopleCallback) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_host_party_people, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.partyPeopleRecyclerView);
        if(recyclerView != null) {
            hostPartyPeopleAdapter = new HostPartyPeopleAdapter(new ArrayList<>());
            recyclerView.setAdapter(hostPartyPeopleAdapter);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(view.getContext());
            recyclerView.setLayoutManager(layoutManager);
        }
        return view;
    }
}