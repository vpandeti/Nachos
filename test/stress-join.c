#include "syscall.h"

int main()
{
  SpaceId bogus = (SpaceId) 12345678;

  Join(bogus);
}
