#include "syscall.h"



int main()
{
  OpenFileId fd;
  char *buf = "A buffer.";

  fd = Open("create-test");
  Write(buf, 5, ConsoleOutput);
  Write(buf, 9, ConsoleOutput);
  Close(fd);
}

