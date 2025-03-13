CC = x86_64-w64-mingw32-gcc
CFLAGS = -Wall -Wextra -O2 -s
LDFLAGS = -lwinhttp -lws2_32

all: agent.exe

agent.exe: agent.c
	$(CC) $(CFLAGS) -o $@ $< $(LDFLAGS)

clean:
	rm -f agent.exe 