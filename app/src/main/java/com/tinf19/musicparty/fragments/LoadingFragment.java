package com.tinf19.musicparty.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tinf19.musicparty.R;


/**
 * Fragment to show that the user is connecting to Spotify
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class LoadingFragment extends Fragment {

    private String message;

    /**
     * Constructor to set the message depending if the call came from client or server
     * @param message Message the user see in the fragment
     */
    public LoadingFragment(String message) {
        this.message = message;
    }

    /**
     * Empty-Constructor which is necessary in fragments
     */
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