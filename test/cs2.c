#define MEMSIZE 24
#define PAGESIZE 128
#define ARRAYSIZE (((MEMSIZE+1)*PAGESIZE)/4)

int A[ARRAYSIZE];

main()
{
  int i;
  int sum;

  sum = 0;
  for(i=0;i<ARRAYSIZE;i++)
    A[i] = i;

  for(i=0;i<ARRAYSIZE;i++)
    sum += A[i];

  if(sum==((ARRAYSIZE/2)*(ARRAYSIZE-1))) {
    Write("OK\n", 3, 1);
  } else {
    Write("Broken\n", 7, 1);
  }
  Exit(0);
}
