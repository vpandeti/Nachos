#include "syscall.h"

int main()

{
  int big[4096];
  SpaceId s2;
  int i;

  Write("Big.\n",5,ConsoleOutput);
  s2 = Exec("vm3");
  for (i=0; i < 64; i++);
  Join(s2);
  Exit(0);
}
