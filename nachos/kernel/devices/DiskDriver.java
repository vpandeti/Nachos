// DiskDriver.java
//	Class for synchronous access of the disk.  The physical disk 
//	is an asynchronous device (disk requests return immediately, and
//	an interrupt happens later on).  This is a layer on top of
//	the disk providing a synchronous interface (requests wait until
//	the request completes).
//
//	Uses a semaphore to synchronize the interrupt handlers with the
//	pending requests.  And, because the physical disk can only
//	handle one operation at a time, uses a lock to enforce mutual
//	exclusion.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.devices;

import nachos.Debug;
import nachos.machine.Machine;
import nachos.machine.Disk;
import nachos.machine.InterruptHandler;
import nachos.kernel.threads.Semaphore;
import nachos.kernel.threads.Lock;


/**
 * This class defines a "synchronous" disk abstraction.
 * As with other I/O devices, the raw physical disk is an asynchronous
 * device -- requests to read or write portions of the disk return immediately,
 * and an interrupt occurs later to signal that the operation completed.
 * (Also, the physical characteristics of the disk device assume that
 * only one operation can be requested at a time).
 *
 * This driver provides the abstraction of "synchronous I/O":  any request
 * blocks the calling thread until the requested operation has finished.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class DiskDriver {

    /** Raw disk device. */
    private Disk disk;

    /** To synchronize requesting thread with the interrupt handler. */
    private Semaphore semaphore;

    /** Only one read/write request can be sent to the disk at a time. */
    private Lock lock;

    /**
     * Initialize the synchronous interface to the physical disk, in turn
     * initializing the physical disk.
     * 
     * @param unit  The disk unit to be handled by this driver.
     */
    public DiskDriver(int unit) {
	semaphore = new Semaphore("synch disk", 0);
	lock = new Lock("synch disk lock");
	disk = Machine.getDisk(unit);
	disk.setHandler(new DiskIntHandler());
    }

    /**
     * Get the total number of sectors on the disk.
     * 
     * @return the total number of sectors on the disk.
     */
    public int getNumSectors() {
	return disk.geometry.NumSectors;
    }

    /**
     * Get the sector size of the disk, in bytes.
     * 
     * @return the sector size of the disk, in bytes.
     */
    public int getSectorSize() {
	return disk.geometry.SectorSize;
    }

    /**
     * Read the contents of a disk sector into a buffer.  Return only
     *	after the data has been read.
     *
     * @param sectorNumber The disk sector to read.
     * @param data The buffer to hold the contents of the disk sector.
     * @param index Offset in the buffer at which to place the data.
     */
    public void readSector(int sectorNumber, byte[] data, int index) {
	Debug.ASSERT(0 <= sectorNumber && sectorNumber < getNumSectors());
	lock.acquire();			// only one disk I/O at a time
	disk.readRequest(sectorNumber, data, index);
	semaphore.P();			// wait for interrupt
	lock.release();
    }

    /**
     * Write the contents of a buffer into a disk sector.  Return only
     *	after the data has been written.
     *
     * @param sectorNumber The disk sector to be written.
     * @param data The new contents of the disk sector.
     * @param index Offset in the buffer from which to get the data.
     */
    public void writeSector(int sectorNumber, byte[] data, int index) {
	Debug.ASSERT(0 <= sectorNumber && sectorNumber < getNumSectors());
	lock.acquire();			// only one disk I/O at a time
	disk.writeRequest(sectorNumber, data, index);
	semaphore.P();			// wait for interrupt
	lock.release();
    }

    /**
     * DiskDriver interrupt handler class.
     */
    private class DiskIntHandler implements InterruptHandler {
	/**
	 * When the disk interrupts, just wake up the thread that issued
	 * the request that just finished.
	 */
	public void handleInterrupt() {
	    semaphore.V();
	}
    }

}
