/* Stress test on Exec()
 * Keeps on forking processes until the space fills up
 */

#include "syscall.h"

int main()
{
  SpaceId child;
  Write("here\n", 5, 1);
  child = Exec("stress-exec");
  Exit(0);
}
