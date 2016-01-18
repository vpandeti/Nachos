CSE 373 - Operating Systems
Professor: Eugene Stark
===========================

IMP Notes:
----------
1. Most of my results are shown in Console window. Please put a breakpoint in Exit() system call and see the output in the console window.
2. There is a bug in File system implementation while creating a file with less than 257 bytes.
   You can add your own files for testing, Please make sure that it should not be less than 257 bytes.
   
Other than the file system issue which is not current assignment, HW5 assignment works as expected. 

Implementation:
---------------

1. Added system calls "void *Mmap(char *name, int *sizep)" and "int Munmap(void *addr);"
2. Added "test.txt" file in "test" folder
3. I used "public boolean FILESYS_REAL = true;"
4. Used the configuration "% ./run -f -d f -cp test/mmap ex -cp test/test.txt test -cp test/test1.txt test1 -x ex"
5. Copied the "test.txt" and "test1.txt" files into the file system
6. "void *address = (void *) Mmap("test", &size)". "test" is the file that is copied into the file system. 
7. When "Mmap" system call is invoked, 
	- OpenFile is opened and reference to it is maintained in the address space of the current process. 
	- Address space is extend above the Stack by the file size. For example, "test.txt" file size is 1024 bytes. So, 1024/128(Machine.pageSize)=8pages are added to the page table. 
	  The extended pages have properties valid = false, dirty = false, readOnly = false, physicalPage = -1, use = false
	- The address from which the address space is extended is returned to the user program.
	- Mapping is maintained between starting address of the extended address space and extended pages.
	- Number of extended pages is updated in the "sizep" pointer.
	- If sizep contains 0 after this system call, which means, memory is not extended, No further operations are carried out (such as accessing a page that may lead to page fault, etc.).
8. "char *a = (char *) address;
    char x = a[0];"
	
	- After the "Mmap" system call, If the any of the location in the extended memory is accessed, Page fault occurs.
	- Added PageFault exception catching code in "handleException()" method in "ExceptionHandler" class.
	- Virtual address of the page in which page fault occurred is collected from the CPU register "39".
	- Allocated a physical page for faulted page, and write 128 bytes of data from "test.txt" and modify its properties 
	  dirty = false, readOnly = true, valid = true and use = true.
	- When any write request accesses any mapped page, "ReadOnlyExcpetion" is thrown in "ExceptionHandler" class.
	  Address, where write access is requested, is collected from CPU register "39".
	  Modified the properties of the corresponding mapped page; readOnly = false and dirty = true;
	  
9. When "Munmap" system call is invoked,
	- Got the pages that have been added to the page table from page list mapping data structure.
	- Reset all the pages; valid = false, dirty = false, readOnly = true, use = false, physicalPage = -1.
	- Reset the extended memory in Machine.mainMemory to 0
	- Written back the modified data of the file into disk.
10. When the process exits, in exit() system call, modified data (from the dirty pages in main memory) of the file is written back into the disk.


How to test
-----------
1. Please see "mmap.c" user program.
2. Use the configuration "% ./run -f -d f -cp test/mmap ex -cp test/test.txt test -cp test/test1.txt test1 -x ex"

--> To check if "Mmap" is working: 
	
	I am displaying 10 characters of the files plus line break for each Page fault.
    
    "Lorem ipsu
    		    m dolor si
    		    		   t amet, co"
    		    		   
    This is the content from "test.txt".
	You can also see the console messages in Eclipse at which page fault occurred.

	13065516	CPU0	system	on	[+] Page fault occurred, Virtual address: 2560
	Reading 128 bytes at 0, from file of length 1023.
	13083449	CPU0	system	on	[+] Page fault occurred, Virtual address: 2698
	Reading 128 bytes at 0, from file of length 1023.
	13099450	CPU0	system	on	[+] Page fault occurred, Virtual address: 2836
	Reading 128 bytes at 0, from file of length 1023.
	
--> To check if write access is working:
	
	I am displaying the character that is written into a mapped page.
	
	"Lorem ipsu
    		    m dolor si
    		    		   t amet, co
    		    		   			  A" -----------> (address[0] = "A")
    		    		   			  
     It was "Lorem...". After write access, it is changed to "Aorem..." in "test.txt".
     
 --> To check if Munmap is working:
 	 
 	 In "Munmap", content from the dirty pages are written back into the disk.
 	 I am showing the content of the "test.txt" where the first letter 'L' is modified to 'A'.
 	 
 	 You can see the OpenFile content after "Munmap" system call in the console of the Eclipse.
 	 
 	 "Reading 1023 bytes at 0, from file of length 1023.
	  13261503	CPU0	system	on	[+] Aorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat a, tellus. Phasellus viverra nulla ut metus varius laoreet. Quisque rutrum. Aenean imperdiet. Etiam ultricies nisi vel augue. Curabitur ullamcorper ultricies nisi. Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero, sit amet adipiscing sem neque sed ipsum. Nam quam nunc, blandit v "
	  
	  Please observe "Aorem.."
	  
--> To check if the implementation is working for multiple files:

	I am displaying the contents of all files in console.
	
	"Lorem ipsu
    		    m dolor si
    		    		   t amet, co
    		    		   			  A
    		    		   			    Test1 Lore
    		    									m dolor si
									    		    		   t amet, co
									    		    		   			  A"
									    		    		   			  
	"Test1 Lore.." -> content of another file "test1.txt".

 --> To see if the data is written back into the disk when the process exits
 	 Please check "mmap1.c", in which there is no "Munmap" system call.
 	 
 	 Use the below configuration,
 	 "% ./run -f -d f -cp test/mmap1 ex -cp test/test.txt test -cp test/test1.txt test1 -x ex"
 	 
 	 Please see the below output in console window of the Eclipse.
 	 
 	 "Reading 1023 bytes at 0, from file of length 1023.
	  11451503	CPU0	system	on	[+] Aorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat a, tellus. Phasellus viverra nulla ut metus varius laoreet. Quisque rutrum. Aenean imperdiet. Etiam ultricies nisi vel augue. Curabitur ullamcorper ultricies nisi. Nam eget dui. Etiam rhoncus. Maecenas tempus, tellus eget condimentum rhoncus, sem quam semper libero, sit amet adipiscing sem neque sed ipsum. Nam quam nunc, blandit v "
 	 
	 Please observe "Aorem..." ('L' is modified to 'A'). 	 