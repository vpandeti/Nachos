package nachos.kernel.threads.test;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.machine.CPU;
import nachos.machine.NachosThread;

/**
 * This class tests/demonstrates the execution of threads on multiple CPUs.
 * It creates a number of kernel threads that each execute a loop that
 * "wastes time" by repeatedly enabling and disabling interrupts.
 * With CPU scheduling timers enabled, the threads will be pre-empt each
 * other and bounce around from one CPU to another.
 * 
 * @author Eugene W. Stark
 * @version 20140114
 */

public class SMPTest {
    /**
     * Entry point for the test.
     */
    public static void start() {
	Debug.println('+', "Entering SMPTest");
	for(int i = 0; i < 8; i++) {
	    NachosThread looper =
		    new NachosThread
		    ("SMP" + i,
		     new Runnable() {
			public void run() {
			    for(int i = 0; i < 2000; i++) {
				Debug.println('+', NachosThread.currentThread().name);
				int oldLevel = CPU.setLevel(CPU.IntOff);
				CPU.setLevel(oldLevel);
			    }
			    Nachos.scheduler.finishThread();
			}
		     });
	    Nachos.scheduler.readyToRun(looper);
	}
    }
}
