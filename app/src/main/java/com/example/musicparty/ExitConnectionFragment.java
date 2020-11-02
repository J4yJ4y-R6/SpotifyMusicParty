package com.example.musicparty;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

public class ExitConnectionFragment extends Fragment {

    public ConfirmExit confirmExit;

    public interface ConfirmExit {
        void denyExit();
        void acceptExit();
    }

    public ExitConnectionFragment(ConfirmExit confirmExit) {
        this.confirmExit = confirmExit;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exit_connection, container, false);
        //binding = ActivityPartyBinding.inflate(getLayoutInflater());

        Button denyButton = view.findViewById(R.id.denyLeaveParty);
        Button acceptButton = view.findViewById(R.id.acceptLeaveParty);
        denyButton.setOnClickListener(v -> confirmExit.denyExit());
        acceptButton.setOnClickListener(v -> confirmExit.acceptExit());

        return view;
    }
}