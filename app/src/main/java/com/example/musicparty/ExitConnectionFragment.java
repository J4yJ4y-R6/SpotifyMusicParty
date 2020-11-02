package com.example.musicparty;

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

public class ExitConnectionFragment extends Fragment {

    public ConfirmExit confirmExit;
    private TextView partyNameTextView;
    private static final String NAME = ExitConnectionFragment.class.getName();

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

        partyNameTextView = view.findViewById(R.id.leavePartyNameTextView);
        Button denyButton = view.findViewById(R.id.denyLeavePartyButton);
        Button acceptButton = view.findViewById(R.id.acceptLeavePartyButton);
        denyButton.setOnClickListener(v -> confirmExit.denyExit());
        acceptButton.setOnClickListener(v -> confirmExit.acceptExit());

        return view;
    }

    public void setPartyName(String name) {
        //TODO: Format String partyName
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