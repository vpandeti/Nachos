#include "syscall.h"

int main()
{
  int arr[16];
  
  Write("Swap release stress test 3\n",27,ConsoleOutput);
  Exec("stress-swaprelease1");
  arr[0]++;
  Write("Test 3 exiting\n",15, ConsoleOutput);
  Exit(0);
}
