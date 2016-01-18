/* Basic test of console reads and writes */

#include "syscall.h"
#define INPUTSIZE 20

int main()
{
  char buffer[INPUTSIZE];
  int num;

  Write("Give me some input: \n", 21, ConsoleOutput);
  num = Read(buffer, INPUTSIZE, ConsoleInput);
  Write("You said: \n", 11, ConsoleOutput);
  Write(buffer, num , ConsoleOutput);
}

