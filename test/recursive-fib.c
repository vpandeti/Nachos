#include "syscall.h"

/*
 * recursive fibonacci
 */

void reverse(str,len)
        char *str;
        int len;
{
        int i,half;
        char c;
        half = (len / 2) + (len % 2) - 1;
        for (i = 0; i <= half; i++) {
                c = str[i];
                str[i] = str[len-i-1];
                str[len-i-1] = c;
        }
}

int int2str(n,str)
int n;
char *str;
{
        int p = 0;
        do {
                str[p++] = '0' + (n % 10);
                n = n / 10;
        } while (n != 0);
        reverse(str,p);
        return p;
}

int fib(int n)
{
  if (n > 1)
    return(fib(n-2) + fib(n-1));
  else
    return(n);
}

int main(void)
{
  int fib20,len;
  char ans[10];
  fib20 = fib(20);
  len = int2str(fib20,ans);
  Write(ans,len,ConsoleOutput);
  Write("\n",1,ConsoleOutput);
}
