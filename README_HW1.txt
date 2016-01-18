README_HW1

Steps to run my code:
=====================
1. Add arguments "-d ti1 -rs 1000" in run configuration.
2. If you want to test Cyclic Barrier, please comment, "TaskManager.demo(new String[]{"5","10"});" in Nachos.java line #119
3. If you want to test Task Manager, please comment, "CyclicBarrier.demo(new String[]{"5","5"});" in Nachos.java line #118 
4. Please Do not pass invalid args in demo method.

I have added java comments in both the files. Comments describes what my code does.

Cyclic Barrier:
===============
1.I have used two semaphores for the synchronization purpose one with 1 permit and the other with 0 permits.
2. Each thread waits at the barriers until the last thread arrives at the barrier and breaks it.

One problem I found while implementing Cyclic Barrier is using "-x test/halt" argument in Run Configuration.
When the barrier is broken/tripped, I called V() on the semaphore in a loop for all the worker threads that are waiting at the barrier.
Worker threads' status changed to "READY" from "BLOCKED", later worker threads didn't run (Status didn't change from "READY" to "RUNNING").
When I removed "-x test/halt" argument in Run Configuration, then status of the worker threads changed to "RUNNING" from "READY".

So please test my code with arguments "-d ti1 -rs 1000" in run configuration

Task Manager:
=============
1. I have used Semaphore for synchronization purpose. It is used for making the Main Thread wait till all the responses have been added to the list that can be accessed by Main Thread.
2. When worker threads complete "doInBackground" activity, requests to call "onCompletion" or "onCancellation" have been added to a list, which will be accessed by the Main thread that call those methods on behalf of worker threads.

One problem, I found in implementing Task Manager is "NullPointerException" which is invoked from CPU.setLevel(). It has already been communicated to Professor E.Stark. I fixed this problem by keeping "Nachos.scheduler.finishThread();" in finally block.