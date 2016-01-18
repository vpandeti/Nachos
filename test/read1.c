#include "syscall.h"

int main()
{
  OpenFileId fd;
  char buf[25];
  int num;

  fd = Open("create-test");
  num = Read(buf, 25, ConsoleInput);
  /*Write(buf,25,ConsoleOutput);*/
  Close(fd);
  Halt();
}
