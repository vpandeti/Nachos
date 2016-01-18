#include "syscall.h"

/* checks whether children inherit open files */


#define DAT_FILE "tmp0"
#define MSG_FILE "tmp1"
#define STR "Jingle Bells Jingle Bells Jingle all the way\n"


main()
{
	int fd, fd1, pid;

	Create(DAT_FILE);
	Create(MSG_FILE);
	fd = Open(DAT_FILE);
	fd1 = Open(MSG_FILE);

	/* write STR to data file */
	Write(STR, sizeof(STR)-1, fd);

	/* reset the file pointer */
	Close(fd);
	fd = Open(DAT_FILE);

	/* write fd to msg file */
	Write((char *)&fd, sizeof(int), fd1);
	Close(fd1);

	pid = Exec("inherit-child");
	Close(fd);
	Join(pid);
}
