package com.github.kaiwinter.nfcsonos.rest.discover.model;

import java.util.List;

public class Group {
    public String id;
    public String name;
    public String coordinatorId;
    public String playbackState;
    public List<String> playerIds;

    @Override
    public String toString() {
        return name;
    }
}