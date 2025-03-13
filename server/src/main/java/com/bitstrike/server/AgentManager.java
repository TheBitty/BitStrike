package com.bitstrike.server;

import com.bitstrike.models.Agent;
import com.bitstrike.models.Command;
import com.bitstrike.models.Command.CommandType;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the registration and tracking of agents.
 */
public class AgentManager {
    private static final Logger logger = LoggerFactory.getLogger(AgentManager.class);
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    
    /**
     * Register a new agent or update an existing one.
     */
    public Agent registerAgent(String agentId, String hostname, String username, String osVersion, String ipAddress) {
        Agent agent;
        
        if (agentId != null && agents.containsKey(agentId)) {
            // Existing agent is checking in
            agent = agents.get(agentId);
            agent.updateLastSeen();
            logger.info("Agent checked in: {}", agent);
        } else {
            // New agent registration
            if (agentId == null) {
                agent = new Agent(hostname, username, osVersion, ipAddress);
            } else {
                agent = new Agent(agentId, hostname, username, osVersion, ipAddress);
            }
            agents.put(agent.getId(), agent);
            logger.info("New agent registered: {}", agent);
        }
        
        return agent;
    }
    
    /**
     * Get an agent by its ID.
     */
    public Agent getAgent(String agentId) {
        return agents.get(agentId);
    }
    
    /**
     * Get all registered agents.
     */
    public Collection<Agent> getAllAgents() {
        return agents.values();
    }
    
    /**
     * Add a command to an agent's queue.
     */
    public boolean addCommand(String agentId, String commandStr, CommandType type) {
        Agent agent = agents.get(agentId);
        if (agent == null) {
            logger.error("Attempted to add command to non-existent agent: {}", agentId);
            return false;
        }
        
        Command command = new Command(commandStr, type);
        agent.addCommand(command);
        logger.info("Added command to agent {}: {}", agentId, commandStr);
        return true;
    }
    
    /**
     * Record the result of a command.
     */
    public boolean setCommandResult(String agentId, String commandId, String result, boolean success) {
        // In a real implementation, we would store command results
        // For simplicity in Phase 1, we'll just log them
        if (success) {
            logger.info("Command {} completed on agent {}: {}", commandId, agentId, result);
        } else {
            logger.error("Command {} failed on agent {}: {}", commandId, agentId, result);
        }
        return true;
    }
} 