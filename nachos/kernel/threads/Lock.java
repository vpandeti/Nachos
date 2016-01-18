// Lock.java
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.threads;

import nachos.machine.NachosThread;
import nachos.Debug;

/**
 * This class defines a "lock".  A lock can be BUSY or FREE.
 * There are only two operations allowed on a lock: 
 *
 *	Acquire -- wait until the lock is FREE, then set it to BUSY.
 *
 *	Release -- set lock to be FREE, waking up a thread waiting
 *		in Acquire if necessary.
 *
 * In addition, by convention, only the thread that acquired the lock
 * may release it.  As with semaphores, you can't read the lock value
 * (because the value might change immediately after you read it).  
 * 
 * NOTE: An implementation of locks and condition variables was not part of
 * the original C++ version of Nachos -- it was part of the student assignments.
 * As far as I know, this implementation originated in Peter Druschel's Java
 * version.
 * 
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class Lock {

    /** Printable name useful for debugging. */
    public final String name;

    /** semaphore used for implementation of lock. */
    private Semaphore sem;

    /** Which thread currently holds this lock? */
    private volatile NachosThread owner;

    /**
     * Initialize a lock.
     *
     *	@param debugName An arbitrary name, useful for debugging.
     */
    public Lock(String debugName) {
	name = debugName;
	sem = new Semaphore("Semaphore for lock \"" + debugName + "\"", 1);
	owner = null;
    }

    /**
     * Wait until the lock is "free", then set the lock to "busy".
     */
    public void acquire() {

	Debug.printf('s', "Acquiring lock %s for thread %s\n",
		name, NachosThread.currentThread().name);

	sem.P();
	owner = NachosThread.currentThread();

	Debug.printf('s', "Acquired lock %s for thread %s\n",
		name, NachosThread.currentThread().name);
    }

    /**
     * Release the lock that was previously acquired, waking up a
     * waiting thread if necessary.
     */
    public void release() {

	Debug.ASSERT((NachosThread.currentThread() == owner),
		"A thread that doesn't own the lock tried to " +
		"release it!\n");
	Debug.printf('s', "Thread %s dropping lock %s\n",
		NachosThread.currentThread().name, name);

	owner = null;
	sem.V();

	Debug.printf('s', "Thread %s dropped lock %s\n",
		NachosThread.currentThread().name, name);
    }

    /**
     * A predicate that determines whether or not the lock is held by the
     * current thread.  Used for sanity checks in condition variables.
     */
    public boolean isHeldByCurrentThread()
    {
	return (owner != null && owner == NachosThread.currentThread());
    }

}
