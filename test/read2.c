#include "syscall.h"

int main()
{
  OpenFileId fd;
  char buf[25];
  int num;

  fd = Open("create-test");
  num = Read(buf, 25, fd);
  Write(buf, num, 1);
  Close(fd);
}
