#include  "syscall.h"

int main()
{
  int array[1024];
  SpaceId s1;
  int i;
  
  Write("Testing large programs.\n",24,ConsoleOutput);
  s1 = Exec("vm2");
  for (i=0;i< 128;i++);
  Join(s1);
  Write("Testing done.\n",14,ConsoleOutput);
  Exit(0);
}
