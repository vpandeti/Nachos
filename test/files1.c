/* Stress test of file system system calls.  Assumes that console
 * writes work.
 */


#include "syscall.h"
#define ALPHA "abcdefghijklmnopqrstuvwxyz9876543210"
#define LONG ALPHA ALPHA ALPHA ALPHA ALPHA ALPHA ALPHA 


int main()
{
  OpenFileId fds[3];
  int nums[3];
  char readbuf[300];

  /* See if it can handle an argument that crosses a page boundary */
  Create(LONG);

  Create("file-test1");
  Create("file-test2");
  Create("file-test3");

  fds[0] = Open("file-test1");
  fds[1] = Open("file-test2");
  fds[2] = Open("file-test3");


  Write(LONG,36*7,fds[1]);	/* Write from across page */
  Write("ABCDEFGHIJKLMNOPQRSTUVW",23,fds[2]);
  Write("123456789",9,fds[0]);

  Close(fds[0]);
  Close(fds[1]);
  Close(fds[2]);
  fds[0] = Open("file-test1");
  fds[1] = Open("file-test2");
  fds[2] = Open("file-test3");

  nums[0] = Read(&readbuf[0],9,fds[0]);
  nums[1] = Read(&readbuf[9],36*7,fds[1]); /* Read across page */
  nums[2] = Read(&readbuf[80],23,fds[2]); /* Overlapping read */

  Write(readbuf,150,ConsoleOutput);

  Close(fds[0]);
  Close(fds[1]);
  /* Leave fds[2] open */

  Exit(0);
}
