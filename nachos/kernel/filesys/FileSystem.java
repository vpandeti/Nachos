// FileSystem.java
//	Interface to a Nachos file system.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.filesys;

import nachos.Debug;
import nachos.Options;
import nachos.kernel.Nachos;
import nachos.kernel.devices.DiskDriver;

/**
 * This abstract class defines the interface to a Nachos file system.
 *
 * A file system is a set of files stored on disk, organized
 * into directories.  Operations on the file system have to
 * do with "naming" -- creating, opening, and deleting files,
 * given a textual file name.  Operations on an individual
 * "open file" (read, write, close) are to be found in the OpenFile
 * class (Openfile.java).
 *
 * We define two separate implementations of the file system:
 * a "real" file system, built on top of a disk simulator,
 * and a "stub" file system, which just re-defines the Nachos file system 
 * operations as operations on the native file system on the machine
 * running the Nachos simulation.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public abstract class FileSystem {
    /**
     * Create a new file with a specified name and size.
     *
     * @param name The name of the file.
     * @param initialSize The size of the file.
     * @return true if the operation was successful, otherwise false.
     */
    public abstract boolean create(String name, long initialSize);

    /**
     * Open the file with the specified name and return an OpenFile
     * object that provides access to the file contents.
     *
     * @param name The name of the file.
     * @return An OpenFile object that provides access to the file contents,
     * if the file was successfully opened, otherwise null.
     */
    public abstract OpenFile open(String name);

    /**
     * Remove the file with the specified name.
     *
     * @param name The name of the file.
     * @return true if the operation was successful, otherwise false.
     */
    public abstract boolean remove(String name);

    /**
     * Protected constructor to force creation of a filesystem using
     * the init() factory method.
     */
    protected FileSystem() { }

    /**
     * Factory method to create the proper type of filesystem and
     * hide the type actually being used.
     * 
     * @param disk  Disk driver to use to access the filesystem.
     */
    public static FileSystem init(DiskDriver disk) {
	Options options = Nachos.options;
	Debug.ASSERT(!(options.FILESYS_STUB && options.FILESYS_REAL),
		     "The stub filesystem and Nachos filesystem cannot both be used.");
	if(options.FILESYS_STUB)
	    return(new FileSystemStub());
	else if(options.FILESYS_REAL) {
	    Debug.ASSERT(disk != null);
	    return new FileSystemReal(disk, options.FORMAT_DISK);
	} else
	    return null;
    }

    /**
     * List contents of the filesystem directory (for debugging).
     * This is only implemented by the real filesystem.
     */
    public void list() { }

    /**
     * Print contents of the entire filesystem (for debugging).
     * This is only implemented by the real filesystem.
     */
    public void print() { }

    /**
     * Creates a new directory with a specified name.
     *
     * @param name The name of the file.
     * @return true if the operation was successful, otherwise false.
     */
    public abstract boolean mkdir(String name);
    
    /**
     * Deletes the specified directory.
     *
     * @param name The name of the file.
     * @return true if the operation was successful, otherwise false.
     */
    public abstract boolean rmdir(String name);
    
    /**
     * Creates a new file with a specified name.
     *
     * @param name The name of the file.
     * @return true if the operation was successful, otherwise false.
     */
    public abstract boolean mkfile(String name, int size);
    
    /**
     * Opens a file with the specified name.
     *
     * @param name The name of the file.
     * @return true if the operation was successful, otherwise false.
     */
    public abstract OpenFile openFile(String name);
    
    /**
     * Checks for inconsistencies
     */
    public abstract void start();
    /**
     * Utility method to deserialize a sequence of bytes into an integer.
     *
     * @param buffer The buffer from which the value is to be deserialized.
     * @param pos Starting offset from the beginning of the buffer at which
     * the serialized bytes representing the value exist.
     * @return The integer value represented by the bytes in the buffer.
     */
    public static int bytesToInt(byte[] buffer, int pos) {
	return (buffer[pos] << 24) | 
		((buffer[pos+1] << 16) & 0xff0000) |
		((buffer[pos+2] << 8) & 0xff00) | 
		(buffer[pos+3] & 0xff);
    }

    /**
     * Utility method to serialize an integer into a sequence of bytes.
     *
     * @param val The integer value to be serialized.
     * @param buffer The buffer into which the value is to be serialized.
     * @param pos Starting offset from the beginning of the buffer at which
     * the serialized bytes representing the value are to be placed.
     */
    public static void intToBytes(int val, byte[] buffer, int pos) {
	buffer[pos] = (byte)(val >> 24 & 0xff);
	buffer[pos+1] = (byte)(val >> 16 & 0xff);
	buffer[pos+2] = (byte)(val >> 8 & 0xff);
	buffer[pos+3] = (byte)(val & 0xff);
    }
    
}
