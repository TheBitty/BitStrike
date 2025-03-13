package com.bitstrike.models;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents an agent connected to the C2 server.
 */
public class Agent {
    private final String id;
    private final String hostname;
    private final String username;
    private final String osVersion;
    private final String ipAddress;
    private Instant firstSeen;
    private Instant lastSeen;
    private final ConcurrentLinkedQueue<Command> commandQueue;
    
    public Agent(String hostname, String username, String osVersion, String ipAddress) {
        this.id = UUID.randomUUID().toString();
        this.hostname = hostname;
        this.username = username;
        this.osVersion = osVersion;
        this.ipAddress = ipAddress;
        this.firstSeen = Instant.now();
        this.lastSeen = Instant.now();
        this.commandQueue = new ConcurrentLinkedQueue<>();
    }
    
    // Constructor with explicit ID (for when agent reconnects)
    public Agent(String id, String hostname, String username, String osVersion, String ipAddress) {
        this.id = id;
        this.hostname = hostname;
        this.username = username;
        this.osVersion = osVersion;
        this.ipAddress = ipAddress;
        this.firstSeen = Instant.now();
        this.lastSeen = Instant.now();
        this.commandQueue = new ConcurrentLinkedQueue<>();
    }
    
    public void updateLastSeen() {
        this.lastSeen = Instant.now();
    }
    
    public void addCommand(Command command) {
        commandQueue.add(command);
    }
    
    public Command getNextCommand() {
        return commandQueue.poll();
    }
    
    public boolean hasCommands() {
        return !commandQueue.isEmpty();
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getOsVersion() {
        return osVersion;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public Instant getFirstSeen() {
        return firstSeen;
    }
    
    public Instant getLastSeen() {
        return lastSeen;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s@%s (%s) - Last seen: %s", 
                id.substring(0, 8), 
                username, 
                hostname, 
                osVersion, 
                lastSeen);
    }
} 