package nachos.kernel.threads;

import nachos.Debug;
import nachos.machine.Machine;
import nachos.machine.Releasable;
import nachos.machine.TestAndSetVariable;

/**
 * This class implements "spin locks" for multiprocessor synchronization.
 * 
 * @author Eugene W. Stark
 * @version 20140106
 */

public class SpinLock implements Releasable {
    
    /** The underlying shared memory location with an atomic test-and-set operation. */
    private TestAndSetVariable<Boolean> shared;
    
    /** Name of this lock, for debugging. */
    private final String name;
    
    /**
     * Initialize a spin lock.
     * 
     * @param name  Name of the lock, for debugging.
     */
    public SpinLock(String name) {
	this.name = name;
	shared = new TestAndSetVariable<Boolean>();
    }
    
    /**
     * Acquire this spin lock.
     */
    public void acquire() {
	if(Machine.NUM_CPUS > 1) {
	    Debug.printf('s', "Acquiring spin lock: %s\n", name);

	    while(shared.testAndSet(true) != null)
		/* spin */;

	    Debug.printf('s', "Acquired spin lock: %s\n", name);
	}
    }
    
    /**
     * Release this spin lock.
     */
    public void release() {
	if(Machine.NUM_CPUS > 1) {
	    Debug.printf('s', "Releasing spin lock: %s\n", name);

	    shared.reset();
	}
    }
    
    /**
     * Determine if this spin lock is currently locked.
     * 
     * @return true if this spin lock is currently locked, false otherwise.
     * Note that a return value of true does not imply that the currently executing
     * activity is the one that acquired the spin lock, only that the spin lock was
     * in fact acquired.  Also, there is nothing that stops any activity from releasing
     * a spin lock, even if that activity was not the one that acquired the lock.
     * So the return value of this method only tells you the state of the spin lock
     * at the time the method was called, and does not necessarily imply that the
     * state has not subsequently changed.  This method is therefore useful only for
     * debugging assertions that check preconditions for methods that assume a spin lock
     * has been acquired when they are called, and should not be used for other purposes.
     */
    public boolean isLocked() {
	if(Machine.NUM_CPUS > 1) {
	    return shared.getValue() != null;
	} else {
	    return true;
	}
    }

}
