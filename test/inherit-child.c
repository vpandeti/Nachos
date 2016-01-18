#include "syscall.h"

#define MSG_FILE "tmp1"
#define STR "Jingle Bells Jingle Bells Jingle all the way\n"
#define ERR_STR "Failed!\n"

main()
{
	int fd, fd1;
	char s[100];

	fd1 = Open(MSG_FILE);

	Read((char *)&fd, sizeof(int), fd1);
	Close(fd1);

	if (Read(s, sizeof(s), fd) < 0) {
		Write(ERR_STR, sizeof(ERR_STR)-1, ConsoleOutput);
		Exit(0);
	}
	Write(s, sizeof(STR)-1, ConsoleOutput);
	Close(fd);
}
