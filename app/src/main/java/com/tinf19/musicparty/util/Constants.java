package com.tinf19.musicparty.util;

import okhttp3.MediaType;

public class Constants {
    public static final String PASSWORD = "password";
    public static final String CODE = "code";
    public static final String ADDRESS = "address";
    public static final String IP_ADDRESS = "ipaddress";
    public static final String USERNAME = "username";
    public static final String PARTYNAME = "partyname";
    public static final String TAG = "tag";
    public static final String SERVICE = "service";

    /**
     * Constant to decide whether the user opened the app for the first time
     */
    public static final String FIRST_CONNECTION = "first_connection";

    public static final String FROM_NOTIFICATION = "from_notification";

    public static final String AFTER_VOTING = "after_voting";


    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String COVER = "cover";
    public static final String ARTIST = "artist";
    public static final String DURATION = "duration";
    public static final String ALBUM = "album";
    public static final String COVER_FULL = "cover_full";

    public static final int IGNORED = 0;
    public static final int YES = 1;
    public static final int NO = 2;
    public static final String YES_VOTE = "yes";
    public static final String NO_VOTE = "no";
    public static final String GREY_VOTE = "grey";
    public static final String HAS_VOTED = "has_voted";
    public static final String VOTE = "vote";
    public static final String FINISHED_VOTE = "finishedVote";
    public static final String COMMAND = "command";
    public static final String MESSAGE = "message";
    public static final double THRESHOLD_VALUE = 0.5;

    public static final String TYPE = "type";
    public static final String THRESHOLD = "threshold";
    public static final String CREATED = "created";
    public static final String TRACK = "track";
    public static final String VOTINGCALLBACK = "votingcallback";


    /**
     * Channel-Id used for creating or updating a service notification
     */
    public static final String CHANNEL_ID = "musicPartyChannel";

    /**
     * Channel-Id used for creating or updating a voting notification
     */
    public static final String VOTING_CHANNEL_ID = "votingNotificationChannel";

    /**
     * Group up all notification into one group
     */
    public static final String NOTIFICATION_GROUP = "musicPartyGroup";

    /**
     * Used to authenticate the app with Spotify
     */
    public static final String REDIRECT_URI = "http://com.example.musicparty/callback";
    public static final String [] SERVER_SCOPES = new String[]{"streaming", "app-remote-control", "playlist-modify-private", "playlist-modify-public", "user-read-private", "ugc-image-upload", "user-read-playback-state"};

    /**
     * Prefix for Spotify-Image Http-Requests
     */
    public static final String IMAGE_URI = "https://i.scdn.co/image/";

    /**
     * Request code for Spotify login
     */
    public static final int REQUEST_CODE = 1337;

    /**
     * Json as a media type of a http request
     */
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    /**
     * Prefix of the Spotify-API which is necessary to make http requests to the API
     */
    public static final String HOST = "api.spotify.com";

    /**
     * Port used for connection between server and client.
     */
    public static final int PORT = 1403;

    /**
     * Command to stop the server from the service-notification from the host or to leave the party
     * from the service-notification from the client
     */
    public static final String STOP = "STOP_SERVICE";

    /**
     * Time (in minutes) the client tries to connect before returning to the MainActivity
     */
    public static final short LOADING_TIME = 5;

    /**
     * Notification-Id of the service notification for the server and the client
     */
    public static final int NOTIFY_ID = 1;

    /**
     * Notification-ID of the voting notification for the server and the client
     */
    public static final int VOTING_NOTIFY_ID = 2;

    /**
     * Group up all notifications by this app
     */
    public static final String CATEGORY_SERVICE = "MusicParty";

    /**
     * Spotify users can set a time between songs while they are overlap. This constant is used to
     * catch this so the queue is still synchronous.
     */
    public static final int CROSSFADE = 5;


    /**
     * Used to identify the action for the image picker used for changing playlist covers
     */
    public static final int RESULT_LOAD_IMAGE = 1;

    /**
     * Transparency number standing for opacity 1
     */
    public static final float ALPHA_FULL = 1.0f;

    /**
     * Lexicon to generate a random username with all letters from the latin alphabet and the
     * numbers from zero to nine.
     */
    public static final String LEXICON = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ12345674890";

    /**
     * Delimiter for splitting the message send by clients or servers.
     */
    public static final String DELIMITER = "~";

    /**
     * Identifier for notification intent
     */
    public static final String FRAGMENT = "fragment";

    public static final String VOTE_FRAGMENT = "voteFragment";
}
