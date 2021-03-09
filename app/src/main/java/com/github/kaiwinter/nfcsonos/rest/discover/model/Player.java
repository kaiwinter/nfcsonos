package com.github.kaiwinter.nfcsonos.rest.discover.model;

import java.util.List;

public class Player {
    public String id;
    public String name;
    public String websocketUrl;
    public String softwareVersion;
    public String apiVersion;
    public String minApiVersion;
    public Boolean isUnregistered;
    public List<String> capabilities;
    public List<String> deviceIds;
}