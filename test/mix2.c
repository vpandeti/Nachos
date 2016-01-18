
#define MEMSIZE 24
#define PAGESIZE 128
#define ARRAYSIZE (MEMSIZE*PAGESIZE/4)

int A[ARRAYSIZE];

main()
{
  int i=0;
  while(1) {
    if(i%100 == 0)
      Write("2: here\n", 8, 1);
    A[i%(PAGESIZE/4)] = i*2;
	i++;
  }
}
