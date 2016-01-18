#define MEMSIZE 24
#define PAGESIZE 128
#define ARRAYSIZE (((MEMSIZE+1)*PAGESIZE)/4)
#define OUT "out"

int A[ARRAYSIZE];

main()
{
  int i;
  int sum;
  int fp;

  sum = 0;
  for(i=0;i<ARRAYSIZE;i++)
    A[i] = i;
  Create(OUT);
  fp = Open(OUT);
  Write(A, ARRAYSIZE*sizeof(int), fp);
  Close(fp);

  for(i=0;i<ARRAYSIZE;i++)
    A[i] = 0;

  fp = Open(OUT);
  Read(A, ARRAYSIZE*sizeof(int), fp);
  Close(fp);
  
  for(i=0;i<ARRAYSIZE;i++)
    sum += A[i];

  if(sum==((ARRAYSIZE/2)*(ARRAYSIZE-1))) {
    Write("OK\n", 3, 1);
  } else {
    Write("Broken\n", 7, 1);
  }
  Exit(0);
}
