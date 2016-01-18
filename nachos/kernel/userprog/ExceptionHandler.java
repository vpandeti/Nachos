// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog;

import nachos.Debug;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.MachineException;
import nachos.machine.NachosThread;
import nachos.machine.TranslationEntry;
import nachos.kernel.Nachos;
import nachos.kernel.userprog.Syscall;

/**
 * An ExceptionHandler object provides an entry point to the operating system
 * kernel, which can be called by the machine when an exception occurs during
 * execution in user mode.  Examples of such exceptions are system call
 * exceptions, in which the user program requests service from the OS,
 * and page fault exceptions, which occur when the user program attempts to
 * access a portion of its address space that currently has no valid
 * virtual-to-physical address mapping defined.  The operating system
 * must register an exception handler with the machine before attempting
 * to execute programs in user mode.
 */
public class ExceptionHandler implements nachos.machine.ExceptionHandler {

  /**
   * Entry point into the Nachos kernel.  Called when a user program
   * is executing, and either does a syscall, or generates an addressing
   * or arithmetic exception.
   *
   * 	For system calls, the following is the calling convention:
   *
   * 	system call code -- r2,
   *		arg1 -- r4,
   *		arg2 -- r5,
   *		arg3 -- r6,
   *		arg4 -- r7.
   *
   *	The result of the system call, if any, must be put back into r2. 
   *
   * And don't forget to increment the pc before returning. (Or else you'll
   * loop making the same system call forever!)
   *
   * @param which The kind of exception.  The list of possible exceptions 
   *	is in CPU.java.
   *
   * @author Thomas Anderson (UC Berkeley), original C++ version
   * @author Peter Druschel (Rice University), Java translation
   * @author Eugene W. Stark (Stony Brook University)
   */
    public void handleException(int which) {
	int type = CPU.readRegister(2);

	if (which == MachineException.SyscallException) {
	    int ptr;
	    int len;
	    byte buf[];
	    
	    switch (type) {
	    case Syscall.SC_Halt:
		Syscall.halt();
		break;
	    case Syscall.SC_Exit:
		Syscall.exit(CPU.readRegister(4));
		break;
	    case Syscall.SC_Exec:
		int spaceId = Syscall.exec("");
		CPU.writeRegister(2, spaceId);
		break;
	    case Syscall.SC_Fork:
		Syscall.fork(CPU.readRegister(4));
		break;
	    case Syscall.SC_Yield:
		Syscall.yield();
		break;
	    case Syscall.SC_Join:
		Syscall.join(CPU.readRegister(4));
		break;	
	    case Syscall.SC_Write:
		ptr = CPU.readRegister(4);
		len = CPU.readRegister(5);
		buf = new byte[len];
		readMemory(ptr, len, buf);
		Syscall.write(buf, len, CPU.readRegister(6));
		break;
	    case Syscall.SC_Open:
		break;
	    case Syscall.SC_Read:
		ptr = CPU.readRegister(4);
		len = CPU.readRegister(5);
		buf = new byte[len];
		int read = Syscall.read(buf, len, CPU.readRegister(6));
		for(int i = 0; i < len; i++) {
		    Machine.mainMemory[ptr++] = buf[i];
		}
		CPU.writeRegister(2, read);
		break;
	    case Syscall.SC_Sleep:
		int sleepTime = CPU.readRegister(4);
		if (sleepTime <= 0)
		    Debug.println('v', "Invalid sleep time");
		Syscall.sleep(sleepTime);
		break;
	    case Syscall.SC_Mkdir:
		Syscall.mkrmdir(true);
		break;
	    case Syscall.SC_Rmdir:
		Syscall.mkrmdir(false);
		break;
	    case Syscall.SC_Mkfile:
		Syscall.mkFile();
		break;
	    case Syscall.SC_Mmap:
		Syscall.Mmap();
		break;
	    case Syscall.SC_Munmap:
		Syscall.Munmap();
		break;
	    }

	    // Update the program counter to point to the next instruction
	    // after the SYSCALL instruction.
	    CPU.writeRegister(MIPS.PrevPCReg,
		    CPU.readRegister(MIPS.PCReg));
	    CPU.writeRegister(MIPS.PCReg,
		    CPU.readRegister(MIPS.NextPCReg));
	    CPU.writeRegister(MIPS.NextPCReg,
		    CPU.readRegister(MIPS.NextPCReg)+4);
	    return;
	} else if (which == MachineException.PageFaultException) {
	    int vAddress = CPU.readRegister(39);
	    Debug.println('+', "Page fault occurred, Virtual address: " + vAddress);
	    AddrSpace space = ((UserThread) NachosThread.currentThread()).space;
	    space.handlePageFault(vAddress);
	    return;
	} else if (which == MachineException.ReadOnlyException) {
	    int vAddress = CPU.readRegister(39);
	    Debug.println('+', "Read only exception - Handle it, Virtual address: " + vAddress);
	    AddrSpace space = ((UserThread) NachosThread.currentThread()).space;
	    space.handleReadOnlyException(vAddress);
	    return;
	}
	System.out.println("Unexpected user mode exception " + which +
		", " + type);
	Debug.ASSERT(false);

    }
    
    protected static TranslationEntry getEntry(int vPage, TranslationEntry[] entries) {
	PhysicalMemoryManager pmm = PhysicalMemoryManager.getInstance();
	if (vPage < 0 || vPage >= pmm.getMaxPages())
		return null;
	TranslationEntry result = entries[vPage];
	if (result == null)
	    return null;
	int[] physicalPages = pmm.getPages();
	if(result.physicalPage < 0)
	    return null;
	if(physicalPages[result.physicalPage] == 0)
	    return null;
	return result;
    }
    
    protected static void readMemory(int ptr, int len, byte[] buf) {
	int vpn = ptr / Machine.PageSize;
	AddrSpace space = ((UserThread)NachosThread.currentThread()).space;
	TranslationEntry entry = getEntry(vpn, space.getPageTable());
	if(null == entry)
	    return;
	int ppn = entry.physicalPage;
	int pAddress = ppn * Machine.PageSize;
	int offset = ptr % Machine.PageSize;
	int source = pAddress + offset;
	System.arraycopy(Machine.mainMemory, source, buf, 0, len);
    }
}
