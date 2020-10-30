package com.example.musicparty;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

public class ShowSongFragment extends Fragment {

    public ExitButtonClicked exitButtonClicked;

    public interface ExitButtonClicked {
        void exitConnection();
    }

    public ShowSongFragment(ExitButtonClicked exitButtonClicked) {
        this.exitButtonClicked = exitButtonClicked;
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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_show_song, container, false);
        //binding = ActivityPartyBinding.inflate(getLayoutInflater());

        ImageButton exitButton = view.findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitButtonClicked.exitConnection();
            }
        });


        return view;
    }
}