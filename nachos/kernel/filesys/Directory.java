// Directory.java
//	Class to manage a directory of file names.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.filesys;

import java.util.ArrayList;

/**
 * This class class defines a UNIX-like "directory".  Each entry in
 * the directory describes a file, and where to find it on disk.
 *
 * The directory is a table of fixed length entries; each
 * entry represents a single file, and contains the file name,
 * and the location of the file header on disk.  The fixed size
 * of each directory entry means that we have the restriction
 * of a fixed maximum size for file names.
 *
 * Also, this implementation has the restriction that the size
 * of the directory cannot expand.  In other words, once all the
 * entries in the directory are used, no more files can be created.
 * Fixing this is one of the parts to the assignment.
 *
 * The directory data structure can be stored in memory, or on disk.
 * When it is on disk, it is stored as a regular Nachos file.
 * The constructor initializes a directory structure in memory; the
 * fetchFrom/writeBack operations shuffle the directory information
 * from/to disk. 
 * 
 * We assume mutual exclusion is provided by the caller.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
class Directory {

    /** Number of entries in the directory. */
    private int tableSize;

    /** Table of pairs: file name/file header location. */
    private DirectoryEntry table[];

    /** The underlying filesystem in which the directory resides. */
    private final FileSystemReal filesystem;
    
    /** Load factor to increase the directory capacity */
    private static final int LOAD_FACTOR = 1;

    /**
     * Initialize a directory; initially, the directory is completely
     * empty.  If the disk is being formatted, an empty directory
     * is all we need, but otherwise, we need to call FetchFrom in order
     * to initialize it from disk.
     *
     * @param size The number of entries in the directory.
     * @param filesystem  The underlying filesystem in which this directory exists.
     */
    Directory(int size, FileSystemReal filesystem)
    {
	this.filesystem = filesystem;
	table = new DirectoryEntry[size];
	tableSize = size;
	for (int i = 0; i < tableSize; i++) {
	    table[i] = new DirectoryEntry();
	}
    }
    
    /**
     * Read the contents of the directory from disk.
     *
     * @param file The file containing the directory contents.
     */
    void fetchFrom(OpenFile file) {
	byte buffer[] = new byte[tableSize * DirectoryEntry.sizeOf()];
	file.readAt(buffer, 0, tableSize * DirectoryEntry.sizeOf(), 0);
	int pos = 0;
	for (int i = 0; i < tableSize; i++) {
	    table[i].internalize(buffer, pos);
	    pos += DirectoryEntry.sizeOf();
	}
    }

    /**
     * Write any modifications to the directory back to disk
     *
     * @param file The file to contain the new directory contents.
     */
    void writeBack(OpenFile file) {
	byte buffer[] = new byte[tableSize * DirectoryEntry.sizeOf()];
	int pos = 0;
	for (int i = 0; i < tableSize; i++) {
	    table[i].externalize(buffer, pos);
	    pos += DirectoryEntry.sizeOf();
	}
	file.writeAt(buffer, 0, tableSize * DirectoryEntry.sizeOf(), 0);
    }

    /**
     * Look up file name in directory, and return its location in the table of
     * directory entries.  Return -1 if the name isn't in the directory.
     *
     * @param name The file name to look up.
     * @return The index of the entry in the table, if present, otherwise -1.
     */
    private int findIndex(String name) {
	for (int i = 0; i < tableSize; i++) {
	    if (table[i].inUse() && name.equals(table[i].getName()))
		return i;
	}
	return -1;		// name not in directory
    }

    /**
     * Look up file name in directory, and return the disk sector number
     * where the file's header is stored. Return -1 if the name isn't 
     * in the directory.
     *
     * @param name The file name to look up.
     * @return The disk sector number where the file's header is stored,
     * if the entry was found, otherwise -1.
     */
    int find(String name) {
	int i = findIndex(name);

	if (i != -1)
	    return table[i].getSector();
	return -1;
    }
    
    String getName(String name) {
	int i = findIndex(name);

	if (i != -1)
	    return table[i].getName();
	return null;
    }

    /**
     * Add a file into the directory.  Return TRUE if successful;
     * return FALSE if the file name is already in the directory,
     * or if the directory is completely full, and has no more space for
     * additional file names, or if the file name cannot be represented
     * in the number of bytes available in a directory entry.
     *
     * @param name The name of the file being added.
     * @param newSector The disk sector containing the added file's header.
     * @return true if the file was successfully added, otherwise false.
     */
    boolean add(String name, int newSector) { 
	if (findIndex(name) != -1)
	    return false;
	int i = 0;
	for (; i < tableSize; i++)
	    if (!table[i].inUse()) {
		if(!table[i].setUsed(name, newSector))
		    return(false);
		return(true);
	    }
	extendTheDirectoryCapacity();
	table[i].setUsed(name, newSector);
	return true;
	//return false;	// no space.  Fix when we have extensible files.
    }

    /**
     * Remove a file name from the directory.  Return TRUE if successful;
     * return FALSE if the file isn't in the directory. 
     *
     * @param name The file name to be removed.
     */
    boolean remove(String name) { 
	int i = findIndex(name);

	if (i == -1)
	    return false; 		// name not in directory
	table[i].setUnused();
	table[i].format();
	return true;	
    }

    /**
     * List all the file names in the directory (for debugging).
     */
    void list() {
	for (int i = 0; i < tableSize; i++)
	    if (table[i].inUse())
		System.out.println(table[i].getName());
    }
    
    ArrayList<String> getList() {
	ArrayList<String> list = new ArrayList<String>();
	for (int i = 0; i < tableSize; i++)
	    if (table[i].inUse())
		list.add(table[i].getName());
	return list;
    }

    /**
     * List all the file names in the directory, their FileHeader locations,
     * and the contents of each file (for debugging).
     */
    void print() {
	FileHeader hdr = new FileHeader(filesystem);

	System.out.print("Directory contents: ");
	for (int i = 0; i < tableSize; i++)
	    if (table[i].inUse()) {
		System.out.println("Name " + table[i].getName()
			+ ", Sector: " + table[i].getSector());
		hdr.fetchFrom(table[i].getSector());
		hdr.print();
	    }
	System.out.println("");
    }
    
    /**
     * Extending the capacity of the directory to hold more files
     */
    private void extendTheDirectoryCapacity() {
	tableSize += LOAD_FACTOR;
	
	DirectoryEntry[] tempTable = new DirectoryEntry[tableSize];
	int i = 0;
	for (; i < table.length; i++) {
	    tempTable[i] = table[i];
	}
	table = tempTable;
	tempTable = null;
	for(;i<tableSize;i++){
	    table[i] = new DirectoryEntry();
	}
    }
    
    public int getTableSize() {
	return tableSize;
    }
}
