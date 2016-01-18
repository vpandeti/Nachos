#include "syscall.h"

int main()
{
  SpaceId waitpid;
  int result;

  waitpid =  Exec("test\\timeshare2");
  result = Join(waitpid);
  waitpid = Exec("test\\timeshare3");
  result = Join(waitpid);
  waitpid = Exec("test\\timeshare4");
  result = Join(waitpid);
  /*Exit(result);*/
  Halt();
}