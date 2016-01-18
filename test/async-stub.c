#include "syscall.h"


/* this program checks the asychronous stub filesystem */

/* use about 4 pages of physical memory */

main()
{
	int c1, c2;
	
	Write("Starting\n", 9, 1);
	c1 = Exec("cs2");
	c2 = Exec("cs3");
	Join(c1);
	Join(c2);
	Write("Done\n", 5, 1);
	Exit(0);
}
