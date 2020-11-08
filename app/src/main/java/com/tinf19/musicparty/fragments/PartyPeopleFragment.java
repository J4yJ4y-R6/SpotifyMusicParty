package com.tinf19.musicparty.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.PartyPeople;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.server.HostActivity;
import com.tinf19.musicparty.util.ClientPlaylistRecycAdapter;
import com.tinf19.musicparty.util.PartyPeopleRecycAdapter;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class PartyPeopleFragment extends Fragment {

    private RecyclerView recyclerView;
    private PartyPeopleRecycAdapter partyPeopleRecycAdapter;

    private ArrayList<PartyPeople> partyPeopleList;
    private HostActivity hostActivity = new HostActivity();

    public PartyPeopleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_party_people, container, false);
        recyclerView = view.findViewById(R.id.partyPeopleRecyclerView);
        partyPeopleList = hostActivity.getPartyPeople();
        if(recyclerView != null) {
            partyPeopleRecycAdapter = new PartyPeopleRecycAdapter(partyPeopleList);
            recyclerView.setAdapter(partyPeopleRecycAdapter);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(view.getContext());
            recyclerView.setLayoutManager(layoutManager);
        }
        return view;
    }
}