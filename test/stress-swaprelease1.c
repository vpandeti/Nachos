#include "syscall.h"

int main()
{
  int arr[22];
  
  Write("Swap release stress test 1\n",27,ConsoleOutput);
  Exec("stress-swaprelease2");
  arr[0]++;
  Write("Test 1 exiting\n",15, ConsoleOutput);
  Exit(0);
}
