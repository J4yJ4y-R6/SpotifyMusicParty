package com.tinf19.musicparty.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tinf19.musicparty.R;


public class LoadingFragment extends Fragment {

    private String message;

    public LoadingFragment(String message) {
        this.message = message;
    }

    public LoadingFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_loading, container, false);
        TextView messageTextView = view.findViewById(R.id.loadingMessageTextView);
        if(messageTextView != null)
            messageTextView.setText(message);
        return view;
    }
}