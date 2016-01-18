// SerialTest.java
//
//	Test program to demonstrate the operation of serial ports.
//
// Copyright (c) 2010 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.devices.test;

import nachos.Debug;
import nachos.machine.NachosThread;
import nachos.kernel.Nachos;
import nachos.kernel.devices.SerialDriver;

/**
 * Loopback test for serial port.
 * Creates a sender thread and a receiver thread.
 * Sender sends several bytes of data, receiver receives them.
 *
 * @author Eugene W. Stark
 */
public class SerialTest {

    /** Reference to the serial device driver. */
    private static SerialDriver driver = Nachos.serialDriver;
    
    /** Array of bytes to be transmitted. */
    private static byte[] data = new byte[] { 'H', 'E', 'L', 'L', 'O', 0x0 };

    /**
     * Entry point for the serial test.
     * 
     * The serial test sets up a loopback from output to input on the first
     * serial port, and creates a sender and a receiver thread.
     * The sender outputs several test bytes on the output side of the port
     * and the receiver receives those bytes on the input side.
     * If everything is OK, the receiver should receive the same bytes as
     * the sender sent!
     */
    public static void start() {
	Debug.println('+', "Entering SerialTest");
	Debug.ASSERT(driver != null);
	driver.openPort(0);

	NachosThread sender =
	    new NachosThread
	    ("Sender thread",
	     new Runnable() {
		    public void run() {
			Debug.print('+', "Sender starting\n");
			for(int i = 0; i < data.length; i++) {
			    byte b = data[i];
			    driver.putByte(0, b);
			    Debug.printf('+', "Sender: sent 0x%x ('%c')\n",
					 b, b);
			}
			Debug.print('+', "Sender terminating\n");
			Nachos.scheduler.finishThread();
		    }
		});

	NachosThread receiver =
	    new NachosThread
	    ("Receiver thread",
	     new Runnable() {
		    public void run() {
			Debug.print('+', "Receiver starting\n");
			byte b = 0;
			do {
			    b = driver.getByte(0);
			    Debug.printf('+', "Receiver: got 0x%x ('%c')\n",
					 b, b);
			} while(b != 0);
			driver.closePort(0);
			Debug.printf('+', "Receiver terminating\n", b);
			Nachos.scheduler.finishThread();
		    }
		});

	Nachos.scheduler.readyToRun(sender);
	Nachos.scheduler.readyToRun(receiver);
    }
}
