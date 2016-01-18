package nachos.kernel.threads;

import java.util.ArrayList;
import java.util.List;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.machine.NachosThread;

/**
 * A <CODE>CyclicBarrier</CODE> is an object that allows a set of threads to all
 * wait for each other to reach a common barrier point. To find out more, read
 * <A HREF=
 * "http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/CyclicBarrier.html"
 * >the documentation</A> for the Java API class
 * <CODE>java.util.concurrent.CyclicBarrier</CODE>.
 *
 * The class is provided in skeletal form. It is your job to complete the
 * implementation of this class in conformance with the Javadoc specifications
 * provided for each method. The method signatures specified below must not be
 * changed. However, the implementation will likely require additional private
 * methods as well as (private) instance variables. Also, it is essential to
 * provide proper synchronization for any data that can be accessed
 * concurrently. You may use ONLY semaphores for this purpose. You may NOT
 * disable interrupts or use locks or spinlocks.
 *
 * NOTE: The skeleton below reflects some simplifications over the version of
 * this class in the Java API.
 */
public class CyclicBarrier {

    // Number of parties
    private int mParties = 0;

    private int mNoOfWaitingParties = 0;
    // Barrier action to be executed when barrier is tripped
    private Runnable mBarrierAction = null;

    private boolean mIsBroken = false;

    private Semaphore mSemaphoreForWaitingParties = null;
    private Semaphore mSemaphoreForExecution = null;
    private Semaphore mSemaphoreForReset = null;
    
    List<String> mThreads = new ArrayList<String>();

    /** Class of exceptions thrown in case of a broken barrier. */
    public static class BrokenBarrierException extends Exception {
    }

    /**
     * Creates a new CyclicBarrier that will trip when the given number of
     * parties (threads) are waiting upon it, and does not perform a predefined
     * action when the barrier is tripped.
     *
     * @param parties
     *            The number of parties.
     */
    public CyclicBarrier(int parties) {
	if (parties <= 0)
	    throw new IllegalArgumentException();
	this.mParties = parties;
	mNoOfWaitingParties = parties;
	mSemaphoreForWaitingParties = new Semaphore("Waiting_Parties", 1);
	mSemaphoreForExecution = new Semaphore("Executing_Parties", 0);
	mSemaphoreForReset = new Semaphore("Reset_Barrier", 1);
    }

    /**
     * Creates a new CyclicBarrier that will trip when the given number of
     * parties (threads) are waiting upon it, and which will execute the given
     * barrier action when the barrier is tripped, performed by the last thread
     * entering the barrier.
     *
     * @param parties
     *            The number of parties.
     * @param barrierAction
     *            An action to be executed when the barrier is tripped,
     *            performed by the last thread entering the barrier.
     */
    public CyclicBarrier(int parties, Runnable barrierAction) {
	this.mParties = parties;
	mNoOfWaitingParties = parties;
	this.mBarrierAction = barrierAction;
	mSemaphoreForWaitingParties = new Semaphore("Waiting_Parties", 1);
	mSemaphoreForExecution = new Semaphore("Executing_Parties", 0);
	mSemaphoreForReset = new Semaphore("Reset_Barrier", 1);
    }

    /**
     * Waits until all parties have invoked await on this barrier. If the
     * current thread is not the last to arrive then it blocks until either the
     * last thread arrives or some other thread invokes reset() on this barrier.
     *
     * @return The arrival index of the current thread, where index getParties()
     *         - 1 indicates the first to arrive and zero indicates the last to
     *         arrive.
     * @throws BrokenBarrierException
     *             in case this barrier is broken.
     * @throws InterruptedException
     */
    public int await() throws BrokenBarrierException, InterruptedException {
	// Check is barrier is tripped
	if (mIsBroken)
	    throw new BrokenBarrierException();
	
	// Access is given only to one thread at a time, Semaphore with 1 permit
	mSemaphoreForWaitingParties.P();

	// If max no of parties arrived, break the barrier and run the barrier command if not null
	if (mNoOfWaitingParties == 1) {
	    for(int i = 0; i < mParties-1; i++) {
		mSemaphoreForExecution.V();
	    }
	    mIsBroken = true;
	    if (null != mBarrierAction)
		mBarrierAction.run();
	} else {
	    --mNoOfWaitingParties;
	}

	// Release the Semaphore with 1 permit
	mSemaphoreForWaitingParties.V();
	if(!mIsBroken)
	    // Semaphore with 0 permit. Makes threads wait at the barrier
	    mSemaphoreForExecution.P();
	return mNoOfWaitingParties;
    }

    /**
     * Returns the number of parties currently waiting at the barrier.
     * 
     * @return the number of parties currently waiting at the barrier.
     */
    public int getNumberWaiting() {
	return mParties - mNoOfWaitingParties;
    }

    /**
     * Returns the number of parties required to trip this barrier.
     * 
     * @return the number of parties required to trip this barrier.
     */
    public int getParties() {
	return mParties;
    }

    /**
     * Queries if this barrier is in a broken state.
     * 
     * @return true if this barrier was reset while one or more threads were
     *         blocked in await(), false otherwise.
     */
    public boolean isBroken() {
	return mIsBroken;
    }

    /**
     * Resets the barrier to its initial state.
     */
    public void reset() {
	mSemaphoreForReset.P();
	mIsBroken = false;
	mNoOfWaitingParties = mParties;
	mSemaphoreForReset.V();
    }

    /**
     * This method can be called to simulate "doing work". Each time it is
     * called it gives control to the NACHOS simulator so that the simulated
     * time can advance by a few "ticks".
     */
    public static void allowTimeToPass() {
	dummy.P();
	dummy.V();
    }

    /** Semaphore used by allowTimeToPass above. */
    private static Semaphore dummy = new Semaphore("Time waster", 1);

    /**
     * Run a demonstration of the CyclicBarrier facility.
     * 
     * @param args
     *            Arguments from the "command line" that can be used to modify
     *            features of the demonstration such as the number of parties,
     *            the amount of "work" performed by each thread, etc.
     *
     *            IMPORTANT: Be sure to test your demo with the "-rs xxxxx"
     *            command-line option passed to NACHOS (the xxxxx should be
     *            replaced by an integer to be used as the seed for NACHOS'
     *            pseudorandom number generator). If you fail to include this
     *            option, then a thread that has been started will always run to
     *            completion unless it explicitly yields the CPU. This will
     *            result in the same (very uninteresting) execution each time
     *            NACHOS is run, which will not be a very good test of your
     *            code.
     */
    public static void demo(String[] args) {
	// Very simple example of the intended use of the CyclicBarrier
	// facility: you should replace this code with something much
	// more interesting.
	int noOfParties = 5;
	int totalWork = 5;
	noOfParties = Integer.valueOf(args[0]);
	totalWork = Integer.valueOf(args[1]);
	final int t = totalWork;
	final CyclicBarrier barrier = new CyclicBarrier(5);
	Debug.println('1', "Demo starting");
	for(int i = 0; i < noOfParties; i++) {
	    NachosThread thread =
		new NachosThread
		("Worker thread " + i, new Runnable() {
		    public void run() {
			Debug.println('1', "Thread "
				+ NachosThread.currentThread().name
				+ " is starting");
			for(int j = 0; j < 3; j++) {
			    Debug.println('1', "Thread "
					  + NachosThread.currentThread().name
					  + " beginning phase " + j);
			    for(int k = 0; k < t; k++) {
				Debug.println('1', "Thread "
					+ NachosThread.currentThread().name
					+ " is working");
				CyclicBarrier.allowTimeToPass();  // Do "work".
			    }
			    Debug.println('1', "Thread "
				    + NachosThread.currentThread().name
				    + " is waiting at the barrier");
			    try {
				barrier.await();
			    } catch (BrokenBarrierException e) {
				//e.printStackTrace();
			    } catch (InterruptedException e) {
				//e.printStackTrace();
			    }
			    Debug.println('1', "Thread "
				    + NachosThread.currentThread().name
				    + " has finished phase " + j);
			}
			Debug.println('1', "Thread "
				+ NachosThread.currentThread().name
				+ " is terminating");
			Nachos.scheduler.finishThread();
		    }
		});
	    Nachos.scheduler.readyToRun(thread);
	}
	Debug.println('1', "Demo terminating");
    }
}
