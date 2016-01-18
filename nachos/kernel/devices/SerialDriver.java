// SerialDriver.java
//
//	Demonstration driver for serial port devices.
//
// Copyright (c) 2010 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.devices;

import nachos.Debug;
import nachos.machine.Machine;
import nachos.machine.SerialPort;
import nachos.machine.InterruptHandler;
import nachos.kernel.threads.Lock;
import nachos.kernel.threads.Semaphore;

import java.util.Queue;
import java.util.LinkedList;
import java.net.SocketAddress;

/**
 * Demonstration driver for serial port devices.
 *
 * @author Eugene W. Stark
 */
public class SerialDriver {

    /** SerialPort device units. */
    private SerialPort[] units = new SerialPort[SerialPort.NUM_UNITS];
    
    /** Queue capacity. */
    private static final int QUEUE_CAPACITY = 2;
    
    /** SerialPort output queues. */
    @SuppressWarnings("unchecked")
    private final Queue<Byte>[] outqs = new Queue[SerialPort.NUM_UNITS];
    
    /** SerialPort input queues. */
    @SuppressWarnings("unchecked")
    private final Queue<Byte>[] inqs = new Queue[SerialPort.NUM_UNITS];
    
    /**
     * Lock that enables a single thread to obtain exclusive access
     * to driver state, among all threads on all CPUs.
     * Holding this lock does not exclude the possibility of
     * an interrupt service routine being active on some CPU.
     */
    private final Lock mutex;
    
    /** Input condition variables. */
    private final Semaphore[] dataAvail = new Semaphore[SerialPort.NUM_UNITS];
    
    /** Output condition variables. */
    private final Semaphore[] spaceAvail = new Semaphore[SerialPort.NUM_UNITS];
    
    /** Busy status of each unit. */
    private boolean[] busy = new boolean[SerialPort.NUM_UNITS];
    
    /**
     * Initialize the driver.
     */
    public SerialDriver() {
	mutex = new Lock("serial driver mutex");
	// Nothing else for now.  The units are initialized individually
	// on demand via openPort().
    }
    
    /**
     * Called to begin a critical section.
     */
    private void beginCS() {
	// Lock out other threads on this and other CPUs.
	mutex.acquire();
	
	// Spin waiting for the completion of any ISR that is
	// currently in progress, and then mask further interrupts
	// of this device type.
	while(!Machine.setInterruptMask(Machine.SerialInt))
	    ; // spin
    }
    
    /**
     * Called to end a critical section.
     */
    private void endCS() {
	// Clear the interrupt mask for devices of this type.
	Machine.clearInterruptMask(Machine.SerialInt);
	
	// Admit other threads.
	mutex.release();
    }

    /**
     * Shut down the driver and the underlying devices.
     */
    public void stop() {
	for(int i = 0; i < units.length; i++)
	    closePort(i);
    }
    
    /**
     * Open a specified serial port and ready it for use.
     * By default, the port is in loopback mode, in which the
     * transmit and receive sides are "wired together".
     *
     * @param i The unit number of the port to open.
     */
    public void openPort(int i) {
	SerialPort unit = SerialPort.getUnit(i);
	units[i] = unit;
	outqs[i] = new LinkedList<Byte>();
	inqs[i] = new LinkedList<Byte>();
	dataAvail[i] = new Semaphore("data available: serial unit " + i, 0);
	spaceAvail[i] = new Semaphore("space available: serial unit " + i,
					QUEUE_CAPACITY);
	
	beginCS();
	unit.setHandler(new SerialIntHandler(i));
	unit.writeMCR(SerialPort.MCR_DTR | SerialPort.MCR_RTS);
	Debug.print('p', "Serial port " + i + " open at address "
		    + unit.getLocalAddress() + "\n");
	endCS();
    }

    /**
     * Close a previously opened port.
     *
     * @param i The unit number of the port to close.
     */
    public void closePort(int i) {
	beginCS();
	SerialPort unit = units[i];
	if(unit != null) {
	    unit.writeMCR(0);
	    unit.setHandler(null);
	    unit.stop();    
	    units[i] = null;
	    outqs[i] = null;
	    inqs[i] = null;
	}
	endCS();
    }

    /**
     * Connect the wire attached to a specified unit number to a remote
     * port located at a specified address.
     * 
     * @param i The unit number of the port to be connected.
     * @param addr  The address of the remote port to connect to,
     * or null to set the specified unit into loopback mode.
     */
    public void connectPort(int i, SocketAddress addr) {
	units[i].connectTo(addr);
    }

    /**
     * Send a byte of data over a serial port.
     * The calling thread blocks until the port is ready
     * to accept the data to be transmitted.
     *
     * @param i The unit number of the port to use.
     * @param data The data byte to be transmitted.
     */
    public void putByte(int i, byte data) {
	// Ensure space in the output queue.
	spaceAvail[i].P();
	
	// Put byte in queue and start transmission,
	// if device is not already busy.
	beginCS();
	outqs[i].add(data);
	startXmit(i);
	endCS();
    }

    /**
     * Wait for a byte of data to be received over the serial port
     * and return it.
     *
     * @param i The unit number of the port to use.
     * @return  The byte of data received.
     */
    public byte getByte(int i) {
	// Wait for data to arrive.
	dataAvail[i].P();
	
	// Dequeue a byte of data and return it.
	beginCS();
	byte data = inqs[i].poll();
	endCS();
	return data;
    }

    /**
     * Start transmission of the next outgoing byte.
     * Call only from within a critical section.
     * 
     * @param i
     */
    private void startXmit(int i) {
	if(!busy[i]) {
	    Byte data = outqs[i].poll();
	    if(data != null) {
		busy[i] = true;
		units[i].writeTDR(data);
		spaceAvail[i].V();
	    }
	}
    }
    
    /**
     * SerialDriver interrupt handler class (sample: not currently used).
     */
    private class SerialIntHandler implements InterruptHandler {

	private int index;

	public SerialIntHandler(int i) {
	    this.index = i;
	}

	public void handleInterrupt() {
	    Debug.println('p', "Serial port interrupt: unit #" + index);
	    SerialPort unit = units[index];
	    unit.showState();
	    if((unit.readLSR() & SerialPort.LSR_TRDY) != 0) {
		busy[index] = false;
		startXmit(index);
	    }
	    if((unit.readLSR() & SerialPort.LSR_RRDY) != 0) {
		byte data = unit.readRDR();
		inqs[index].add(data);
		dataAvail[index].V();
	    }
	}
    }
}
