// ConsoleTest.java
//
//	Class for testing the Console hardware device.
//
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.devices.test;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.kernel.devices.ConsoleDriver;
import nachos.machine.NachosThread;

/**
 * Class for testing the Console hardware device.
 * 
 * @author Peter Druschel (Rice University)
 * @author Eugene W. Stark (Stony Brook University)
 */
public class ConsoleTest implements Runnable {

    /** Reference to the console device driver. */
    private ConsoleDriver console;

    /**
     * Test the console by echoing characters typed at the input onto
     * the output.  Stop when the user types a 'q'.
     */
    public void run() {
	Debug.println('+', "ConsoleTest: starting");
	Debug.ASSERT(Nachos.consoleDriver != null,
			"There is no console device to test!");

	console = Nachos.consoleDriver;
	while (true) {
	    char ch = console.getChar();
	    console.putChar(ch);	// echo it!

	    if(ch == '\n')
		console.putChar('\r');

	    if (ch == 'q') {
		Debug.println('+', "ConsoleTest: quitting");
		console.stop();
		Nachos.scheduler.finishThread();    // if q, quit
	    }
	}
    }

    /**
     * Entry point for the Console test.  If "-c" is included in the
     * command-line arguments, then run the console test; otherwise, do
     * nothing.
     *
     * The console test reads characters from the input and echoes them
     * onto the output.  The test ends when a 'q' is read.
     */
    public static void start() {
	NachosThread thread = new NachosThread("Console test", new ConsoleTest());
	Nachos.scheduler.readyToRun(thread);
    }
}


