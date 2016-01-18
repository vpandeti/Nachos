#include "syscall.h"

int main()
{
  int arr[40];
  
  Write("Swap release stress test 2\n",27,ConsoleOutput);
  Exec("stress-swaprelease3");
  arr[0]++;
  Write("Test 2 exiting\n",15, ConsoleOutput);
  Exit(0);
}
