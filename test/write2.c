#include "syscall.h"
int main()
{
   char buf[] = {'a'};

   Write(&buf[0],1,ConsoleOutput);
}
