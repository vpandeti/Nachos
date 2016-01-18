// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.kernel.filesys.OpenFile;
import nachos.kernel.threads.Semaphore;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.NachosThread;
import nachos.machine.Simulation;
import nachos.machine.TranslationEntry;

/**
 * Nachos system call interface. These are Nachos kernel operations that can be
 * invoked from user programs, by trapping to the kernel via the "syscall"
 * instruction.
 *
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class Syscall {

    // System call codes -- used by the stubs to tell the kernel
    // which system call is being asked for.

    /** Integer code identifying the "Halt" system call. */
    public static final byte SC_Halt = 0;

    /** Integer code identifying the "Exit" system call. */
    public static final byte SC_Exit = 1;

    /** Integer code identifying the "Exec" system call. */
    public static final byte SC_Exec = 2;

    /** Integer code identifying the "Join" system call. */
    public static final byte SC_Join = 3;

    /** Integer code identifying the "Create" system call. */
    public static final byte SC_Create = 4;

    /** Integer code identifying the "Open" system call. */
    public static final byte SC_Open = 5;

    /** Integer code identifying the "Read" system call. */
    public static final byte SC_Read = 6;

    /** Integer code identifying the "Write" system call. */
    public static final byte SC_Write = 7;

    /** Integer code identifying the "Close" system call. */
    public static final byte SC_Close = 8;

    /** Integer code identifying the "Fork" system call. */
    public static final byte SC_Fork = 9;

    /** Integer code identifying the "Yield" system call. */
    public static final byte SC_Yield = 10;

    /** Integer code identifying the "Remove" system call. */
    public static final byte SC_Remove = 11;

    /** Integer code identifying the "Sleep" system call. */
    public static final byte SC_Sleep = 12;
    
    /** Integer code identifying the "Create new directory" system call. */
    public static final byte SC_Mkdir = 13;
    
    /** Integer code identifying the "Remove directory" system call. */
    public static final byte SC_Rmdir = 14;
    
    /** Integer code identifying the "Make file" system call. */
    public static final byte SC_Mkfile = 15;

    /** Integer code identifying the "Mmap" system call. */
    public static final int SC_Mmap = 16;
    
    /** Integer code identifying the "Munmap" system call. */
    public static final int SC_Munmap = 17;
    /**
     * Stop Nachos, and print out performance stats.
     */
    public static void halt() {
	Debug.println('+', "Halt test");
	Debug.print('+', "Shutdown, initiated by user program.\n");
	Simulation.stop();
    }

    /* Address space control operations: Exit, Exec, and Join */

    /**
     * This user program is done.
     *
     * @param status
     *            Status code to pass to processes doing a Join(). status = 0
     *            means the program exited normally.
     */
    static HashMap<Integer, Integer> waitingProcessesMap = new HashMap<Integer, Integer>();

    public static void exit(int status) {
	Debug.println('+', "User program exits with status=" + status + ": "
		+ NachosThread.currentThread().name);
	AddrSpace space = ((UserThread) NachosThread.currentThread()).space;

	UserThread thread = (UserThread) NachosThread.currentThread();

	if (thread.getForked()) {
	    mSemaphoreForFork.V();
	}

	if (!thread.getIsJoiningWithParent()) {
	    // for(int i = 0; i < thread.getChildrenCount(); i++)
	    if(thread.getParent() != null)
		mSemaphoreForExecution.V();
	} else {
	    // Free the memory allocated for address space such as page table,
	    // etc.
	    CPU.writeRegister(2, status);
	}
	space.handleMunmap();
	space.destroy();
	
	UserThread parentThread = thread.getParent();
	if(parentThread != null) {
	    parentThread.removeChild(thread);
	}
	
	Nachos.scheduler.finishThread();

    }

    /**
     * Run the executable, stored in the Nachos file "name", and return the
     * address space identifier.
     *
     * @param name
     *            The name of the file to execute.
     */
    public static int exec(String name) {
	int vAddress = CPU.readRegister(4);
	String fileName = getUserProgram(NachosThread.currentThread().name,
		vAddress);
	if (fileName == "")
	    Debug.println('I', "Wrong file name");

	String execFileName = "name";
	copy(fileName, execFileName);
	
	OpenFile executable;
	if ((executable = Nachos.fileSystem.open(execFileName)) == null) {
	    Debug.println('+', "Unable to open executable file: " + fileName);
	    return -1;
	}

	AddrSpace space = new AddrSpace();
	UserThread thread = new UserThread("exec_user_program_thread",
		new Runnable() {

		    @Override
		    public void run() {
			// execute user program
			space.exec(executable);
			// set the initial register values
			space.initRegisters();
			// load page table register
			space.restoreState();
			// run user code
			CPU.runUserCode();
		    }
		}, space);
	thread.setParent((UserThread) NachosThread.currentThread());
	((UserThread) NachosThread.currentThread()).setChildren(thread);
	map.put(space.getSpaceId(), thread);
	Nachos.scheduler.readyToRun(thread);
	// returns the space id of newly created process
	return space.getSpaceId();
    }

    /**
     * Wait for the user program specified by "id" to finish, and return its
     * exit status.
     *
     * @param id
     *            The "space ID" of the program to wait for.
     * @return the exit status of the specified program.
     */
    private static HashMap<Integer, NachosThread> map = new HashMap<Integer, NachosThread>();

    static private Semaphore mSemaphoreForExecution = new Semaphore(
	    "Executing_Parties", 0);

    static private Semaphore mSemaphoreForFork = new Semaphore(
	    "Executing_Parties_Fork", 0);

    private static boolean isJoinCall = false;

    /*
     * public static int join(int id) { isJoinCall = true; NachosThread thread =
     * map.get(id); if(null != thread) { mSemaphoreForExecution.P(); }
     * CPU.writeRegister(2, 0); isJoinCall = false; return 0; }
     */

    public static int join(int id) {
	boolean isFound = false;
	UserThread thread = ((UserThread) NachosThread.currentThread());
	UserThread child = null;
	for (int i = 0; i < thread.getChildrenCount(); i++) {
	    child = thread.getChild(i);
	    if (child.getParent() != null && child.space.getSpaceId() == id
		    && child.getParent() == NachosThread.currentThread()) {
		thread.setIsJoiningWithParent(true);
		child.setIsJoinCall(true);
		isFound = true;
		break;
	    }
	}
	if (isFound) {
	    // ArrayList<Integer> processes = new ArrayList<Integer>();
	    // waitingProcessesMap.put(child.space.getSpaceId(),
	    // thread.space.getSpaceId());
	    mSemaphoreForExecution.P();
	} else {
	    return 0;
	}
	CPU.writeRegister(2, 0);
	return 0;
    }

    /*
     * File system operations: Create, Open, Read, Write, Close These functions
     * are patterned after UNIX -- files represent both files *and* hardware I/O
     * devices.
     * 
     * If this assignment is done before doing the file system assignment, note
     * that the Nachos file system has a stub implementation, which will work
     * for the purposes of testing out these routines.
     */

    // When an address space starts up, it has two open files, representing
    // keyboard input and display output (in UNIX terms, stdin and stdout).
    // Read and write can be used directly on these, without first opening
    // the console device.

    /** OpenFileId used for input from the keyboard. */
    public static final int ConsoleInput = 0;

    /** OpenFileId used for output to the display. */
    public static final int ConsoleOutput = 1;

    /**
     * Create a Nachos file with a specified name.
     *
     * @param name
     *            The name of the file to be created.
     */
    public static void create(String name) {
    }

    /**
     * Remove a Nachos file.
     *
     * @param name
     *            The name of the file to be removed.
     */
    public static void remove(String name) {
    }

    /**
     * Open the Nachos file "name", and return an "OpenFileId" that can be used
     * to read and write to the file.
     *
     * @param name
     *            The name of the file to open.
     * @return An OpenFileId that uniquely identifies the opened file.
     */
    public static int open(String name) {
	return 0;
    }

    /**
     * Write "size" bytes from "buffer" to the open file.
     *
     * @param buffer
     *            Location of the data to be written.
     * @param size
     *            The number of bytes to write.
     * @param id
     *            The OpenFileId of the file to which to write the data.
     */
    public static void write(byte buffer[], int size, int id) {
	if (id == ConsoleOutput) {
	    for (int i = 0; i < size; i++) {
		Nachos.consoleDriver.putCharWithBuffer((char) buffer[i], i == size-1);
	    }
	}
    }

    /**
     * Read "size" bytes from the open file into "buffer". Return the number of
     * bytes actually read -- if the open file isn't long enough, or if it is an
     * I/O device, and there aren't enough characters to read, return whatever
     * is available (for I/O devices, you should always wait until you can
     * return at least one character).
     *
     * @param buffer
     *            Where to put the data read.
     * @param size
     *            The number of bytes requested.
     * @param id
     *            The OpenFileId of the file from which to read the data.
     * @return The actual number of bytes read.
     */
    public static int read(byte buffer[], int size, int id) {
	int i = 0;
	/*char[] cBuffer = new char[size];
	if (id == ConsoleInput) {
	    for (; i < size; i++) {
		char ch = Nachos.consoleDriver.getChar();
		//Nachos.consoleDriver.putChar(ch);
		buffer[i] = (byte) (ch);
		cBuffer[i] = Nachos.consoleDriver.getChar();
	    }
	}
	for (i = 0; i < size; i++) {
	    Nachos.consoleDriver.putChar(cBuffer[i]);
	}*/
	//Nachos.consoleDriver.getChar();
	if (id == ConsoleInput) {
	    for (; i < size; i++) {
		Debug.println('+', "Typed character is: " + Nachos.consoleDriver.getChar());
	    }
	}
	return i;
    }

    /**
     * Close the file, we're done reading and writing to it.
     *
     * @param id
     *            The OpenFileId of the file to be closed.
     */
    public static void close(int id) {
    }

    /*
     * User-level thread operations: Fork and Yield. To allow multiple threads
     * to run within a user program.
     */

    /**
     * Fork a thread to run a procedure ("func") in the *same* address space as
     * the current thread.
     *
     * @param func
     *            The user address of the procedure to be run by the new thread.
     */
    private static boolean mIsForkInProgress = false;

    public static void fork(int func) {
	AddrSpace space = ((UserThread) NachosThread.currentThread()).space
		.clone();
	UserThread thread = new UserThread("fork_user_thread", new Runnable() {

	    @Override
	    public void run() {
		PhysicalMemoryManager.getInstance().mapProcessIdToAddressSpace(
			NachosThread.currentThread().name, space);
		space.restoreState();
		CPU.writeRegister(MIPS.PCReg, func);
		CPU.writeRegister(MIPS.NextPCReg, func + 4);
		((UserThread) NachosThread.currentThread()).setForked();
		CPU.runUserCode();
	    }
	}, space);
	thread.setParent((UserThread) NachosThread.currentThread());
	((UserThread) NachosThread.currentThread()).setChildren(thread);
	Nachos.scheduler.readyToRun(thread);
	mSemaphoreForFork.P();
	Debug.println('+', "Main thread is exiting after fork is exited");
	// Nachos.scheduler.yieldThread();
    }

    /**
     * Yield the CPU to another runnable thread, whether in this address space
     * or not.
     */
    public static void yield() {
	Debug.println('+', "Yielding the current thread to another thread");
	Nachos.scheduler.yieldThread();
    }
    
    public static void sleep(int sleepTime) {
	Nachos.scheduler.sleep(sleepTime);
    }

    protected static TranslationEntry getEntry(int vPage,
	    TranslationEntry[] entries) {
	TranslationEntry result = entries[vPage];
	if (result == null)
	    return null;
	return result;
    }

    /**
     * Reads memory to get the name of the file. Takes virtual address, Goes to
     * memory and then loops till null bytes is found. Copies the chunk of bytes
     * (star position: virtual address, end position: next null byte). Copies
     * the chunk into another byte array. Converts the byte array into String.
     * 
     * @param pId
     *            Process Id to fetch the corresponding address space
     * @param vAddress
     *            Virtual address of the user program in the memory
     * @param data
     * @param maxStringlength
     * @return
     */
    private static String getUserProgram(String pId, int vAddress) {
	byte[] data = new byte[256];
	PhysicalMemoryManager pmm = PhysicalMemoryManager.getInstance();
	AddrSpace space = pmm.getAddressSpaceFromProcessId(pId);
	int vpn = vAddress / Machine.PageSize;
	TranslationEntry entry = getEntry(vpn, space.getPageTable());
	if (null == entry)
	    return null;
	byte[] memory = Machine.mainMemory;
	int ppn = entry.physicalPage;
	int pAddress = ppn * Machine.PageSize;
	int offset = vAddress % Machine.PageSize;
	int source = pAddress + offset;
	int i = source;
	for (; i < memory.length; i++) {
	    if (memory[i] == 0x00)
		break;
	}
	byte[] d = new byte[i - source];
	System.arraycopy(memory, source, d, 0, i - source);
	/*
	 * for (int j = 0; j < i-source; j++) { if(data[j] == 0) break; d[j] =
	 * data[j]; }
	 */
	return new String(d);
    }
    
    public static void mkrmdir(boolean mkdir) {
	int vAddress = CPU.readRegister(4);
	String name = getUserProgram(NachosThread.currentThread().name,
		vAddress);
	if (name == "")
	    Debug.println('I', "Wrong file name");
	if(mkdir)
	    if(Nachos.fileSystem.mkdir(name))
		Debug.println('f', "Succesfully created directory " + name);
	    else
		Debug.println('f', "Failed in creating directory " + name);
	else
	    Nachos.fileSystem.rmdir(name);
	Nachos.fileSystem.list();
    }
    
    public static void mkFile() {
	int vAddress = CPU.readRegister(4);
	String name = getUserProgram(NachosThread.currentThread().name,
		vAddress);
	
	Nachos.fileSystem.mkfile(name, (int) 128);
	OpenFile file = Nachos.fileSystem.openFile(name);
	if(null != file) {
	    File fp;
	    FileInputStream fs = null;
	    fp = new File("test/test.txt");
	    if (!fp.exists()) {
		return;
	    }
	    byte[] buffer = new byte[128];
	    try {
		int amountRead = 0;
		fs = new FileInputStream(fp);
		int index = 0;
		while ((amountRead = fs.read(buffer)) > 0) {
		    file.write(buffer, index, amountRead);
		    index += amountRead;
		}
		file = Nachos.fileSystem.openFile(name);
		byte[] b = new byte[3840];
		file.read(b, 0, 3840);
		Debug.println('f', new String(b));
	    } catch (IOException e) {
		Debug.print('+', "Copy: data copy failed\n");
		return;
	    } finally {
		try {
		    fs.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}
    }
    
    public static void Mmap() {
	int vAddress = CPU.readRegister(4);
	String fileName = getUserProgram(NachosThread.currentThread().name,
		vAddress);
	fileName = "/" + fileName;
	//Nachos.fileSystem.create(fileName, 1024);
	OpenFile file = Nachos.fileSystem.openFile(fileName);
	if(null == file) {
	    Debug.println('+', "File is not copied into the filesystem");
	    return;
	}
	int length = (int) file.length();
	AddrSpace space = ((UserThread) NachosThread.currentThread()).space;
	int pAddress = space.handleMmap(length, fileName, file);
	CPU.writeRegister(2, pAddress);
    }
    
    public static void Munmap() {
	int vAddress = CPU.readRegister(4);
	AddrSpace space = ((UserThread) NachosThread.currentThread()).space;
	space.handleMunmap(vAddress);
    }
    
    private static void copy(String from, String to) {
	File fp;
	FileInputStream fs;
	OpenFile openFile;
	int amountRead;
	long fileLength;
	byte buffer[];

	// Open UNIX file
	fp = new File(from);
	if (!fp.exists()) {
	    Debug.printf('+', "Copy: couldn't open input file %s\n", from);
	    return;
	}

	// Figure out length of UNIX file
	fileLength = fp.length();

	// Create a Nachos file of the same length
	Debug.printf('f', "Copying file %s, size %d, to file %s\n", from,
		new Long(fileLength), to);
	if (!Nachos.fileSystem.create(to, (int) fileLength)) {
	    // Create Nachos file
	    Debug.printf('+', "Copy: couldn't create output file %s\n", to);
	    return;
	}

	openFile = Nachos.fileSystem.open(to);
	Debug.ASSERT(openFile != null);

	// Copy the data in TransferSize chunks
	buffer = new byte[128];
	try {
	    fs = new FileInputStream(fp);
	    while ((amountRead = fs.read(buffer)) > 0)
		openFile.write(buffer, 0, amountRead);
	} catch (IOException e) {
	    Debug.print('+', "Copy: data copy failed\n");
	    return;
	}
	// Close the UNIX and the Nachos files
	// delete openFile;
	try {
	    fs.close();
	} catch (IOException e) {
	}
  }
}
