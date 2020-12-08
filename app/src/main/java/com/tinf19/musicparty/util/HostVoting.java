package com.tinf19.musicparty.util;

import android.graphics.ColorSpace;
import android.util.Log;

import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.server.HostService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HostVoting implements Voting {

    private static final String TAG = HostVoting.class.getName();
    private final long created;
    private final Type type;
    private final Track track;
    private final double threshold;
    private final int id;
    private final VotingCallback votingCallback;
    private List<Thread> accepted = new ArrayList<>();
    private List<Thread> denied = new ArrayList<>();
    private List<Thread> ignored = new ArrayList<>();

    public interface VotingCallback {
        void skipNext(int id);
        void addAndClose(int id);
        int getClientCount();
        void close(int id);
    }

    public HostVoting(Type type, Track track, double threshold, int id, VotingCallback votingCallback) {
        this.type = type;
        this.threshold = threshold;
        this.id = id;
        this.created = System.currentTimeMillis();
        this.track = track;
        this.votingCallback = votingCallback;
    }

    public HostVoting(String json, VotingCallback votingCallback) throws JSONException {
        JSONObject tempObject = new JSONObject(json);
        this.type = tempObject.getString(Constants.TYPE).equals("QUE") ? Type.QUE : Type.SKIP;
        this.threshold = tempObject.getDouble(Constants.THRESHOLD);
        this.id = tempObject.getInt(Constants.ID);
        this.created = tempObject.getLong(Constants.CREATED);
        this.track = new Track(tempObject.getString(Constants.TRACK));
        this.votingCallback = votingCallback;
    }



    public long getCreated() {
        return created;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int[] getVoteListSizes() {
        int yes = (100 * accepted.size())/(votingCallback.getClientCount() + 1);
        int no = (100 * denied.size())/(votingCallback.getClientCount() + 1);
        int grey = (100 * (votingCallback.getClientCount() + 1))/(votingCallback.getClientCount() + 1) - yes - no;
        return new int[]{yes, no, grey};
    }

    private void makeCallback(){
        switch (type) {
            case QUE:
                if(votingCallback.getClientCount()*threshold < accepted.size())
                    votingCallback.addAndClose(id);
                else if(votingCallback.getClientCount()*threshold < denied.size())
                    votingCallback.close(id);
                break;
            case SKIP:
                if(votingCallback.getClientCount()*threshold < accepted.size())
                    votingCallback.addAndClose(id);
                break;
        }
    }

    public boolean containsIgnored(Thread thread) {
        return ignored.contains(thread);
    }

    /**
     * A thread has voted for this voting. Depending on the vote one of the three Lists. At the end
     * of the method the size of the three lists are compered to the count of all threads at the
     * party
     * @param vote Voting result
     * @param thread Thread which has voted so he can not vote twice.
     */
    @Override
    public void addVoting(int vote, Thread thread) {
        if(!accepted.contains(thread) && !denied.contains(thread) && !ignored.contains(thread)) {
            if (vote == Constants.YES) {
                accepted.add(thread);
                Log.d(TAG, thread.getName() + " voted yes. Currently " + accepted.size() +
                        " threads have voted yes");
            } else if (vote == Constants.NO) {
                denied.add(thread);
                Log.d(TAG, thread.getName() + " voted no. Currently " + denied.size() +
                        " threads have voted no");
            } else {
                ignored.add(thread);
                Log.d(TAG, thread.getName() + " ignored the vote. Currently " +
                        ignored.size() + " threads have ignored the vote");
            }
        } else {
            Log.d(TAG, "thread already voted");
            if(vote == Constants.IGNORED)
                ignored.add(thread);
            return;
        }
        if((accepted.size() + denied.size() + ignored.size()) >= (votingCallback.getClientCount() + 1))
            Log.d(TAG, "all threads have voted with the result: \r\n" +
                    "accepted: " + accepted.size() + " \r\n" +
                    "denied: " + denied.size() + " \r\n" +
                    "ignored: " + ignored.size());
    }

    public void removeThread(Thread thread) {
        accepted.remove(thread);
        denied.remove(thread);
        ignored.remove(thread);
    }

    @Override
    public Track getTrack() {
        return track;
    }

    @Override
    public boolean isVoted(Thread thread) {
        return (accepted.contains(thread) || denied.contains(thread));
    }

    @Override
    public Type getType() {
        return type;
    }

    public String serialize(Thread thread) throws JSONException {
        JSONObject tempObject = new JSONObject();
        int yes = accepted.size();
        int no = denied.size();
        tempObject = tempObject
                .put(Constants.TYPE, type)
                .put(Constants.THRESHOLD, threshold)
                .put(Constants.ID, id)
                .put(Constants.CREATED, created)
                .put(Constants.TRACK, new JSONObject(track.serialize()))
                .put(Constants.YES_VOTE, yes)
                .put(Constants.NO_VOTE, no)
                .put(Constants.GREY_VOTE, (votingCallback.getClientCount() + 1) - yes - no)
                .put(Constants.HAS_VOTED, isVoted(thread));
        return tempObject.toString();
    }

    public String serializeResult() throws JSONException {
        JSONObject tempObject = new JSONObject();
        int yes = accepted.size();
        int no = denied.size();
        tempObject = tempObject
                .put(Constants.ID, id)
                .put(Constants.YES_VOTE, yes)
                .put(Constants.NO_VOTE, no)
                .put(Constants.GREY_VOTE, (votingCallback.getClientCount() + 1) - yes - no);
        return tempObject.toString();
    }
}
