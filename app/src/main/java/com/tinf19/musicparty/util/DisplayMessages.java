package com.tinf19.musicparty.util;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.tinf19.musicparty.R;

public class DisplayMessages {

    private final String text;
    private final View view;

    public DisplayMessages(String text, View view) {
        this.text = text;
        this.view = view;
    }

    public void makeMessage() {
        Log.d(DisplayMessages.class.getName(), "makeMessage: " + text + view);
        Snackbar.make(view, text, Snackbar.LENGTH_SHORT)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .show();
    }
}
