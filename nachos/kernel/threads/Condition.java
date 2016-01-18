// Condition.java
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.threads;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.machine.CPU;
import nachos.machine.NachosThread;
import nachos.util.FIFOQueue;
import nachos.util.Queue;

/**
 * This class defines a "condition variable".  A condition
 * variable does not have a value, but threads may be queued, waiting
 * on the variable.  The following are the only operations on a condition
 * variable: 
 *
 *	await() -- release the lock, relinquish the CPU until signaled, 
 *		then re-acquire the lock
 *
 *	signal() -- wake up a thread, if there are any waiting on 
 *		the condition
 *
 *	broadcast() -- wake up all threads waiting on the condition
 *
 * All operations performed by a thread on a condition variable must be made
 * while the thread is holding a lock.  Indeed, all accesses
 * to a given condition variable must be protected by the same lock.
 * In other words, mutual exclusion must be enforced among threads calling
 * the condition variable operations.  The await() method must only be
 * called from a thread context.  However, signal() and broadcast() may also
 * be called from an interrupt handler.  In that case, it is not necessary
 * (and not possible) for the caller to be holding a lock.
 *
 * In Nachos, condition variables are assumed to obey *Mesa*-style
 * semantics.  When a Signal or Broadcast wakes up another thread,
 * it simply puts the thread on the ready list, and it is the responsibility
 * of the woken thread to re-acquire the lock (this re-acquire is
 * taken care of within await()).  By contrast, some define condition
 * variables according to *Hoare*-style semantics -- where the signalling
 * thread gives up control over the lock and the CPU to the woken thread,
 * which runs immediately and gives back control over the lock to the 
 * signaller when the woken thread leaves the critical section.
 *
 * The consequence of using Mesa-style semantics is that some other thread
 * can acquire the lock, and change data structures, before the woken
 * thread gets a chance to run.  This generally means that waiting on
 * a condition variable has to occur within a loop that tests whether the
 * reason for waiting is no longer present before proceeding.
 * 
 * NOTE: An implementation of locks and condition variables was not part of
 * the original C++ version of Nachos -- it was part of the student assignments.
 * As far as I know, this implementation originated in Peter Druschel's Java
 * version.
 * 
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class Condition {

    /** Printable name useful for debugging. */
    public final String name;

    /** The lock associated with this condition. */
    private final Lock conditionLock;

    /** Who's waiting on this condition? */
    private final Queue<NachosThread> waitingThreads;

    /**
     * Spin lock used to obtain exclusive access to condition state
     * in a multiprocessor setting.
     */
    private final SpinLock spinLock;

    /**
     * Initialize a new condition variable.
     *
     * @param debugName An arbitrary name, useful for debugging.
     * @param lock A lock to be associated with this condition.
     */
    public Condition(String debugName, Lock lock) {
	name = debugName;
	conditionLock = lock;;
	waitingThreads = new FIFOQueue<NachosThread>();
	spinLock = new SpinLock(name + " spin lock");
    }

    /**
     * Accessor to obtain the lock associated with a condition.
     *
     * @return the lock associated with this condition.
     */
    public Lock getLock() {
	return(conditionLock);
    }

    /**
     * Wait on a condition until signalled.  The caller must hold the
     * lock associated with the condition.  The lock is released, and
     * the caller relinquishes the CPU until it is signalled by another
     * thread that calls signal or broadcast on the same condition.
     */
    public void await() {
	Debug.ASSERT(conditionLock.isHeldByCurrentThread(),
		"Non-owner tried to manipulate condition variable.");
	Debug.printf('s', "Thread %s waiting on condition variable %s\n",
		NachosThread.currentThread().name, name);

	int oldLevel = CPU.setLevel(CPU.IntOff);	// disable interrupts
	spinLock.acquire();				// exclude other CPUs

	waitingThreads.offer(NachosThread.currentThread());
	conditionLock.release();
	Nachos.scheduler.sleepThread(spinLock);

	CPU.setLevel(oldLevel);			// and re-enable interrupts.

	Debug.printf('s', "Trying to reacquire condition %s's lock (%s) for " +
		"thread %s\n", name, conditionLock.name,
		NachosThread.currentThread().name);

	conditionLock.acquire();

	Debug.printf('s', "Reacquired condition %s's lock (%s) for " +
		"thread %s\n", name, conditionLock.name,
		NachosThread.currentThread().name);
    }

    /**
     * Wake up a thread, if any, that is waiting on the condition.
     */
    public void signal() {
	Debug.ASSERT(NachosThread.currentThread() == null || conditionLock.isHeldByCurrentThread(),
		"Can't signal unless we own the lock!");
	Debug.printf('s', "Signalling condition %s\n", name);

	int oldLevel = CPU.setLevel(CPU.IntOff);
	spinLock.acquire();

	NachosThread newThread = waitingThreads.poll();
	if (newThread != null) {
	    Debug.printf('s', "Waking up thread %s\n", newThread.name);
	    Nachos.scheduler.readyToRun(newThread);
	}

	spinLock.release();
	CPU.setLevel(oldLevel);
    }    

    /**
     * Wake up all threads waiting on the condition.
     */
    public void broadcast() {
	Debug.ASSERT(NachosThread.currentThread() == null || conditionLock.isHeldByCurrentThread(),
		"Can't broadcast unless we own the lock!");
	Debug.printf('s', "Broadcasting condition %s\n", name);

	int oldLevel = CPU.setLevel(CPU.IntOff);
	spinLock.acquire();

	NachosThread newThread = waitingThreads.poll();
	while (newThread != null) {
	    Debug.printf('s', "Waking thread %s\n", newThread.name);
	    Nachos.scheduler.readyToRun(newThread);
	    newThread = waitingThreads.poll();
	}

	spinLock.release();
	CPU.setLevel(oldLevel);
    }
}
