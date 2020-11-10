package com.tinf19.musicparty.fragments;

import android.graphics.Typeface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Spannable;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tinf19.musicparty.R;

public class ExitConnectionFragment extends Fragment {

    public ConfirmExit confirmExit;
    private TextView partyNameTextView;
    private static final String NAME = ExitConnectionFragment.class.getName();

    public interface ConfirmExit {
        void denyExit();
        void acceptExit();
        String getPartyName();
    }

    public ExitConnectionFragment(ConfirmExit confirmExit) {
        this.confirmExit = confirmExit;
    }

    public ExitConnectionFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setPartyName(confirmExit.getPartyName());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exit_connection, container, false);
        //binding = ActivityPartyBinding.inflate(getLayoutInflater());

        partyNameTextView = view.findViewById(R.id.leavePartyNameTextView);
        Button denyButton = view.findViewById(R.id.denyLeavePartyButton);
        Button acceptButton = view.findViewById(R.id.acceptLeavePartyButton);
        denyButton.setOnClickListener(v -> confirmExit.denyExit());
        acceptButton.setOnClickListener(v -> confirmExit.acceptExit());

        return view;
    }

    public void setPartyName(String name) {
        if(partyNameTextView != null) {
            Log.d(NAME, name);
            partyNameTextView.setText(name, TextView.BufferType.SPANNABLE);
            Spannable spannable = (Spannable)partyNameTextView.getText();
            int start = 0;
            int end = name.length();
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}