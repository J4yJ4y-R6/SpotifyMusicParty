package com.tinf19.musicparty.music;

/**
 * PartyPerson object to create a person with an username and the time he is connected to the
 * server.
 * It is used to show the host who has joined hin party and how long he is connected. This
 * information is printed out in the RecyclerView of
 * {@link com.tinf19.musicparty.server.fragments.HostPartyPeopleFragment}
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class PartyPerson {

    private String username;
    private long duration;

    /**
     * Constructor to set the information about the PartyPerson
     * @param username Username of the PartyPerson
     * @param duration Duration since the PartyPerson joined the party
     */
    public PartyPerson(String username, long duration) {
        this.username = username;
        this.duration = duration;
    }

    /**
     * @return Get Username from a PartyPerson
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return Get Duration since the PartyPerson joined the party
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Set username of a PartyPerson
     * @param username Username of the PartyPerson
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Set Duration since the PartyPerson joined the party
     * @param duration Duration since the PartyPerson joined the party
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }
}
