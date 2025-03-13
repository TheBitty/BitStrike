package com.bitstrike.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a command to be sent to an agent.
 */
public class Command {
    private final String id;
    private final String command;
    private final CommandType type;
    private final Instant timestamp;
    private String result;
    private CommandStatus status;
    
    public Command(String command, CommandType type) {
        this.id = UUID.randomUUID().toString();
        this.command = command;
        this.type = type;
        this.timestamp = Instant.now();
        this.status = CommandStatus.PENDING;
    }
    
    public void setResult(String result) {
        this.result = result;
        this.status = CommandStatus.COMPLETED;
    }
    
    public void setError(String error) {
        this.result = error;
        this.status = CommandStatus.FAILED;
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getCommand() {
        return command;
    }
    
    public CommandType getType() {
        return type;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getResult() {
        return result;
    }
    
    public CommandStatus getStatus() {
        return status;
    }
    
    /**
     * Types of commands that can be sent to agents.
     */
    public enum CommandType {
        SHELL,       // Execute shell command
        UPLOAD,      // Upload file to agent
        DOWNLOAD,    // Download file from agent
        SLEEP,       // Change sleep interval
        KILL         // Terminate agent
    }
    
    /**
     * Status of a command.
     */
    public enum CommandStatus {
        PENDING,    // Command has been created but not sent
        SENT,       // Command has been sent to the agent
        COMPLETED,  // Command completed successfully
        FAILED      // Command failed to execute
    }
} 