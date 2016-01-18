/* Basic test of Exec() system call */

#include "syscall.h"

int
main()
{
  Exec("test//halt2");
  Exit(0);
}
