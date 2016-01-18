#include "syscall.h"

/* allocate a large chunk of uninitialized data and randomly poke around */

/* generate a pseudo-random number */
int rand(void)
{
  static unsigned long next = 1;
  next = next * 1103515245 + 12345;
  return (unsigned int)(next/65535) % 32768;
}

char data[32768];

int main()
{
  int i;
  char c;
  for(i=0;(i<1000);i++) {
    data[rand()]=1;
    c = (char)((i%26) + 'a');
    Write(&c,1,ConsoleOutput);
  }
  Write("\ndone\n", 6, ConsoleOutput);
}
