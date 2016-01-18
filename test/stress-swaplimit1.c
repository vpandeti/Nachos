#include "syscall.h"

int main()
{
  int arr[512];
  SpaceId proc;

  Write("Filling up swap space, again.\n",30,ConsoleOutput);
  proc = Exec("test//stress-swaplimit1");
  arr[0]++; /* arr[500]++ */
  Join(proc);
  Write("Exiting now.\n",13,ConsoleOutput);
  Exit(0);
}

