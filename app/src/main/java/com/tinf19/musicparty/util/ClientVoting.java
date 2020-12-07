package com.tinf19.musicparty.util;

import com.tinf19.musicparty.music.Track;

import org.json.JSONException;
import org.json.JSONObject;

public class ClientVoting implements Voting {

    private final Track track;
    private final Type type;
    private final int id;
    private final ClientVotingCallback clientVotingCallback;
    private boolean voted = false;

    public interface ClientVotingCallback {
        void submitVoting(int vote, int id);
    }


    public ClientVoting(Track track, Type type, int id, ClientVotingCallback clientVotingCallback) {
        this.track = track;
        this.type = type;
        this.id = id;
        this.clientVotingCallback = clientVotingCallback;
    }

    public ClientVoting(String json, ClientVotingCallback clientVotingCallback) throws JSONException {
        JSONObject tempObject = new JSONObject(json);
        this.track = new Track(tempObject.getString(Constants.TRACK));
        this.type = tempObject.getString(Constants.TYPE).equals("QUE") ? Type.QUE : Type.SKIP;
        this.id = tempObject.getInt(Constants.ID);
        this.clientVotingCallback = clientVotingCallback;
    }

    @Override
    public void addVoting(int vote, Thread thread) {
        this.voted = true;
        clientVotingCallback.submitVoting(vote, id);
    }

    @Override
    public Track getTrack() {
        return track;
    }

    @Override
    public boolean isVoted() {
        return voted;
    }

    @Override
    public Type getType() {
        return type;
    }

}
