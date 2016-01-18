1.a. Extend the file beyond the end of the file until the maximum size of the file.
Implementation:
===============
I made changes in OpenFileReal.java -> writeAt() method and FileHeader.java -> beyondFileExtension() method (newly created)

There are three cases to handle in writeAt() method:

First one -> If the number of bytes to be written is less than the file size
Second one -> If the number of bytes to be written partially inside the file and partially more than the file size
Third one -> If the number of bytes is more than or equal to the file size

I handled these three cases in writeAt() method in OpenFileReal.java

In the last two cases, I am extending the file by creating new datasectors in beyondFileExtension method in FileHeader.java

Test:
=====
Use the configuration: "% ./run -f -d f m -cp test/mkdir2 mkdir2 -x mkdir2"

Added a file in the workspace "test/test.txt" This contains 3712 characters which mean 3712 Bytes. Created a file with 128 bytes and reading the test file in blocks of 128 bytes and writing the read bytes into nachos file. Please see the below console messages

test.txt starts with "Lorem ipsum dolor sit amet, consectetuer.....". Please see this file.

Prerequisites: Create a directory like "/root" and then create a file in it "/root/test1.txt" and then read the file that is added to the workspace in "test/test.txt"


Writing 128 bytes at 0, from file of length 180.
Writing 128 bytes at 128, from file of length 180.
Writing 128 bytes at 256, from file of length 256.
Writing 128 bytes at 384, from file of length 384.
Writing 128 bytes at 512, from file of length 512.
Writing 128 bytes at 640, from file of length 640.
Writing 128 bytes at 768, from file of length 768.
Writing 128 bytes at 896, from file of length 896.
Writing 128 bytes at 1024, from file of length 1024.
Writing 128 bytes at 1152, from file of length 1152.
Writing 128 bytes at 1280, from file of length 1280.
Writing 128 bytes at 1408, from file of length 1408.
Writing 128 bytes at 1536, from file of length 1536.
Writing 128 bytes at 1664, from file of length 1664.
Writing 128 bytes at 1792, from file of length 1792.
Writing 128 bytes at 1920, from file of length 1920.
Writing 128 bytes at 2048, from file of length 2048.
Writing 128 bytes at 2176, from file of length 2176.
Writing 128 bytes at 2304, from file of length 2304.
Writing 128 bytes at 2432, from file of length 2432.
Writing 128 bytes at 2560, from file of length 2560.
Writing 128 bytes at 2688, from file of length 2688.
Writing 128 bytes at 2816, from file of length 2816.
Writing 128 bytes at 2944, from file of length 2944.
Writing 128 bytes at 3072, from file of length 3072.
Writing 128 bytes at 3200, from file of length 3200.
Writing 128 bytes at 3328, from file of length 3328.
Writing 128 bytes at 3456, from file of length 3456.
Writing 128 bytes at 3584, from file of length 3584.

+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

1.b. Extending the directory size beyond 10:
Implementation:
===============
Added method "extendTheDirectoryCapacity()" in Directory.java which extends the size of the directoryEntry table by one.
Writes back the extended table to the memory

Test:
=====
Use the configuration: "% ./run -f -d f m -cp test/mkdir1 mkdir1 -x mkdir1"
Create 11 files in one directory
Observe the logs
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

2.a. mkdir system call:
Implementation:
===============
Added system call and added "mkdir.c" user program

First step: Parses the directory path. For example "/root/test/" into "[root,test]
Second step: Checks if directory "root" exists using "directoryFile" in FileSystemReal.java. If it is available, File is fetched from the memory and checks if directory "test" exists. If not, Creates the directory "test" using createEntity() in FileSystemReal.java.

Test:
=====
Use the configuration "% ./run -f -d f m -cp test/mkdir mkdir -x mkdir"
Please observe the message in console "Succesfully created directory /root"
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

2.b. rmdir system call:
Implementation:
===============
Added system call and added "rmdir.c" user program

First step: Parses the directory path. For example "/root/test/" into "[root,test]
Second step: Checks if directory "root" exists using "directoryFile" in FileSystemReal.java. If it is available, File is fetched from the memory and checks if directory "test" exists. If yes, Removes the directory "test" using removeEntity() in FileSystemReal.java.

Test:
=====
You can use "mkdir.c" itself. Because in addition to mkdir system call, rmdir system call is also added in the same file
"% ./run -f -d f m -cp test/mkdir mkdir -x mkdir"
or
Use the configuration "% ./run -d f m -cp test/rmdir rmdir -x rmdir"

Please observer "Successfully deleted" message in the console.
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

2.c. list:
Implementation:
===============
step 1: Takes the root directory.
step 2: Print if there are any contents inside.
step 3: Traverse each directory which is inside the root directory
step 4: Print if there are any contents inside.
step 5: Repeat 3 & 4 until there are no directories to visit

Please see the list() method in FileSystemReal.java

Test:
=====
Use the configuation "% ./run -f -d f m -cp test/mkdir mkdir -x mkdir"
Please observe the console that says "Listing the contents of the filesystem"


+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

3. Long Files:

Haven't implemented fully due to time lag, though wrote some code.
check IndirectBlock.java and FileHeader.java (beyondFileExtension() method)

+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

4. Test your implementation
Implementation:
===============

step 1: Creates a thread in FileSystemReal.java upon start() method invocation
step 2: Tests Directory creation, Lists the directory contents, checks the inconsistencies and Removes a directory

Please see start() and consistencyCheck() methods in FileSystemReal.java

Test:
=====
Use the configuration "% ./run -f -t -d f m -cp test/mkdir mkdir -x mkdir"

IMPORTANT: "-t" must be passed to test the inconsistencies.

Observe the print messages in console that start with "Consistency checks"


Please see below observed messages in console:



1862503	CPU0	system	on	[f] Consistency test: Successfully created directory /root
1862503	CPU0	system	on	[f] Consistency test: listing the directory contents
1862503	CPU0	system	on	[f] Listing the contents of the filesystem
Reading 180 bytes at 0, from file of length 180.
mkdir
root
1890503	CPU0	system	on	[f] /root
Reading 180 bytes at 0, from file of length 180.
Reading 180 bytes at 0, from file of length 180.
Reading 180 bytes at 0, from file of length 180.
mkdir
root
2002503	CPU0	system	on	[f] /root
2021503	CPU0	system	on	[+] Bit map file header:
FileHeader contents.  File size: 128.,  File blocks: 2 
File contents:
\0\0\1f\ff\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0
2033503	CPU0	system	on	[+] Directory file header:
FileHeader contents.  File size: 180.,  File blocks: 11 12 
File contents:
\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0
\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0
Reading 128 bytes at 0, from file of length 128.
2081503	CPU0	system	on	[+] Bitmap set: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 
Reading 180 bytes at 0, from file of length 180.
Directory contents: Name mkdir, Sector: 5
FileHeader contents.  File size: 392.,  File blocks: 6 7 8 9 
File contents:
\ad\df\ba\0\0\0\0\0(\0\0\0`\1\0\0\0\0\0\0\88\1\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0D\0\0\c\f0\ff\bd'\8\0\0\c! \0\0\0\0\2$\c\0\0\0\8\0\e0\3\0\0\0\0\1\0\2$\c\0\0\0\8\0\e0\3\0\0\0\0\2\0\2$\c\0\0\0\8\0\e0\3\0\0\0\0\3\0\2$\c\0\0\0\8\0\e0\3\0\0\0\0\4\0\2$\c\0\0\0
\8\0\e0\3\0\0\0\0\b\0\2$\c\0\0\0\8\0\e0\3\0\0\0\0\5\0\2$\c\0\0\0\8\0\e0\3\0\0\0\0\6\0\2$\c\0\0\0\8\0\e0\3\0\0\0\0\7\0\2$\c\0\0\0\8\0\e0\3\0\0\0\0\c\0\2$\c\0\0\0\8\0\e0\3\0\0\0\0\d\0\2$\c\0\0\0\8\0\e0\3\0\0\0\0\e\0\2$\c\0\0\0\8\0\e0\3\0\0\0\0\8\0\2$\c\0\0\0
\8\0\e0\3\0\0\0\0\9\0\2$\c\0\0\0\8\0\e0\3\0\0\0\0\a\0\2$\c\0\0\0\8\0\e0\3\0\0\0\0\8\0\e0\3\0\0\0\0\0\0\0\0\0\0\0\0\e8\ff\bd'\14\0\bf\af\10\0\be\af@\0\0\c!\f0\a0\3\0\0\4<P\1\84$,\0\0\c\0\0\0\0\8\0\0\c! \0\0!\e8\c0\3\14\0\bf\8f\10\0\be\8f\8\0\e0\3\18\0\bd'/root\0\0\0
\0\0\0\0\0\0\0\0
Name root, Sector: 10
FileHeader contents.  File size: 180.,  File blocks: 11 12 
File contents:
\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0
\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0

Reading 180 bytes at 0, from file of length 180.
Reading 180 bytes at 0, from file of length 180.
2310503	CPU0	system	on	[f] Consistency test: Removing the directory /root