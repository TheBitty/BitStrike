# BitStrike C2 Framework

A Java-based Command and Control (C2) server with native agents for security research and educational purposes.

**IMPORTANT**: This software is intended for legitimate security research, educational purposes, and authorized penetration testing only. Do not use for any illegal activities.

## Project Overview

BitStrike is a lightweight C2 framework consisting of:

1. A Java-based C2 server that manages agent communications and provides a command console
2. Native agents written in C for various platforms (Phase 1 includes Windows)

## Features (Phase 1)

- **Server**:
  - HTTP/HTTPS communication with agents
  - Agent registration and management
  - Command queuing system
  - Console interface for managing agents
  - Encrypted communications
  - Command execution on connected agents

- **Windows Agent**:
  - HTTP/HTTPS communication with C2 server
  - System information collection
  - Command execution via cmd.exe
  - Sleep with jitter pattern
  - Basic anti-analysis features
  - Encrypted communications

## Setup

### Prerequisites

- **Server**:
  - Java 11 or higher
  - Maven

- **Windows Agent**:
  - MinGW-w64 (for cross-compilation on non-Windows systems)
  - Windows build tools (if building on Windows)

### Building the Server

```bash
cd BitStrike/server
mvn clean package
```

The server JAR will be generated in the `target` directory.

### Building the Windows Agent

#### Option 1: Cross-compile on macOS/Linux using MinGW-w64

1. Install MinGW-w64
   - macOS: `brew install mingw-w64`
   - Ubuntu: `apt install mingw-w64`

2. Build the agent
   ```bash
   # Using the Makefile
   cd BitStrike/agents/windows
   make
   
   # OR using the provided build script on macOS
   cd BitStrike/agents/windows
   ./build_mac.sh
   ```

#### Option 2: Build on Windows

1. Install build tools (MinGW or Visual Studio Build Tools)
2. Compile with:
   ```
   gcc -Wall -o agent.exe agent.c -lwinhttp -lws2_32
   ```

## Usage

### Running the Server

```bash
java -jar server/target/c2-server-1.0-SNAPSHOT.jar
```

The server will start on port 8443.

### Server Commands

Once the server is running, you can use the following commands in the console:

- `help` - Show available commands
- `list` - List all connected agents
- `shell <agent_id> <command>` - Execute shell command on agent
- `sleep <agent_id> <seconds>` - Set agent sleep interval
- `kill <agent_id>` - Terminate agent
- `exit` - Exit the C2 server

### Agent Execution

Simply run the agent executable on the target Windows system:

```
agent.exe
```

The agent will automatically connect to the C2 server defined in the agent code.

## Testing on macOS

Since the Windows agent relies on Windows-specific APIs like WinHTTP and other Windows SDK functions, you need a Windows environment to properly run and test the agent. Here are your options:

### Option 1: Virtual Machine (Recommended)

1. Install a virtualization solution:
   - VMware Fusion (commercial)
   - Parallels Desktop (commercial)
   - VirtualBox (free)

2. Set up a Windows 10/11 VM

3. Cross-compile the agent on your Mac:
   ```bash
   cd BitStrike/agents/windows
   ./build_mac.sh
   ```

4. Copy the generated `agent.exe` to your Windows VM

5. Run the C2 server on your Mac
   ```bash
   java -jar server/target/c2-server-1.0-SNAPSHOT.jar
   ```

6. Configure your VM's network settings to allow communication with the host machine

7. Update the C2 server address in the agent code to point to your Mac's IP address, then recompile

8. Run the agent in the Windows VM

### Option 2: Remote Windows Machine

If you have access to a remote Windows machine or server:

1. Cross-compile the agent on your Mac
2. Copy the agent to the Windows machine
3. Run the C2 server on your Mac (ensure it's accessible from the Windows machine)
4. Run the agent on the Windows machine

### Option 3: Wine (Limited Testing Only)

Wine can run some Windows applications on macOS, but with limitations, especially for applications using Windows-specific APIs:

1. Install Wine:
   ```bash
   brew install --cask wine-stable
   ```

2. Try running the agent:
   ```bash
   wine agent.exe
   ```

**Note**: The agent likely won't work properly under Wine due to WinHTTP dependencies, but it's useful for basic testing.

## Development Environment Setup for macOS

For macOS users, we've included configuration files to help with development:

1. VS Code will show Windows header errors since these headers aren't available on macOS
2. Use the included `.vscode/c_cpp_properties.json` for IDE integration
3. For actual testing, always use a Windows VM or Windows machine

## Project Structure

```
BitStrike/
├── server/                    # Java C2 server
│   ├── src/                   # Server source code
│   └── pom.xml                # Maven configuration
├── agents/                    # Agent implementations
│   └── windows/               # Windows agent
│       ├── agent.c            # Windows agent source code
│       ├── Makefile           # Build script for Windows agent
│       └── build_mac.sh       # macOS cross-compilation script
└── docs/                      # Documentation
```

## Security Considerations

- This is a basic implementation for educational purposes
- In a production environment, you would want to:
  - Implement proper TLS certificate validation
  - Use more secure key management
  - Add additional layers of obfuscation
  - Implement proper command sanitization
  - Add more robust anti-analysis features

## Troubleshooting

### Java Package Errors

If you see package resolution errors in your IDE:
1. Make sure Maven dependencies are properly downloaded: `cd server && mvn dependency:resolve`
2. Ensure your IDE recognizes the project structure: Import the project as a Maven project
3. Check that your Java version matches the one in `pom.xml` (Java 11)

### Windows Agent Build Errors

1. Make sure MinGW-w64 is properly installed: `brew install mingw-w64`
2. Check that Windows headers are found by the compiler
3. Try using the provided `build_mac.sh` script

### C2 Server Connection Issues

1. Check firewall settings on both machines
2. Ensure the C2 host/port in the agent code matches your server
3. Test basic connectivity with `ping` and `telnet`

## Future Development (Beyond Phase 1)

- Additional agent platforms (Linux, macOS)
- Web-based administration interface
- File upload/download capabilities
- More advanced anti-detection techniques
- Plugin system for extending functionality 