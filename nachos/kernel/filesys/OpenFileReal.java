// OpenFileReal.java
//	Class for reading and writing to individual files.
//	The operations supported are similar to	the UNIX ones.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.filesys;

import nachos.Debug;

/**
 * This is a class for managing an open Nachos file.  As in UNIX, a
 * file must be open before we can read or write to it.
 * Once we're all done, we can close it (in Nachos, by deleting
 * the OpenFile data structure).
 *
 *	Also as in UNIX, for convenience, we keep the file header in
 *	memory while the file is open.
 *
 *	Supports opening, closing, reading and writing to 
 *	individual files.  The operations supported are similar to
 *	the UNIX ones.
 *
 *	There are two implementations.
 *	This is the "real" implementation, that turns these
 *	operations into read and write disk sector requests. 
 *	In this baseline implementation of the file system, we don't 
 *	worry about concurrent accesses to the file system
 *	by different threads -- this is part of the assignment.
 *
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
class OpenFileReal implements OpenFile {

    /** Underlying filesystem in which this file exists. */
    private final FileSystemReal filesystem;

    /** Size of a disk sector in the underlying filesystem. */
    private final int diskSectorSize;

    /** Cached copy of the header for this file. */
    private FileHeader hdr;
    private int hSector;

    /** Current position within the file. */
    private int seekPosition;

    /**
     * Open a Nachos file for reading and writing.  Bring the file header
     * into memory while the file is open.  This constructor is not public,
     * because users of the filesystem should be using the methods of the
     * FileSystem class to obtain an OpenFile.
     *
     * @param sector The location on disk of the file header for this file.
     * @param filesystem  The underlying filesystem in which this file exists.
     */
    OpenFileReal(int sector, FileSystemReal filesystem) { 
	hdr = new FileHeader(filesystem);
	hdr.fetchFrom(sector);
	hSector = sector;
	seekPosition = 0;
	this.filesystem = filesystem;
	diskSectorSize = filesystem.diskSectorSize;
    }

    /**
     * Change the current location within the open file -- the point at
     * which the next Read or Write will start from.
     *
     * @param position -- the location within the file for the next Read/Write.
     */
    public void seek(long position) {
	seekPosition = (int)position;
    }	

    /**
     * Read a portion of a file, starting from seekPosition.
     * Return the number of bytes actually read, and as a
     * side effect, increment the current position within the file.
     *
     * Implemented using the more primitive ReadAt.
     *
     * @param into The buffer to contain the data to be read from disk .
     * @param index Position in the buffer at which to begin placing data.
     * @param numBytes The number of bytes to transfer.
     * @return The number of bytes actually read (0 if error).
     */
    public int read(byte[] into, int index, int numBytes) {
	int result = readAt(into, index, numBytes, seekPosition);
	seekPosition += result;
	return result;
    }

    /**
     * Write a portion of a file, starting from seekPosition.
     * Return the number of bytes actually written, and as a
     * side effect, increment the current position within the file.
     *
     * Implemented using the more primitive WriteAt.
     *
     * @param from The buffer containing the data to be written to disk .
     * @param index Position in the buffer at which to begin taking data.
     * @param numBytes The number of bytes to transfer.
     * @return The number of bytes actually written (0 if error).
     */
    public int write(byte[] from, int index, int numBytes) {
	int result = writeAt(from, index, numBytes, seekPosition);
	seekPosition += result;
	return result;
    }

    /**
     * Read a portion of a file, starting at "position".
     * Return the number of bytes actually read, but has
     * no side effects.
     *
     * There is no guarantee the request starts or ends on an even disk sector
     * boundary; however the disk only knows how to read a whole disk
     * sector at a time.  Thus:
     *
     *	   We read in all of the full or partial sectors that are part of the
     *	   request, but we only copy the part we are interested in.
     *
     * @param into The buffer to contain the data to be read from disk.
     * @param index Position in the buffer at which to begin placing data.
     * @param numBytes The number of bytes to transfer.
     * @param position The offset within the file of the first byte to be
     *			read/written.
     * @return The number of bytes actually read (0 if error).
     */
    public int readAt(byte[] into, int index, int numBytes, long position) {
	int fileLength = hdr.fileLength();
	int i, firstSector, lastSector, numSectors;
	byte buf[];

	if ((numBytes <= 0) || (position >= fileLength))
	    return 0; 				// check request
	if ((position + numBytes) > fileLength)		
	    numBytes = fileLength - (int)position;
	Debug.printf('f', "Reading %d bytes at %d, from file of length %d.\n",
		new Integer(numBytes), new Long(position), 
		new Integer(fileLength));

	firstSector = (int)position / diskSectorSize;
	lastSector = ((int)position + numBytes - 1) / diskSectorSize;
	numSectors = 1 + lastSector - firstSector;

	// read in all the full and partial sectors that we need
	buf = new byte[numSectors * diskSectorSize];
	for (i = firstSector; i <= lastSector; i++)	
	    filesystem.readSector(hdr.byteToSector(i * diskSectorSize), 
		    buf, (i - firstSector) * diskSectorSize);

	// copy the part we want
	System.arraycopy(buf, (int)position - (firstSector * diskSectorSize),
		into, index, numBytes);
	return numBytes;
    }

    /**
     * Write a portion of a file, starting at "position".
     * Return the number of bytes actually written, but has
     * no side effects (except that Write modifies the file, of course).
     *
     * There is no guarantee the request starts or ends on an even disk sector
     * boundary; however the disk only knows how to write a whole disk
     * sector at a time.  Thus:
     *
     *	   We must first read in any sectors that will be partially written,
     *	   so that we don't overwrite the unmodified portion.  We then copy
     *	   in the data that will be modified, and write back all the full
     *	   or partial sectors that are part of the request.
     *
     * @param from The buffer containing the data to be written to disk.
     * @param index Position in the buffer at which to begin placing data.
     * @param numBytes The number of bytes to transfer.
     * @param position The offset within the file of the first byte to be
     *			read/written.
     */
    public int writeAt(byte from[], int index, int numBytes, long position) {

	int fileLength = hdr.fileLength();
	int i, firstSector, lastSector, numSectors;
	boolean firstAligned, lastAligned;
	byte buf[];

	if ((numBytes <= 0) || (position >= fileLength))
	    return 0;				// check request
	if ((position + numBytes) > fileLength)
	    numBytes = fileLength - (int)position;
	Debug.printf('f', "Writing %d bytes at %d, from file of length %d.\n",
		new Integer(numBytes), new Long(position), 
		new Integer(fileLength));

	firstSector = (int)position / diskSectorSize;
	lastSector = ((int)position + numBytes - 1) / diskSectorSize;
	numSectors = 1 + lastSector - firstSector;

	buf = new byte[numSectors * diskSectorSize];

	firstAligned = (position == (firstSector * diskSectorSize));
	lastAligned = ((position + numBytes) == ((lastSector + 1) * diskSectorSize));

	// read in first and last sector, if they are to be partially modified
	if (!firstAligned)
	    readAt(buf, 0, diskSectorSize, firstSector * diskSectorSize);	
	if (!lastAligned && ((firstSector != lastSector) || firstAligned))
	    readAt(buf, (lastSector - firstSector) * diskSectorSize, 
		    diskSectorSize, lastSector * diskSectorSize);	

	// copy in the bytes we want to change 
	System.arraycopy(from, index, 
		buf, (int)position - (firstSector * diskSectorSize), 
		numBytes);

	// write modified sectors back
	for (i = firstSector; i <= lastSector; i++)	
	    filesystem.writeSector(hdr.byteToSector(i * diskSectorSize), 
		    buf, (i - firstSector) * diskSectorSize);

	return numBytes;
    }

    /**
     * Determine the number of bytes in the file.
     *
     * @return the length of the file in bytes.
     */
    public long length() { 
	return hdr.fileLength(); 
    }

    /**
     * Close the file, releasing any resources held in kernel memory.
     * Subsequent attempts to access the file will fail.
     *
     * @return 0 if an error occurred while closing the file, otherwise
     * nonzero.
     */
    public int close() {
	// If it is possible that we made changes to the FileHeader,
	// it must be written back to the disk at this point.
	hdr = null;  // Ensure further access fails.
	return(1);
    }

}
