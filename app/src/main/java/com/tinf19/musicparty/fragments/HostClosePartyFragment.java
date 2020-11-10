package com.tinf19.musicparty.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.tinf19.musicparty.R;

public class HostClosePartyFragment extends Fragment {

    private ClosePartyCallback closePartyCallback;

    public interface ClosePartyCallback {
        void denyEndParty();
        void acceptEndParty();
    }

    public HostClosePartyFragment(ClosePartyCallback closePartyCallback) {
        this.closePartyCallback = closePartyCallback;
    }

    public HostClosePartyFragment() {
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
        View view =  inflater.inflate(R.layout.fragment_host_close_party, container, false);

        Button denyEndPartyButton = view.findViewById(R.id.denyEndPartyButton);
        if(denyEndPartyButton != null) {
            denyEndPartyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closePartyCallback.denyEndParty();
                }
            });
        }
        Button acceptEndPartyButton = view.findViewById(R.id.acceptEndPartyButton);
        if(acceptEndPartyButton != null) {
            acceptEndPartyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closePartyCallback.acceptEndParty();
                }
            });
        }

        return view;
    }
}