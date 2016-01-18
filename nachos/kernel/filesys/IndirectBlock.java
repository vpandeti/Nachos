// FileHeader.jave
//	Routines for managing the disk file header (in UNIX, this
//	would be called the i-node).
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.filesys;

import nachos.Debug;

class IndirectBlock {

    public static final int DIRECTORY_TYPE = 0;
    public static final int FILE_TYPE = 1;
    
    protected final int MaxFileSize = 0;
    private int numBytes;
    private int numSectors;
    private int dataSectors[];
    private final FileSystemReal filesystem;
    private final int diskSectorSize;
    
    private int maxIndirectPointers;
    IndirectBlock(FileSystemReal filesystem) {
	this.filesystem = filesystem;
	diskSectorSize = filesystem.diskSectorSize;
	dataSectors = new int[maxIndirectPointers];
	maxIndirectPointers = filesystem.diskSectorSize;
	for(int i = 0; i < maxIndirectPointers; i++)
	    dataSectors[i] = -1;
    }

    // the following methods deal with conversion between the on-disk and
    // the in-memory representation of a DirectoryEnry.
    // Note: these methods must be modified if any instance variables 
    // are added!!

    /**
     * Initialize the fields of this FileHeader object using
     * data read from the disk.
     *
     * @param buffer A buffer holding the data read from the disk.
     * @param pos Position in the buffer at which to start.
     */
    private void internalize(byte[] buffer, int pos) {
	numBytes = FileSystem.bytesToInt(buffer, pos);
	numSectors = FileSystem.bytesToInt(buffer, pos+4);
	for (int i = 0; i < maxIndirectPointers; i++)
	    dataSectors[i] = FileSystem.bytesToInt(buffer, pos+8+i*4);
    }

    /**
     * Export the fields of this FileHeader object to a buffer
     * in a format suitable for writing to the disk.
     *
     * @param buffer A buffer into which to place the exported data.
     * @param pos Position in the buffer at which to start.
     */
    private void externalize(byte[] buffer, int pos) {
	FileSystem.intToBytes(numBytes, buffer, pos);
	FileSystem.intToBytes(numSectors, buffer, pos+4);
	for (int i = 0; i < maxIndirectPointers; i++)
	    FileSystem.intToBytes(dataSectors[i], buffer, pos+8+i*4);
    }

    /**
     * Initialize a fresh file header for a newly created file.
     * Allocate data blocks for the file out of the map of free disk blocks.
     * Return FALSE if there are not enough free blocks to accomodate
     *	the new file.
     *
     * @param freeMap is the bit map of free disk sectors.
     * @param fileSize is size of the new file.
     */
    boolean allocate(BitMap freeMap, int fileSize) {
	if(fileSize > MaxFileSize)
	    return false;		// file too large
	numBytes = fileSize;
	numSectors  = fileSize / diskSectorSize;
	if (fileSize % diskSectorSize != 0) numSectors++;

	if (freeMap.numClear() < numSectors || maxIndirectPointers < numSectors)
	    return false;		// not enough space

	for (int i = 0; i < numSectors; i++)
	    dataSectors[i] = freeMap.find();
	return true;
    }

    /**
     * De-allocate all the space allocated for data blocks for this file.
     *
     * @param freeMap is the bit map of free disk sectors.
     */
    void deallocate(BitMap freeMap) {
	for (int i = 0; i < numSectors; i++) {
	    Debug.ASSERT(freeMap.test(dataSectors[i]));  // ought to be marked!
	    freeMap.clear(dataSectors[i]);
	}
    }

    /**
     * Fetch contents of file header from disk. 
     *
     * @param sector is the disk sector containing the file header.
     */
    void fetchFrom(int sector) {
	byte buffer[] = new byte[diskSectorSize];
	filesystem.readSector(sector, buffer, 0);
	internalize(buffer, 0);
    }

    /**
     * Write the modified contents of the file header back to disk. 
     *
     * @param sector is the disk sector to contain the file header.
     */
    void writeBack(int sector) {
	byte buffer[] = new byte[diskSectorSize];
	externalize(buffer, 0);
	filesystem.writeSector(sector, buffer, 0); 
    }

    /**
     * Calculate which disk sector is storing a particular byte within the file.
     *    This is essentially a translation from a virtual address (the
     *	offset in the file) to a physical address (the sector where the
     *	data at the offset is stored).
     *
     * @param offset The location within the file of the byte in question.
     * @return the disk sector number storing the specified byte.
     */
    int byteToSector(int offset) {
	return(dataSectors[offset / diskSectorSize]);
    }

    /**
     * Retrieve the number of bytes in the file.
     *
     * @return the number of bytes in the file.
     */
    int fileLength() {
	return numBytes;
    }

    /**
     * 	Print the contents of the file header, and the contents of all
     *	the data blocks pointed to by the file header.
     */
    void print() {
	int i, j, k;
	byte data[] = new byte[diskSectorSize];

	System.out.print("FileHeader contents.  File size: " + numBytes
		+ ".,  File blocks: ");
	for (i = 0; i < numSectors; i++)
	    System.out.print(dataSectors[i] + " ");

	System.out.println("\nFile contents:");
	for (i = k = 0; i < numSectors; i++) {
	    filesystem.readSector(dataSectors[i], data, 0);
	    for (j = 0; (j < diskSectorSize) && (k < numBytes); j++, k++) {
		if ('\040' <= data[j] && data[j] <= '\176')   // isprint(data[j])
		    System.out.print((char)data[j]);
		else
		    System.out.print("\\" + Integer.toHexString(data[j] & 0xff));
	    }
	    System.out.println();
	}
    }
    
    protected boolean beyondFileExtension(int noOfSectors) {
	//noOfSectors -= numSectors;
	if(noOfSectors > 29)
	    return false;
	
	BitMap freeMap = FileSystemReal.freeMap;
	
	if(null == freeMap)
	    return false;
	
	// All sectors are allocated
	if(freeMap.numClear() == 0 || freeMap.numClear() < noOfSectors)
	    return false;

	if (freeMap.numClear() < numSectors || maxIndirectPointers < numSectors)
	    return false;		// not enough space

	try {
	    for (int i = numSectors; i < numSectors+noOfSectors; i++)
		dataSectors[i] = freeMap.find();
	    numSectors += noOfSectors;
	} catch (Exception ex) {
	    
	}
	return true;
    }
    
    protected void updateFileSize(int numBytes) {
	this.numBytes = numBytes;
    }

}
