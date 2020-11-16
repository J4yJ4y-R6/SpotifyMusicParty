package com.tinf19.musicparty.fragments;

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
import com.tinf19.musicparty.music.PartyPeople;
import com.tinf19.musicparty.server.HostActivity;
import com.tinf19.musicparty.util.PartyPeopleRecycAdapter;

import static com.tinf19.musicparty.util.Constants.STATE_COUNTER;

import java.util.ArrayList;

public class PartyPeopleFragment extends Fragment {

    private static final String TAG = PartyPeopleFragment.class.getName();
    private int mCounter;
    private RecyclerView recyclerView;
    private PartyPeopleRecycAdapter partyPeopleRecycAdapter;
    private PartyPeopleList partyPeopleList;
    private ArrayList<PartyPeople> partyPeopleArrayList;
    private HostActivity hostActivity = new HostActivity();

    public interface PartyPeopleList {
        ArrayList<PartyPeople> getPartyPeopleList();
    }

    public PartyPeopleFragment(PartyPeopleList partyPeopleList) {
        this.partyPeopleList = partyPeopleList;
    }

    public PartyPeopleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_COUNTER, mCounter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        partyPeopleRecycAdapter.setDataset(partyPeopleList.getPartyPeopleList());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof PartyPeopleList)
            partyPeopleList = (PartyPeopleList) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_party_people, container, false);

        if(savedInstanceState != null)
            mCounter = savedInstanceState.getInt(STATE_COUNTER, 0);

        recyclerView = view.findViewById(R.id.partyPeopleRecyclerView);
        if(recyclerView != null) {
            partyPeopleRecycAdapter = new PartyPeopleRecycAdapter(new ArrayList<>());
            recyclerView.setAdapter(partyPeopleRecycAdapter);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(view.getContext());
            recyclerView.setLayoutManager(layoutManager);
        }
        return view;
    }
}