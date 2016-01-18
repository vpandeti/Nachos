// OpenFile.java
//	Class for reading and writing to individual files.
//	The operations supported are similar to	the UNIX ones.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.filesys;

/**
 * Interface that provides read/write access to files in a filesystem.
 *
 * There are two implementations.  One is a "stub" that directly
 * turns the file operations into the underlying UNIX operations.
 * (cf. comment in FileSystem.java).
 *
 * The other is the "real" implementation, which turns these
 * operations into read and write disk sector requests. 
 * In this baseline implementation of the file system, we don't 
 * worry about concurrent accesses to the file system
 * by different threads -- this is part of the assignment.
 * 
 * @author Peter Druschel (Rice University)
 * @author Eugene W. Stark (Stony Brook University)
 */
public interface OpenFile {
    /**
     * Set the position from which to
     * start reading/writing -- UNIX lseek
     *
     * @param position The desired position in the file, in bytes from
     * the start of file.
     */
    public void seek(long position);

    /**
     * Read bytes from the file, bypassing the implicit position.
     *
     * @param into Buffer into which to read data.
     * @param index Starting position in the buffer.
     * @param numBytes Number of bytes desired.
     * @param position Starting position in the file.
     * @return The number of bytes actually read (0 in case of an error).
     */
    public int readAt(byte into[], int index, int numBytes, long position);

    /**
     * Write bytes to the file, bypassing the implicit position.
     *
     * @param from Buffer containing the data to be written.
     * @param index Starting position in the buffer.
     * @param numBytes Number of bytes to write.
     * @param position Starting position in the file.
     * @return The number of bytes actually written (0 in case of an error).
     */
    public int writeAt(byte from[], int index, int numBytes, long position);

    /**
     * Read bytes from the file, starting at the implicit position.
     * The position is incremented by the number of bytes read.
     *
     * @param into Buffer into which to read data.
     * @param index Starting position in the buffer.
     * @param numBytes Number of bytes desired.
     * @return The number of bytes actually read (0 in case of an error).
     */
    public int read(byte into[], int index, int numBytes);

    /**
     * Write bytes to the file, starting at the implicit position.
     * The position is incremented by the number of bytes read.
     *
     * @param from Buffer containing the data to be written.
     * @param index Starting position in the buffer.
     * @param numBytes Number of bytes to write.
     * @return The number of bytes actually written (0 in case of an error).
     */
    public int write(byte from[], int index, int numBytes);

    /**
     * Determine the number of bytes in the file
     * (this interface is simpler than the UNIX idiom -- lseek to
     * end of file, tell, lseek back).
     *
     * @return the length of the file in bytes.
     */
    public long length();

    /**
     * Close the file, releasing any resources held in kernel memory.
     * Subsequent attempts to access the file will fail.
     *
     * @return 0 if an error occurred while closing the file, otherwise
     * nonzero.
     */
    public int close();
}
