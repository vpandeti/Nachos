// UserThread.java
//	A UserThread is a NachosThread extended with the capability of
//	executing user code.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog;

import java.util.ArrayList;
import java.util.List;

import nachos.kernel.threads.Condition;
import nachos.kernel.threads.Lock;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.NachosThread;

/**
 * A UserThread is a NachosThread extended with the capability of
 * executing user code.  It is kept separate from AddrSpace to provide
 * for the possibility of having multiple UserThreads running in a
 * single AddrSpace.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class UserThread extends NachosThread {

    /** The context in which this thread will execute. */
    public final AddrSpace space;
    private boolean mForked = false;
    // A thread running a user program actually has *two* sets of 
    // CPU registers -- one for its state while executing user code,
    // and one for its state while executing kernel code.
    // The kernel registers are managed by the super class.
    // The user registers are managed here.

    /** User-level CPU register state. */
    private int userRegisters[] = new int[MIPS.NumTotalRegs];

    /**
     * Initialize a new user thread.
     *
     * @param name  An arbitrary name, useful for debugging.
     * @param runObj Execution of the thread will begin with the run()
     * method of this object.
     * @param addrSpace  The context to be installed when this thread
     * is executing in user mode.
     */
    public UserThread(String name, Runnable runObj, AddrSpace addrSpace) {
	super(name, runObj);
	space = addrSpace;
    }

    /**
     * Save the CPU state of a user program on a context switch.
     */
    @Override
    public void saveState() {
	// Save state associated with the address space.
	space.saveState();  

	// Save user-level CPU registers.
	for (int i = 0; i < MIPS.NumTotalRegs; i++)
	    userRegisters[i] = CPU.readRegister(i);

	// Save kernel-level CPU state.
	super.saveState();
    }

    /**
     * Restore the CPU state of a user program on a context switch.
     */
    @Override
    public void restoreState() {
	// Restore the kernel-level CPU state.
	super.restoreState();

	// Restore the user-level CPU registers.
	for (int i = 0; i < MIPS.NumTotalRegs; i++)
	    CPU.writeRegister(i, userRegisters[i]);

	// Restore state associated with the address space.
	space.restoreState();
    }
    
    List<UserThread> children = new ArrayList<UserThread>();
    public void setChildren(UserThread thread) {
	children.add(thread);
    }
    
    public int getChildrenCount() {
	return children.size();
    }
    
    public UserThread getChild(int index) {
	return children.get(index);
    }
    
    UserThread parentThread = null;
    public void setParent(UserThread thread) {
	parentThread = thread;
    }
    
    public UserThread getParent() {
	return parentThread;
    }
    
    public void removeChild(UserThread thread) {
	for(int i = 0; i < children.size(); i++) {
	    if(children.get(i) == thread) {
		children.remove(thread);
		break;
	    }
	}
    }
    
    boolean isJoiningWithParent = false;
    boolean isJoinCall = false;
    public boolean getIsJoiningWithParent() {
	return isJoiningWithParent;
    }
    
    public void setIsJoiningWithParent(boolean isJoining) {
	isJoiningWithParent = isJoining;
    }
    
    public void setIsJoinCall(boolean isJoinCall) {
	this.isJoinCall = isJoinCall;
    }
    
    public boolean getIsJoinCall() {
	return isJoinCall;
    }
    
    public void setForked() {
	mForked = true;
    }
    
    public boolean getForked() {
	return mForked;
    }
	  
}
