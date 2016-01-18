CSE 306, Eugene W. Stark
HW3 (Homework 3)
Student name: Venkateswara Prasad, Pandeti, Id: 110396994
===============================================================================================================================================

1. Round robin scheduling - changed quantum from 100 to 1000:
-------------------------------------------------------------
Implementation: 
===============
By default, Nachos provides Round robin scheduling. To increase the quantum from 100 to 1000, I am maintaining a variable 'quantum' which is increased by 100 in every 'handleInterruput. If 'quantum' is less than 1000, handleInterrupt() returns without yielding the CPU for other threads that are in the ready queue. That way, I increased the quantum to '1000' ticks.

Test: 
=====
i. You can use the following run configuration to test "% ./run -ps -d m  -ncpu 2 -x test/sleep"
ii. Go to Options.java, change SMP_TEST from false to true, and Go to SMPTest.java, change the number of times the for-loop loops to 2000.
iii. When you run the application, you will find the threads SMTP0, SMTP1, .... , SMTP7 running more than 1000 ticks in a round robin fashion

===============================================================================================================================================

2. Multilevel feedback scheduling:
----------------------------------

Implementation:
===============
i. Maintaining five queues readyList1, readyList2, readyList3, readyList4 and readyList5 with 100, 200, 400, 800 and 1600 quantums respectively.
ii. Whenever any new thread comes, it will be added to readyList1.

Test:
=====
i. Use the following configuration "% ./run -ps -d m -mlfs 100 -ncpu 1 -x test/write1"
   "mlfs" is for enabling Multilevel Feedback Scheduling"
   100 is for quantum, which is configurable
ii. Go to Options.java, change SMP_TEST from "false" to "true".
iii. You can observer how the threads are running

I changed the number of times for-loop loops to observe the functionality.
   
===============================================================================================================================================

3. Sleep system call
--------------------

Implementation:
===============
i. Added #define SC_Sleep	12 in syscall.h
ii. Sleep:
	addiu $2,$0,SC_Sleep
	syscall
	j	$31
	.end Sleep
	
	.globl Sleep
	.ent	Sleep
	
	Added the above code in start.s file
iii. Added sleep.c to test
iv. Added SC_Sleep in handleException() in ExceptionHandler.java.
v. In Scheduler, please see sleep() method. I used a semaphore to block the thread.
vi. I am maintaining a Hashmap with key as the current CPU and the sleeping thread as value
vii. I am using two lists. one for sleeping threads and the other for the associated sleeping time.
viii. In handleInterrupt(), I use Hashmap, to update the time elapsed for each thread. If any of the thread's sleep time is elpased, I remove it from the list and release the semaphore, which wakes the sleeping thread by keeping that thread in the ready list.
ix. It works for the multiple CPUs

Test:
=====
i. You can use the following run configuration to test "% ./run -ps -d m  -ncpu 2 -x test/sleep"
ii. Please see the below console messages in eclipse: 

24	CPU0	system	on	[+] Thread ProgTest0(test/sleep) went to sleep  of 10000000 ticks
111100002	CPU0	system	on	[+] Thread ProgTest0(test/sleep) Waken up from the sleep  of 10000000 ticks

===============================================================================================================================================

4. Console driver
-----------------
Implementation:
===============
a. getChar():
+++++++++++++
i. I am using two buffers one for storing all the typed characters and another for storing just one line (to handle CTRL+U and CTRL+R)
ii. Defined two lists to handle new line ('\n','\r') and backspace (BKSPC, SPC, BKSPC)
	new char[]{'\n','\r'};    
    new char[]{'\b',' ','\b'};
iii. I am using charAvail semaphore to block the thread until user presses enter (new line)
iv. When user presses enter, I am sending back the characters one by one in getChar().
v. Only valid characters (ASCII 32 to 126) and new line (ASCII 10 and 13) are stored in the buffer.
vi. I am using boolean variables for CTRL+U, CTRL+R and BKSPC operations which are being used in handleInterrupt() in OutputHandler.
vii. For, CTRL+U, I am doing BKSPC operation till the beginning of the current line using second buffer.
viii. For, CTRL+R, I am doing BKSPC operation till the beginning of the current line using second buffer and writing the characters from second buffer.

Please see the methods getChar() in syscall.java and getchar(), handleChars(), handleInterrupt() (InputHandler) and handleInterrupt() in OutputHandler, in ConsoleDriver.java

Please see read1.c

b. putChar():
+++++++++++++
i. I am using outputBuffer of size 10. When it is full, semaphore blocks further insertion of data into the buffer.
ii. Prints the characters from the outputBuffer till the number of characters written from the buffer decreases below 10.

Please see the methods putChar() in syscall.java and putChar() and handleInterrupt() in OutputHandler, in ConsoleDriver.java

Please see write1.c

Test:
=====
a. getChar():
+++++++++++++
i. Go to Options.java, change SMP_TEST from "true" to "false"
ii. Use the following configuration "% ./run -ps -d m -x test/read1"
iii. Type 'abcdefghijklmnopqrstuvwxyz' (26 chars), I am allowing only 25 characters to be read in read1.c, You will see the chars from a to y (25 chars).

You can aslo type random chars, play with the input using CTRL+U and CTRL+R and BKSPC.
When you press enter, you will see the following messages in eclipse console:

33275969	CPU0	system	on	[+] Typed character is: a
33275971	CPU0	system	on	[+] Typed character is: b
33275973	CPU0	system	on	[+] Typed character is: c
33275975	CPU0	system	on	[+] Typed character is: d
33275977	CPU0	system	on	[+] Typed character is: e
33275979	CPU0	system	on	[+] Typed character is: f
33275981	CPU0	system	on	[+] Typed character is: j
33275983	CPU0	system	on	[+] Typed character is: h
33275985	CPU0	system	on	[+] Typed character is: i
33275987	CPU0	system	on	[+] Typed character is: j
33275989	CPU0	system	on	[+] Typed character is: k
33275991	CPU0	system	on	[+] Typed character is: l
33275993	CPU0	system	on	[+] Typed character is: m
33275995	CPU0	system	on	[+] Typed character is: n
33275997	CPU0	system	on	[+] Typed character is: o
33275999	CPU0	system	on	[+] Typed character is: p
33276001	CPU0	system	on	[+] Typed character is: q
33276003	CPU0	system	on	[+] Typed character is: r
33276005	CPU0	system	on	[+] Typed character is: s
33276007	CPU0	system	on	[+] Typed character is: t
33276009	CPU0	system	on	[+] Typed character is: u
33276011	CPU0	system	on	[+] Typed character is: v
33276013	CPU0	system	on	[+] Typed character is: w
33276015	CPU0	system	on	[+] Typed character is: x
33276017	CPU0	system	on	[+] Typed character is: y


b. putChar():
+++++++++++++
i. Go to Options.java, change SMP_TEST from "true" to "false"
ii. Use the following configuration "% ./run -ps -d m -x test/write1"
iii. You will see the following output "A bufA buffer."