package com.tinf19.musicparty.util;

import com.tinf19.musicparty.music.Track;

public interface Voting {
    void addVoting(int vote, Thread thread);
    Track getTrack();
    boolean isVoted(Thread thread);
    Type getType();
    int[] getVoteListSizes();
    int getId();
}
