/* matmult.c 
 *    Test program to do matrix multiplication on large arrays.
 *
 *    Intended to stress virtual memory system.
 *
 *    Ideally, we could read the matrices off of the file system,
 *	and store the result back to the file system!
 */

#include "syscall.h"

#define Dim 	20	/* sum total of the arrays doesn't fit in 
			 * physical memory 
			 */

int A[Dim][Dim];
int B[Dim][Dim];
int C[Dim][Dim];

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

int
main()
{
    int i, j, k;
    char ans[10];
    int len;

    for (i = 0; i < Dim; i++)		/* first initialize the matrices */
	for (j = 0; j < Dim; j++) {
	     A[i][j] = i;
	     B[i][j] = j;
	     C[i][j] = 0;
	}

    for (i = 0; i < Dim; i++)		/* then multiply them together */
	for (j = 0; j < Dim; j++)
            for (k = 0; k < Dim; k++)
		 C[i][j] += A[i][k] * B[k][j];

    len = int2str(C[Dim-1][Dim-1],ans);
    Write(ans,len,ConsoleOutput);		/* and then we're done */
    Write("\n",1,ConsoleOutput);
}
