package com.tinf19.musicparty.util;

import android.os.CountDownTimer;
import android.util.Log;

import com.tinf19.musicparty.music.Track;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Voting class for the host where all information about the voting are saved. Every voting has
 * three list containing all threads already voted sorted by the vote content.
 * The threshold is the limit of the acceptance of the {@link Track}. When the size of the accepted
 * list reached this threshold, the song gets added to the queue if the type is Type.QUE or deleted
 * from the queue if the type is Type.SKIP. The threshold can be changed in the
 * {@link com.tinf19.musicparty.server.fragments.HostSettingsFragment}.
 * The created value is used to close the voting after a amount of time which also can be changed
 * in the settings fragment.
 */
public class HostVoting implements Voting {

    private static final String TAG = HostVoting.class.getName();
    private final Type type;
    private final Track track;
    private final double threshold;
    private final int id;
    private final VotingCallback votingCallback;
    private List<Thread> accepted = new ArrayList<>();
    private List<Thread> denied = new ArrayList<>();
    private List<Thread> ignored = new ArrayList<>();
    private final CountDownTimer closeTimer;
    private int ignoredCount = 0;

    public interface VotingCallback {
        void skipAndClose(int id);
        void addAndClose(int id);
        int getClientCount();
        void close(int id);
        void notifyClients(HostVoting voting, Thread thread);
    }

    /**
     * Constructor to initialize the information about the voting
     * @param type Voting-Type
     * @param track Song which should be added or removed from the queue
     * @param threshold Limit of accepting the voting set by the host
     * @param id Voting-Id
     * @param votingCallback Communication callback for
     *                       {@link com.tinf19.musicparty.server.HostActivity}
     */
    public HostVoting(Type type, Track track, double threshold, int id, VotingCallback votingCallback) {
        this.type = type;
        this.threshold = threshold;
        this.id = id;
        this.track = track;
        this.votingCallback = votingCallback;
        closeTimer = new CountDownTimer(60*1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                if(Math.ceil((accepted.size() + denied.size()) * threshold) <= accepted.size())
                    votingCallback.addAndClose(id);
                else
                    votingCallback.close(id);
            }
        }.start();
    }



    //Getter

    /**
     * Evaluate the voting results regarding the threshold
     */
    private void makeCallback(){
        int clientCount = (int) Math.ceil((votingCallback.getClientCount() + 1 - ignoredCount) * threshold);
        switch (type) {
            case QUE:
                if(clientCount <= accepted.size())
                    votingCallback.addAndClose(id);
                else if(clientCount < denied.size())
                    votingCallback.close(id);
                break;
            case SKIP:
                if(clientCount <= accepted.size())
                    votingCallback.skipAndClose(id);
                break;
            default:
                votingCallback.close(id);
        }
    }

    /**
     * @param thread Thread which gets checked to be in the ignored list
     * @return Get true if the given thread is in the ignored list or false if it is not
     */
    public boolean containsIgnored(Thread thread) {
        return ignored.contains(thread);
    }

    /**
     * Removing thread from the result lists
     * @param thread Thread which gets removed from the result lists
     */
    public void removeThread(Thread thread) {
        accepted.remove(thread);
        denied.remove(thread);
        ignored.remove(thread);
    }

    public void closeVoting() {
        closeTimer.cancel();
    }

    /**
     * Serializing the HostVoting to send it as a message to all clients with all information about
     * about the voting.
     * @param thread Thread to check whether he has voted already
     * @return Get the serialized JSONObject as a string.
     * @throws JSONException when the creation of the JSONObject failed
     */
    public String serialize(Thread thread) throws JSONException {
        JSONObject tempObject = new JSONObject();
        int yes = accepted.size();
        int no = denied.size();
        tempObject = tempObject
                .put(Constants.TYPE, type)
                .put(Constants.THRESHOLD, threshold)
                .put(Constants.ID, id)
                .put(Constants.TRACK, new JSONObject(track.serialize()))
                .put(Constants.YES_VOTE, yes)
                .put(Constants.NO_VOTE, no)
                .put(Constants.GREY_VOTE, (votingCallback.getClientCount() + 1) - yes - no)
                .put(Constants.HAS_VOTED, isVoted(thread));
        return tempObject.toString();
    }

    /**
     * @return Get the serialized JSONObject with the current voting results and the unique voting-
     * id to identify the voting
     * @throws JSONException when the creation of the JSONObject failed
     */
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
                ignoredCount++;
                Log.d(TAG, thread.getName() + " ignored the vote. Currently " +
                        ignored.size() + " threads have ignored the vote");
            }
        } else {
            Log.d(TAG, "thread already voted");
            if(vote == Constants.IGNORED)
                ignored.add(thread);
            return;
        }
        makeCallback();
        votingCallback.notifyClients(this, thread);
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
}
