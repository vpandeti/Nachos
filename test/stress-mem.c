#include "syscall.h"


int main()
{
  char* bogusptr = (char*) -5;
  int x;

  x = (int) *bogusptr;
}
