#include "syscall.h"

int main()
{
  Read((char*)-1024, 5, ConsoleInput);
  Write("Done\n", 5 , ConsoleOutput);
}

