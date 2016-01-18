// FileSystemReal.java
//	Class to manage the overall operation of the file system.
//	Implements methods to map from textual file names to files.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.filesys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.kernel.devices.DiskDriver;
import nachos.kernel.filesys.test.FileSystemTest;
import nachos.machine.NachosThread;

/**
 * This class manages the overall operation of the file system.
 *	It implements methods to map from textual file names to files.
 *	Each file in the file system has:
 *	   A file header, stored in a sector on disk 
 *		(the size of the file header data structure is arranged
 *		to be precisely the size of 1 disk sector);
 *	   A number of data blocks;
 *	   An entry in the file system directory.
 *
 * 	The file system consists of several data structures:
 *	   A bitmap of free disk sectors (cf. bitmap.h);
 *	   A directory of file names and file headers.
 *
 *      Both the bitmap and the directory are represented as normal
 *	files.  Their file headers are located in specific sectors
 *	(sector 0 and sector 1), so that the file system can find them 
 *	on bootup.
 *
 *	The file system assumes that the bitmap and directory files are
 *	kept "open" continuously while Nachos is running.
 *
 *	For those operations (such as create, remove) that modify the
 *	directory and/or bitmap, if the operation succeeds, the changes
 *	are written immediately back to disk (the two files are kept
 *	open during all this time).  If the operation fails, and we have
 *	modified part of the directory and/or bitmap, we simply discard
 *	the changed version, without writing it back to disk.
 *
 * 	Our implementation at this point has the following restrictions:
 *
 *	   there is no synchronization for concurrent accesses;
 *	   files have a fixed size, set when the file is created;
 *	   files cannot be bigger than about 3KB in size;
 *	   there is no hierarchical directory structure, and only a limited
 *	     number of files can be added to the system;
 *	   there is no attempt to make the system robust to failures
 *	    (if Nachos exits in the middle of an operation that modifies
 *	    the file system, it may corrupt the disk).
 *
 *	A file system is a set of files stored on disk, organized
 *	into directories.  Operations on the file system have to
 *	do with "naming" -- creating, opening, and deleting files,
 *	given a textual file name.  Operations on an individual
 *	"open" file (read, write, close) are to be found in the OpenFile
 *	class (OpenFile.java).
 *
 *	We define two separate implementations of the file system. 
 *	This version is a "real" file system, built on top of 
 *	a disk simulator.  The disk is simulated using the native
 *	file system on the host platform (in a file named "DISK"). 
 *
 *	In the "real" implementation, there are two key data structures used 
 *	in the file system.  There is a single "root" directory, listing
 *	all of the files in the file system; unlike UNIX, the baseline
 *	system does not provide a hierarchical directory structure.  
 *	In addition, there is a bitmap for allocating
 *	disk sectors.  Both the root directory and the bitmap are themselves
 *	stored as files in the Nachos file system -- this causes an interesting
 *	bootstrap problem when the simulated disk is initialized. 
 *
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
class FileSystemReal extends FileSystem implements Runnable {

  // Sectors containing the file headers for the bitmap of free sectors,
  // and the directory of files.  These file headers are placed in 
  // well-known sectors, so that they can be located on boot-up.

  /** The disk sector containing the bitmap of free sectors. */
  private static final int FreeMapSector = 0;

  /** The disk sector containing the directory of files. */
  private static final int DirectorySector = 1;
  
  /** The maximum number of entries in a directory. */
  private static final int NumDirEntries = 10;

  /** Access to the disk on which the filesystem resides. */
  private final DiskDriver diskDriver;
  
  /** Number of sectors on the disk. */
  public final int numDiskSectors;
  
  /** Sector size of the disk. */
  public final int diskSectorSize;
  
  /** Freemap */
  public static BitMap freeMap;
  
  // Initial file sizes for the bitmap and directory; until the file system
  // supports extensible files, the directory size sets the maximum number 
  // of files that can be loaded onto the disk.

  /** The initial file size for the bitmap file. */
  private final int FreeMapFileSize;

  /** The initial size of a directory file. */
  private final int DirectoryFileSize;

  /** Bit map of free disk blocks, represented as a file. */
  private final OpenFile freeMapFile;

  /** "Root" directory -- list of file names, represented as a file. */
  private final OpenFile directoryFile;

  /** Contains Directory name as key and child directories as value*/
  private HashMap<String, ArrayList<String>> directoryTracker = new HashMap<String, ArrayList<String>>();
  
  /** Contains Directory name as key and files in that directory as value*/
  private HashMap<String, ArrayList<String>> fileTracker = new HashMap<String, ArrayList<String>>();
  
  /**
   * Initialize the file system.  If format = true, the disk has
   * nothing on it, and we need to initialize the disk to contain
   * an empty directory, and a bitmap of free sectors (with almost but
   * not all of the sectors marked as free).  
   *
   * If format = false, we just have to open the files
   * representing the bitmap and the directory.
   *
   * @param diskDriver  Access to the disk on which the filesystem resides.
   * @param format  Should we initialize the disk?
   */
  protected FileSystemReal(DiskDriver diskDriver, boolean format) { 
    Debug.print('f', "Initializing the file system.\n");
    this.diskDriver = diskDriver;
    numDiskSectors = diskDriver.getNumSectors();
    diskSectorSize = diskDriver.getSectorSize();
    FreeMapFileSize = (numDiskSectors / BitMap.BitsInByte);
    DirectoryFileSize = (DirectoryEntry.sizeOf() * NumDirEntries);
    
    if (format) {
      BitMap freeMap = new BitMap(numDiskSectors);
      Directory directory = new Directory(NumDirEntries, this);
      FileHeader mapHdr = new FileHeader(this);
      FileHeader dirHdr = new FileHeader(this);

      Debug.print('f', "Formatting the file system.\n");

      // First, allocate space for FileHeaders for the directory and bitmap
      // (make sure no one else grabs these!)
      freeMap.mark(FreeMapSector);	    
      freeMap.mark(DirectorySector);

      // Second, allocate space for the data blocks containing the contents
      // of the directory and bitmap files.  There better be enough space!

      Debug.ASSERT(mapHdr.allocate(freeMap, FreeMapFileSize));
      Debug.ASSERT(dirHdr.allocate(freeMap, DirectoryFileSize));

      // Flush the bitmap and directory FileHeaders back to disk
      // We need to do this before we can "Open" the file, since open
      // reads the file header off of disk (and currently the disk has 
      // garbage on it!).

      Debug.print('f', "Writing headers back to disk.\n");
      mapHdr.writeBack(FreeMapSector);    
      dirHdr.writeBack(DirectorySector);

      // OK to open the bitmap and directory files now
      // The file system operations assume these two files are left open
      // while Nachos is running.

      freeMapFile = new OpenFileReal(FreeMapSector, this);
      directoryFile = new OpenFileReal(DirectorySector, this);
     
      // Once we have the files "open", we can write the initial version
      // of each file back to disk. The directory at this point is completely
      // empty; but the bitmap has been changed to reflect the fact that
      // sectors on the disk have been allocated for the file headers and
      // to hold the file data for the directory and bitmap.

      Debug.print('f', "Writing bitmap and directory back to disk.\n");
      freeMap.writeBack(freeMapFile);	 // flush changes to disk
      directory.writeBack(directoryFile);

      if (Debug.isEnabled('f')) {
	freeMap.print();
	directory.print();
      }
      
    } else {
      // if we are not formatting the disk, just open the files representing
      // the bitmap and directory; these are left open while Nachos is 
      // running
      freeMapFile = new OpenFileReal(FreeMapSector, this);
      directoryFile = new OpenFileReal(DirectorySector, this);
    }
    directoryTracker.put("/", new ArrayList<String>());
  }
  
  /**
   * Read a sector of the filesystem, using the underlying disk driver.
   *
   * @param sectorNumber The disk sector to read.
   * @param data The buffer to hold the contents of the disk sector.
   * @param index Offset in the buffer at which to place the data.
   */
  void readSector(int sectorNumber, byte[] data, int index) {
      diskDriver.readSector(sectorNumber, data, index);
  }
  
  /**
   * Write a sector of the filesystem, using the underlying disk driver.
   *
   * @param sectorNumber The disk sector to be written.
   * @param data The new contents of the disk sector.
   * @param index Offset in the buffer from which to get the data.
   */
  void writeSector(int sectorNumber, byte[] data, int index) {
      diskDriver.writeSector(sectorNumber, data, index);
  }

  /**
   * Create a file in the Nachos file system (similar to UNIX create).
   * Since we can't increase the size of files dynamically, we have
   * to supply the initial size of the file.
   *
   * The steps to create a file are:
   *  Make sure the file doesn't already exist;
   *  Allocate a sector for the file header;
   *  Allocate space on disk for the data blocks for the file;
   *  Add the name to the directory;
   *  Store the new file header on disk;
   *  Flush the changes to the bitmap and the directory back to disk.
   *
   * Return true if everything goes ok, otherwise, return false.
   *
   * Create fails if:
   * 	file is already in directory;
   *	no free space for file header;
   *	no free entry for file in directory;
   *	no free space for data blocks for the file.
   *
   * Note that this implementation assumes there is no concurrent access
   *	to the file system!
   *
   * @param name  The name of file to be created.
   * @param initialSize  The size of file to be created.
   * @return true if the file was successfully created, otherwise false.
   */
  public boolean create(String name, long initialSize) {
    Directory directory;
    
    FileHeader hdr;
    int sector;
    boolean success;

    Debug.printf('f', "Creating file %s, size %d\n", name, 
		 new Long(initialSize));

    directory = new Directory(NumDirEntries, this);
    directory.fetchFrom(directoryFile);

    if (directory.find(name) != -1)
      success = false;			// file is already in directory
    else {	
      freeMap = new BitMap(numDiskSectors);
      freeMap.fetchFrom(freeMapFile);
      sector = freeMap.find();	// find a sector to hold the file header
      if (sector == -1) 		
	success = false;		// no free block for file header 
      else if (!directory.add(name, sector))
	success = false;	// no space in directory
      else {
	hdr = new FileHeader(this);
	if (!hdr.allocate(freeMap, (int)initialSize))
	  success = false;	// no space on disk for data
	else {	
	  success = true;
	  // everthing worked, flush all changes back to disk
	  hdr.type = FileHeader.FILE_TYPE;
	  hdr.writeBack(sector); 		
	  directory.writeBack(directoryFile);
	  freeMap.writeBack(freeMapFile);
	}
      }
    }
    return success;
  }

  /**
   * Open a file for reading and writing.  
   * To open a file:
   *	  Find the location of the file's header, using the directory;
   *	  Bring the header into memory.
   *
   * @param name The text name of the file to be opened.
   */
  public OpenFile open(String name) { 
    Directory directory = new Directory(NumDirEntries, this);
    OpenFile openFile = null;
    int sector;

    Debug.printf('f', "Opening file %s\n", name);
    directory.fetchFrom(directoryFile);
    sector = directory.find(name); 
    if (sector >= 0) 		
      openFile = new OpenFileReal(sector, this);// name was found in directory 
    return openFile;			        // return null if not found
  }
  
  public OpenFile openDirectory(String name, OpenFile file) { 
      Directory directory = new Directory(NumDirEntries, this);
      OpenFile openFile = null;
      int sector;

      Debug.printf('f', "Opening file %s\n", name);
      directory.fetchFrom(file);
      sector = directory.find(name); 
      if (sector >= 0) 		
        openFile = new OpenFileReal(sector, this);// name was found in directory 
      return openFile;			        // return null if not found
    }
  
  public OpenFile openDirectoryForMkdir(String name, OpenFile file, Directory directory) { 
      OpenFile openFile = null;
      int sector;

      Debug.printf('f', "Opening file %s\n", name);
      directory.fetchFrom(file);
      sector = directory.find(name); 
      if (sector >= 0) 		
        openFile = new OpenFileReal(sector, this);// name was found in directory 
      return openFile;			        // return null if not found
    }

  /**
   * Delete a file from the file system.  This requires:
   *    Remove it from the directory;
   *    Delete the space for its header;
   *    Delete the space for its data blocks;
   *    Write changes to directory, bitmap back to disk.
   *
   * Return true if the file was deleted, false if the file wasn't
   *	in the file system.
   *
   * @param name The text name of the file to be removed.
   */
  public boolean remove(String name) { 
    Directory directory;
    BitMap freeMap;
    FileHeader fileHdr;
    int sector;
    
    directory = new Directory(NumDirEntries, this);
    directory.fetchFrom(directoryFile);
    sector = directory.find(name);
    if (sector == -1) {
       return false;			 // file not found 
    }
    fileHdr = new FileHeader(this);
    fileHdr.fetchFrom(sector);

    freeMap = new BitMap(numDiskSectors);
    freeMap.fetchFrom(freeMapFile);

    fileHdr.deallocate(freeMap);  		// remove data blocks
    freeMap.clear(sector);			// remove header block
    directory.remove(name);

    freeMap.writeBack(freeMapFile);		// flush to disk
    directory.writeBack(directoryFile);        // flush to disk
    return true;
  } 
  
  public boolean removeEntity(String name) { 
      Directory directory;
      BitMap freeMap;
      FileHeader fileHdr;
      int sector;
      
      String[] dirs = name.split("/");
      directory = loadFileForRmdir(name);
      if(directory == null && name.lastIndexOf("/") == 0) {
	  directory = new Directory(NumDirEntries, this);
	  directory.fetchFrom(directoryFile);
      }
      //directory.fetchFrom(directoryFile);
      sector = directory.find(dirs[dirs.length-1]);
      if (sector == -1) {
         return false;			 // file not found 
      }
      OpenFile openFile = new OpenFileReal(sector, this);
      fileHdr = new FileHeader(this);
      fileHdr.fetchFrom(sector);

      freeMap = new BitMap(numDiskSectors);
      freeMap.fetchFrom(freeMapFile);

      fileHdr.deallocate(freeMap);  		// remove data blocks
      freeMap.clear(sector);			// remove header block
      directory.remove(dirs[dirs.length-1]);

      freeMap.writeBack(freeMapFile);		// flush to disk
      if(name.lastIndexOf("/") == 0) {
	  directory = new Directory(NumDirEntries, this);
	  directory.writeBack(directoryFile);        // flush to disk
      } else {
	  directory.writeBack(openFile);        // flush to disk
      }
      return true;
    }

  /**
   * List all the files in the file system directory (for debugging).
   */
  public void list() {
      /*Directory directory = new Directory(NumDirEntries, this);

      directory.fetchFrom(directoryFile);
      directory.list();*/
      Debug.println('f', "Listing the contents of the filesystem");
      list(directoryFile, "");
      
  }
  
    public void list(OpenFile file, String root) {
	try {
	    Directory directory = new Directory(NumDirEntries, this);

	    directory.fetchFrom(file);
	    directory.list();

	    OpenFile openFile;
	    FileHeader hdr;
	    ArrayList<String> list = directory.getList();
	    int sector = -1;
	    for (String s : list) {
		if (s.contains("mkdir"))
		    continue;
		sector = directory.find(s);
		if (sector != -1) {
		    String entryName = directory.getName(s);
		    if (entryName == null)
			continue;
		    Debug.println('f', root + "/" + entryName);
		    openFile = new OpenFileReal(sector, this);
		    hdr = new FileHeader(this);
		    hdr.fetchFrom(sector);
		    directory = new Directory(NumDirEntries, this);
		    if (isFile(s))
			continue;
		    directory.fetchFrom(openFile);
		    list(openFile, root + "/" + s);
		}
	    }
	} catch (Exception ex) {

	}
    }
  
  ArrayList<Integer> sectors = new ArrayList<Integer>();
  public void consistencyCheck(OpenFile file, String root) {
      Directory directory = new Directory(NumDirEntries, this);

      directory.fetchFrom(file);
      directory.list();
      
      OpenFile openFile;
      FileHeader hdr;
      ArrayList<String> list = directory.getList();
      int sector = -1;
      for(String s: list) {
	  if(s.equals("mkdir"))
	      continue;
	  sector = directory.find(s);
	  if(sectors.contains(sector))
	      Debug.println('f', "Consistency check - Same sector is allocated to multiple files");
	  else
	      sectors.add(sector);
	  if (sector != -1) {
	      String entryName = directory.getName(s);
	      if (entryName == null)
		  continue;
	      Debug.println('f', root + "/" + entryName);
	      openFile = new OpenFileReal(sector, this);
	      hdr = new FileHeader(this);
	      hdr.fetchFrom(sector);
	      directory = new Directory(NumDirEntries, this);
	      
	      FileHeader bitHdr = new FileHeader(this);
	      FileHeader dirHdr = new FileHeader(this);
	      BitMap freeMap = new BitMap(numDiskSectors);

	      Debug.print('+', "Bit map file header:\n");
	      bitHdr.fetchFrom(FreeMapSector);
	      bitHdr.print();

	      Debug.print('+', "Directory file header:\n");
	      dirHdr.fetchFrom(sector);
	      dirHdr.print();

	      freeMap.fetchFrom(freeMapFile);
	      freeMap.print();

	      directory.fetchFrom(directoryFile);
	      directory.print();
	      
	      if(isFile(s))
		  continue;
	      directory.fetchFrom(openFile);
	      consistencyCheck(openFile, root+"/"+s);
	  }		
      }
  }

  /**
   * Print everything about the file system (for debugging):
   *  the contents of the bitmap;
   *  the contents of the directory;
   *  for each file in the directory:
   *      the contents of the file header;
   *      the data in the file.
   */
  public void print() {
    FileHeader bitHdr = new FileHeader(this);
    FileHeader dirHdr = new FileHeader(this);
    BitMap freeMap = new BitMap(numDiskSectors);
    Directory directory = new Directory(NumDirEntries, this);

    Debug.print('+', "Bit map file header:\n");
    bitHdr.fetchFrom(FreeMapSector);
    bitHdr.print();

    Debug.print('+', "Directory file header:\n");
    dirHdr.fetchFrom(DirectorySector);
    dirHdr.print();

    freeMap.fetchFrom(freeMapFile);
    freeMap.print();

    directory.fetchFrom(directoryFile);
    directory.print();

  }
  
    public boolean mkdir(String name) {
	boolean success = false;
	String[] dirs = name.split("/");
	
	Directory directory;
	FileHeader hdr;
	int sector;
	OpenFile openFile = directoryFile;

	directory = new Directory(NumDirEntries, this);
	directory.fetchFrom(directoryFile);
	for(int i = 1; i < dirs.length; i++) {
	    sector = directory.find(dirs[i]);
	    if (sector != -1) {
		if(i == dirs.length-1) {
		    Debug.println('f', "Duplicate directory");
		    return false;
		} else {
		    String entryName = directory.getName(dirs[i]);
		    if(entryName == null)
			continue;
		    if(isFile(entryName))
			continue;
		    openFile = new OpenFileReal(sector, this);
		    hdr = new FileHeader(this);
		    hdr.fetchFrom(sector);
		    directory = new Directory(NumDirEntries, this);
		    directory.fetchFrom(openFile);
		}
	    } else {
		if(i != dirs.length-1) {
		    Debug.println('f', "Invalid directory path");
		    return false;
		} else {
		    createEntity(dirs[i], DirectoryFileSize, openFile);
		    OpenFile file = openDirectory(dirs[i], openFile);
		    if (file == null) {
		        return false;
		    }
		    Directory d = new Directory(NumDirEntries, this);
		    d.writeBack(file);

		    return true;
		}
	    }
	}
	
	return success;
    }
    
    private boolean isFile(String name) {
	String extension = null;

	int i = name.lastIndexOf('.');
	if (i > 0) {
	    extension = name.substring(i+1);
	}
	if(extension != null)
	    return true;
	return false;
    }
    
    public Directory createEntity(String name, long initialSize, OpenFile file) {
	Directory directory;

	FileHeader hdr;
	int sector;
	boolean success;

	Debug.printf('f', "Creating file %s, size %d\n", name, new Long(
		initialSize));

	directory = new Directory(NumDirEntries, this);
	directory.fetchFrom(file);
	
	if (directory.find(name) != -1)
	    success = false; // file is already in directory
	else {
	    freeMap = new BitMap(numDiskSectors);
	    freeMap.fetchFrom(freeMapFile);
	    sector = freeMap.find(); // find a sector to hold the file header
	    if (sector == -1)
		success = false; // no free block for file header
	    else if (!directory.add(name, sector))
		success = false; // no space in directory
	    else {
		hdr = new FileHeader(this);
		if (!hdr.allocate(freeMap, (int) initialSize))
		    success = false; // no space on disk for data
		else {
		    success = true;
		    // everthing worked, flush all changes back to disk
		    hdr.type = FileHeader.FILE_TYPE;
		    hdr.writeBack(sector);
		    directory.writeBack(file);
		    freeMap.writeBack(freeMapFile);
		    return directory;
		}
	    }
	}
	return null;
    }
    
    public boolean mkfile(String name, int size) {
	boolean success = false;
	String[] dirs = name.split("/");
	
	Directory directory;
	FileHeader hdr;
	int sector;
	OpenFile openFile = directoryFile;

	directory = new Directory(NumDirEntries, this);
	directory.fetchFrom(directoryFile);
	for(int i = 1; i < dirs.length; i++) {
	    sector = directory.find(dirs[i]);
	    if (sector != -1) {
		if(i == dirs.length-1) {
		    Debug.println('f', "Duplicate file");
		    return false;
		} else {
		    String entryName = directory.getName(dirs[i]);
		    if(entryName == null)
			continue;
		    if(isFile(entryName))
			continue;
		    openFile = new OpenFileReal(sector, this);
		    hdr = new FileHeader(this);
		    hdr.fetchFrom(sector);
		    directory = new Directory(NumDirEntries, this);
		    directory.fetchFrom(openFile);
		}
	    } else {
		if(i != dirs.length-1) {
		    Debug.println('f', "Invalid directory path");
		    return false;
		} else {
		    createEntity(dirs[i], size, openFile);
		    OpenFile file = openDirectory(dirs[i], openFile);
		    if (file == null) {
		        return false;
		    }
		    Directory d = new Directory(NumDirEntries, this);
		    d.writeBack(file);

		    return true;
		}
	    }
	}
	return success;
    }
    
    @Override
    public boolean rmdir(String name) {
	if(removeEntity(name)) {
	    Debug.println('f', "Successfully removed the directory " + name);
	    return true;
	}
	return false;
    }
    
    public Directory loadFile(String name) {
	String[] dirs = name.split("/");

	Directory directory;
	FileHeader hdr;
	int sector;
	OpenFile openFile = directoryFile;

	directory = new Directory(NumDirEntries, this);
	directory.fetchFrom(directoryFile);
	for (int i = 1; i < dirs.length; i++) {
	    sector = directory.find(dirs[i]);
	    if (sector != -1) {
		String entryName = directory.getName(dirs[i]);
		if (entryName == null)
		    continue;
		openFile = new OpenFileReal(sector, this);
		hdr = new FileHeader(this);
		hdr.fetchFrom(sector);
		directory = new Directory(NumDirEntries, this);
		directory.fetchFrom(directoryFile);
		if (name.lastIndexOf("/") == 0 || i == dirs.length-2)
		    return directory;
	    } else {
		if (i != dirs.length - 1) {
		    Debug.println('f', "Invalid directory path");
		    return null;
		}
	    }
	}

	return null;
    }
    
    public Directory loadFileForRmdir(String name) {
	String[] dirs = name.split("/");

	Directory directory;
	FileHeader hdr;
	int sector;
	OpenFile openFile = directoryFile;

	directory = new Directory(NumDirEntries, this);
	directory.fetchFrom(directoryFile);
	for (int i = 1; i < dirs.length; i++) {
	    sector = directory.find(dirs[i]);
	    if (sector != -1) {
		String entryName = directory.getName(dirs[i]);
		if (entryName == null)
		    continue;
		openFile = new OpenFileReal(sector, this);
		hdr = new FileHeader(this);
		hdr.fetchFrom(sector);
		directory = new Directory(NumDirEntries, this);
		directory.fetchFrom(directoryFile);
		if (i == dirs.length-2)
		    return directory;
	    } else {
		if (i != dirs.length - 1) {
		    Debug.println('f', "Invalid directory path");
		    return null;
		}
	    }
	}

	return null;
    }

    public OpenFile openFile(String name) {
	Directory directory = new Directory(NumDirEntries, this);
	OpenFile openFile = null;
	int sector;
	directory = loadFile(name);
	if(null == directory) {
	    Debug.println('f', "Not able to load file");
	    return null;
	}
	String[] dirs = name.split("/");
	Debug.printf('f', "Opening file %s\n", name);
	//directory.fetchFrom(file);
	sector = directory.find(dirs[dirs.length-1]);
	if (sector >= 0)
	    openFile = new OpenFileReal(sector, this);// name was found in directory
	return openFile; // return null if not found
    }
    
    public void start() {
	NachosThread thread = new NachosThread("Filesystem test", this);
	Nachos.scheduler.readyToRun(thread);
    }

    @Override
    public void run() {
	String directoryName = "/root";
	Debug.println('f', "Consistency test: Start");
	Debug.println('f', "Consistency test: Creating directory " + directoryName);
	if(mkdir("/root")) {
	    Debug.println('f', "Consistency test: Successfully created directory " + directoryName);
	}
	Debug.println('f', "Consistency test: listing the directory contents");
	list();
	consistencyCheck(directoryFile, "");
	Debug.println('f', "Consistency test: Removing the directory " + directoryName);
	rmdir(directoryName);
	Nachos.scheduler.finishThread();
    }
}
