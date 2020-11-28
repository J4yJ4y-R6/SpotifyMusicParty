package com.tinf19.musicparty.util;

import okhttp3.MediaType;

public class Constants {
    public static final String TOKEN = "token";
    public static final String PASSWORD = "password";
    public static final String CODE = "code";
    public static final String ADDRESS = "address";
    public static final String USERNAME = "username";
    public static final String PARTYNAME = "partyname";
    public static final String TAG = "tag";
    public static final String SERVICE = "service";
    public static final String FIRST_CONNECTION = "first_connection";


    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String COVER = "cover";
    public static final String ARTIST = "artist";
    public static final String DURATION = "duration";
    public static final String ALBUM = "album";
    public static final String COVER_FULL = "cover_full";

    
    public static final String CHANNEL_ID = "musicPartyChannel";
    public static final String REDIRECT_URI = "http://com.example.musicparty/callback";
    public static final int REQUEST_CODE = 1337;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    public static final String HOST = "api.spotify.com";
    public static final int PORT = 1403;
    public static final String STOP = "STOP_SERVICE";
    public static final short LOADING_TIME = 5;
    public static final int NOTIFY_ID = 1;
    public static final int CROSSFADE = 5;

    public static final int RESULT_LOAD_IMAGE = 1;
    public static final float ALPHA_FULL = 1.0f;
    public static final String LEXICON = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345674890";
}
