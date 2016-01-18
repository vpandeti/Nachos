// DirectoryEntry.java
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.filesys;

/**
 * This class defines a "directory entry", representing a file
 * in the directory.  Each entry gives the name of the file, and where
 * the file's header is to be found on disk.
 *
 * Internal data structures kept non-private so that Directory operations
 * can access them directly.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
class DirectoryEntry {
    /**
     * Maximum length of a file name.
     * For simplicity, we assume file names are <= 9 characters long.
     */
    static final int FileNameMaxLen = 9;

    /** Is this directory entry in use? */
    private boolean inUse;

    /** Location on disk to find the FileHeader for this file. */
    private int sector;

    /** Length of filename. */
    private int nameLen;

    /**
     * Text name for file (must be representable by a sequence of bytes
     * no longer than FileNameMaxLen).
     */
    private String name;

    /** On-disk representation of the file name. */
    private byte[] nameBytes;

    DirectoryEntry() {
	inUse = false;
    }

    /**
     * Determine if this DirectoryEntry is in use.
     *
     * @return true if the entry is in use, false if not.
     */
    boolean inUse() {
	return(inUse);
    }

    /**
     * Get the file name stored in this DirectoryEntry.
     *
     * @return the file name stored in this DirectoryEntry, or null
     * if the entry is not in use.
     */
    String getName() {
	if(!inUse)
	    return(null);
	return(name);
    }

    /**
     * Get the sector number stored in this DirectoryEntry.
     *
     * @return the sector number stored in this DirectoryEntry.
     */
    int getSector() {
	return(sector);
    }

    /**
     * Mark this directory entry as "in use" and set the file name
     * and disk sector number.
     *
     * @param name The name of the file.
     * @param sector The disk sector number of the file header.
     * @return true if the operation was successful, false if the entry
     * was already in use or the file name could not be represented within
     * the maximum number of bytes.
     */
    boolean setUsed(String name, int sector) {
	if(inUse)
	    return(false);
	int index = name.lastIndexOf('/');
	String fileName = name;
	if(index != -1)
	    fileName = name.substring(index+1);
	byte[] bytes = fileName.getBytes();
	if(bytes.length > FileNameMaxLen)
	    return(false);
	inUse = true;
	nameBytes = bytes;
	this.name = name;
	this.nameLen = bytes.length;
	this.sector = sector;
	return(true);
    }

    /**
     * Mark this directory entry as "unused".
     */
    void setUnused() {
	inUse = false;
    }
    
    void format() {
	inUse = false;
	nameBytes = null;
	name = null;
	nameLen = 0;
	sector = 0;
    }

    // the following methods deal with conversion between the on-disk and
    // the in-memory representation of a DirectoryEnry.
    // Note: these methods must be modified if any instance variables 
    // are added!!

    /**
     * Calculate size of on-disk representation of a DirectoryEntry.
     *
     * @return the number of bytes required to store a DirectoryEntry
     * on the disk.
     */
    public static int sizeOf() {
	// 1 byte for inUse, 4 bytes for sector, 4 bytes for nameLen,
	// and 1 byte for each possible byte in the name.
	return 1 + 4 + 4 + FileNameMaxLen;
    }

    /**
     * Initialize the fields of this DirectoryEntry object using
     * data read from the disk.
     *
     * @param buffer A buffer holding the data read from the disk.
     * @param pos Position in the buffer at which to start.
     */
    void internalize(byte[] buffer, int pos) {
	if (buffer[pos] != 0) {
	    inUse = true; 
	    sector = FileSystem.bytesToInt(buffer, pos+1);
	    nameLen = FileSystem.bytesToInt(buffer, pos+5);
	    name = new String(buffer, pos+9, nameLen);
	    nameBytes = name.getBytes();
	} else 
	    inUse = false;
    }

    /**
     * Export the fields of this DirectoryEntry object to a buffer
     * in a format suitable for writing to the disk.
     *
     * @param buffer A buffer into which to place the exported data.
     * @param pos Position in the buffer at which to start.
     */
    void externalize(byte[] buffer, int pos) {
	if (inUse) { 
	    buffer[pos] = 1; 
	    FileSystem.intToBytes(sector, buffer, pos+1);
	    FileSystem.intToBytes(nameLen, buffer, pos+5);
	    for(int i = 0; i < nameLen; i++)
		buffer[pos+9+i] = nameBytes[i];
	} else 
	    buffer[pos] = 0;
    }

}


