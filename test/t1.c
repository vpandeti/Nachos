#include "syscall.h"

int main()
{
  int i, j;
  
  Exec("timeshare2");
  Exec("timeshare3");  

  for(i=0;i<10;i++) {
	for (j=0; j<100; j++);
    Write("Timesharing 1\n",14,ConsoleOutput);
  }
}
