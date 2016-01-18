#include "syscall.h"

int main()
{
  OpenFileId fd;

  fd = Open("create-test");
  Close(fd);
}
