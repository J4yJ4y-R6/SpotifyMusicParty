package com.tinf19.musicparty.util;

import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.server.ServerService;

import java.util.ArrayList;
import java.util.List;

public class Voting {

    private final long created;
    private final Type type;
    private final Track track;
    private final double threshold;
    private final String name;
    private final VotingCallback votingCallback;
    private List<Thread> accepted = new ArrayList<>();
    private List<Thread> denied = new ArrayList<>();

    public interface VotingCallback {
        void skipNext(String name);
        void addAndClose(String name);
        int getClientCount();
        void close(String name);
    }

    public Voting(Type type, Track track, double threshold, String name, VotingCallback votingCallback) {
        this.type = type;
        this.threshold = threshold;
        this.name = name;
        this.created = System.currentTimeMillis();
        this.track = track;
        this.votingCallback = votingCallback;
    }

    public Type getType() {
        return type;
    }

    public long getCreated() {
        return created;
    }

    public Track getTrack() {
        return track;
    }

    public void evaluateResult(Response response, Thread thread) {
        if(accepted.contains(thread) || denied.contains(thread))
            return;
        switch (type) {
            case SKIP:
                if(response == Response.YES)
                    accepted.add(thread);
                break;
            case QUE:
                if(response == Response.YES)
                    accepted.add(thread);
                else if(response == Response.NO)
                    denied.add(thread);
                break;
        }
        makeCallback();
    }

    private void makeCallback(){
        switch (type) {
            case QUE:
                if(votingCallback.getClientCount()*threshold < accepted.size())
                    votingCallback.addAndClose(name);
                else if(votingCallback.getClientCount()*threshold < denied.size())
                    votingCallback.close(name);
                break;
            case SKIP:
                if(votingCallback.getClientCount()*threshold < accepted.size())
                    votingCallback.addAndClose(name);
                break;
        }
    }

    public enum Type {
        QUE,
        SKIP
    }

    public enum Response {
        YES,
        NO
    }
}
