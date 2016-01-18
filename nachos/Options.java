// Usage: 
// run <options> ("run" is a simple script; edit it for your platform), or:
// on Windows: java -classpath 'machine.jar;.' nachos.kernel.Nachos <options>
// on Unix:    java -classpath 'machine.jar:.' nachos.kernel.Nachos <options>
// where <options> are:
//
//  GENERAL
//    -d <flags> causes debugging messages to be printed (see Debug.java)
//    -rs <seed> causes yield to occur at pseudo-random points during
//         execution.  <seed> is the seed to a pseudo-random number generator.
//         Re-execution with the same seed should produce the same results.
//    -tl <time limit> halt the machine if totalTicks exceeds <time limit>
//    -z prints the copyright message
//
//  USER_PROGRAM (set USER_PROGRAM=true below before using these options!)
//    -s causes user programs to be executed in single-step mode
//    -x <nachos file> runs a user program
//    -c <consoleIn> <consoleOut> tests the console
//         if omitted, consoleIn and consoleOut default to stdin and stdout
//
//  FILESYS (set FILESYS=true below before using these options!)
//    -f causes the physical disk to be formatted
//    -cp <unix file> <nachos file> copies a file from UNIX to Nachos
//    -p <nachos file> prints a Nachos file to stdout
//    -r <nachos file> removes a Nachos file from the file system
//    -l lists the contents of the Nachos directory
//    -D prints the contents of the entire file system 
//    -t tests the performance of the Nachos file system
//
//  NETWORK (set NETWORK=true below before using these options!)
//    -n <reliability>  sets the network reliability -- currently unsupported
//    -m <machine id> sets this machine's host id; id's start at 0.
//    -nt <numMach> tests the network, assuming <numMach> machines
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import nachos.machine.Disk;

/**
 * Central repository of Nachos options.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */

public class Options {
    
    private static final String copyright =
	      "Copyright (c) 1992-1993 The Regents of the University of California.\n"
	    + "Copyright (c) 1998-1999 Rice University.\n"
	    + "Copyright (c) 2003-2014 State University of New York at Stony Brook.\n"
	    + "All rights reserved.";
    
    /** Arguments in the form of a list. */
    private final List<String> argList;
    
    // Simulation options.
    
    /**
     * If non-zero, then a seed to use for the simulation random number generator.
     * If zero, then the simulation will be randomized.
     */
    public int RANDOM_SEED = 0;
    
    /**
     * If non-zero, the maximum number of ticks that the simulation will run for.
     * If the current simulated time reaches this value, the simulation will terminate
     * abruptly.  Not that if the simulated time reaches Integer.MAX_VALUE, things
     * might not work properly, so it probably doesn't make sense to run any longer
     * than Integer.MAX_VALUE-1.
     */
    public int TIME_LIMIT_TICKS = Integer.MAX_VALUE-1;
    
    /**
     * Target simulation rate.  If nonzero, then the simulation will throttle to
     * no more than roughly this many ticks per second of real time.
     * If zero, then there is no throttle.
     */
    public int TICKS_PER_SECOND = 1000000;
    
    /**
     * Single-stepping user programs.  If true, then user program execution is controlled
     * by a crude debugger, which allows the program to be single-stepped.  This might
     * not be very useful with more than one CPU.
     */
    public boolean SINGLE_STEP = false;
    
     // Hardware configuration options.
    
    /**
     * The number of CPUs on the system.
     * If there is just one CPU, then there is no real concurrent execution of
     * threads and the spinlock synchronization facility is bypassed.
     * If there is more than one CPU, then threads running on different CPUs
     * will run concurrently as Java threads, and will run truly concurrently
     * if the host system is a multi-CPU system and the JVM supports it.
     */
    public int NUM_CPUS = 1;
    
    /**
     * The number of consoles on the system.
     * 
     * NOTE: Configuring a console will start the console driver, which
     * enables keyboard interrupts.  Once keyboard interrupts have been enabled,
     * the simulation will run forever unless explicitly stopped.
     */
    public int NUM_CONSOLES = 1;
    
    /** The number of serial ports on the system. */
    public int NUM_PORTS = 0;
    
    /** The types of disk devices on the system. */
    public Class<?>[] DISK_TYPES = new Class<?>[] { Disk.class };

    /** The number of disks on the system. */
    public int NUM_DISKS = DISK_TYPES.length;
    
     // Device driver initialization options.
    
    /** Base name to use for files representing simulated disks. */
    public String DISK_FILE_NAME = "DISK";
    
    /** This machine's network ID. */
    public byte NETWORK_ID = -1;

    // Kernel configuration options.
    
    /**
     * Are we going to be using per-CPU time-slicing timers?
     *
     * NOTE: Once these are enabled, the simulation will run forever unless
     * explicitly stopped.
     */
    public boolean CPU_TIMERS = false;
    
    /** Should the time-slicing timers be randomized? */
    public boolean RANDOM_YIELD = false;
    
    /** Should we use the stub filesystem, rather than the Nachos filesystem? **/
    public boolean FILESYS_STUB = false;
   
    /** Should we use the "real" Nachos filesystem (requires disk)? */
    public boolean FILESYS_REAL = true;
    
    /** Should we format the Nachos disk before using it? */
    public boolean FORMAT_DISK = false;
    
    // Test/demo configuration options.

    /** Should we run the thread test? */
    public boolean THREAD_TEST = false;  // Traditional Nachos behavior.
    
    /** Should we run the multiprocessor scheduling test? */
    public boolean SMP_TEST = false;

    /** Should we run the user program test? */
    public boolean PROG_TEST = false;
    
    /** Should we run the console test? */
    public boolean CONSOLE_TEST = false;
    
    /** Should we run the filesystem test? */
    public boolean FILESYS_TEST = false;
    
    /** Should we run the serial port test? */
    public boolean SERIAL_TEST = false;
    
    /** Should we run the network test? */
    public boolean NETWORK_TEST = false;
    
    /** Multilevel feedback scheduling test */
    public boolean MULTI_LEVEL_FEEDBACK_SCHEDULE_TEST = false;
    
    public int MULTI_LEVEL_FEEDBACK_SCHEDULE_QUANTUM = 100;
    
    public Options(String[] args) {
	argList = Arrays.asList(args);
	parseArgList();
    }
    
    /**
     * Get the list of command-line arguments originally passed to NACHOS.
     */
    public List<String> getArgList() {
	return Collections.unmodifiableList(argList);
    }
    
    /**
     * Go through the program arguments and set configuration variables.
     */
    private void parseArgList() {
	processOptions
	(new Spec[] {
		new Spec("-z",  // print copyright message
			 new Class[] { },
			 null,
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				System.out.println(copyright);
			    }
			 }),
		new Spec("-c",  // run console test
			 new Class[] { },
			 null,
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				CONSOLE_TEST = true;
			    }
			 }),
		new Spec("-f",  // format the disk before using it
			 new Class[] { },
			 null,
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				FORMAT_DISK = true;
			    }
			 }),
		new Spec("-s",  // single-step user programs
			 new Class[] { },
			 null,
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				SINGLE_STEP = true;
			    }
			 }),
		new Spec("-x",  // enable user program test
			 new Class[] { },
			 null,
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				PROG_TEST = true;
			    }
			 }),
		new Spec("-nt",  // enable network test
			 new Class[] { },
			 null,
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				NETWORK_TEST = true;
			    }
			 }),
		new Spec("-tl",  // set simulation time limit
			 new Class[] {Integer.class},
			 "Usage: -tl <ticks>",
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				TIME_LIMIT_TICKS = (Integer)params[0];
			    }
			 }),
		new Spec("-sr",  // set simulation rate
			 new Class[] {Integer.class},
			 "Usage: -sr <ticks per second>",
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				TICKS_PER_SECOND = (Integer)params[0];
			    }
			 }),
		new Spec("-rs",  // set simulation random number generator seed
			 new Class[] {Integer.class},
			 "Usage: -rs <seed>",
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				RANDOM_SEED = (Integer)params[0];
			    }
			 }),
		new Spec("-ps",  // enable pre-emptive scheduling using per-CPU scheduling timers
			 new Class[] { },
			 null,
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				CPU_TIMERS = true;
			    }
			 }),
		new Spec("-ncpu",  // set the number of CPUs to use
			 new Class[] {Integer.class},
			 "Usage: -ncpu <ncpus>",
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				NUM_CPUS = (Integer)params[0];
			    }
			 }),
		new Spec("-ncon",  // set the number of consoles to use
			 new Class[] {Integer.class},
			 "Usage: -ncon <nconsoles>",
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				NUM_CONSOLES = (Integer)params[0];
			    }
			 }),
		new Spec(
			"-mlfs", // set multilevel feedback scheduling test
			new Class[] { Integer.class },
			"Usage: -mlfs <quantum>", new Options.Action() {
			    public void processOption(String flag,
				    Object[] params) {
				MULTI_LEVEL_FEEDBACK_SCHEDULE_TEST = true;
				MULTI_LEVEL_FEEDBACK_SCHEDULE_QUANTUM = (Integer)params[0];
			    }
			}),	 
		new Spec("-df",  // set the base filename for simulated disks
			 new Class[] {String.class},
			 "Usage: -df <filename>",
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				DISK_FILE_NAME = (String)params[0];
			    }
			 })
	});
    }
    
    /**
     * Class whose objects represent the specification of a particular type of option
     * to look for in the argument list.
     */
    public static class Spec {
	
	/** Flag that signals the start of the option. */
	private final String flag;
	
	/** Specification of the number and types of the parameters. */
	private final Class<?>[] types;
	
	/** Action to take when an instance of the option is encountered. */
	private final Action action;
	
	/** Message to print if the proper number of parameters is not found. */
	private final String usage;
	
	/**
	 * Initialize a new option Spec.
	 * 
	 * @param flag  The flag that signals the start of the option.
	 * This can be any string, but conventionally option flags start with "-".
	 * @param types  Array of Class objects that specifies the number and types
	 * of the parameters that should follow this option flag.
	 * @param usage  Message to be printed when the proper number or types of
	 * parameters are not found for an option, or null for no useful message.
	 * @param action  Action to be taken when an occurrence of this option
	 * and its associated parameters is encountered in the argument list.
	 */
	public Spec(String flag, Class<?>[] types, String usage, Action action) {
	    this.flag = flag;
	    this.types = types;
	    this.action = action;
	    this.usage = usage;
	}
	
	/**
	 * Method that checks whether an option flag matches this specification,
	 * and if so, collects and validates option parameters and dispatches
	 * the processing of this option.
	 * 
	 * @param flag  The option flag to match.
	 * @param args  Iterator that provides the remaining arguments.
	 * If the option flag matches this spec, then any parameters required
	 * will be obtained from this iterator.
	 */
	public void match(String flag, Iterator<String> args) {
	    if(!this.flag.equals(flag))
		return;

	    Object[] params = new Object[types.length];
	    for(int i = 0; i < params.length; i++) {
		if(!args.hasNext()) {
		    System.out.println
		           ((usage != null ? usage + "\n" : "")
			    + "Option " + flag + " takes " + params.length
			    + " parameters, " + i + " found");
		    Debug.ASSERT(false);
		}
		String arg = args.next();
		Class<?> c = types[i];
		if(c == String.class) {
		    params[i] = arg;
		} else if(c == Boolean.class) {
		    params[i] = Boolean.parseBoolean(arg);
		} else if(c == Integer.class) {
		    try {
			params[i] = Integer.parseInt(arg);
		    } catch(NumberFormatException x) {
			System.out.println
			       ((usage != null ? usage + "\n" : "")
				+ "Parameter " + arg + " to option " + flag
				+ " is not a valid integer");
			Debug.ASSERT(false);
		    }
		} else if(c == Double.class) {
		    try {
			params[i] = Double.parseDouble(arg);
		    } catch(NumberFormatException x) {
			System.out.println
			       ((usage != null ? usage + "\n" : "")
				+ "Parameter " + arg + " to option " + flag
				+ " is not a valid number");
			Debug.ASSERT(false);
		    }
		} else {
		    params[i] = arg;
		}
	    }
	    action.processOption(flag, params);
	}
	
    }
    
    /**
     * Interface implemented by objects to be invoked when an option has been encountered.
     */
    public static interface Action {
	/**
	 * Action to be performed when an option has been encountered.
	 * 
	 * @param flag  The option flag that was matched.
	 * @param params  The parameters to the option.
	 */
	public void processOption(String flag, Object[] params);
    }
    
    /**
     * Method to scan the argument list looking for options matching a list of specifications,
     * and dispatching option processing when an option occurrence is identified.
     */
    public void processOptions(Spec[] specs) {
	Iterator<String> args = argList.iterator();
	while(args.hasNext()) {
	    String flag = args.next();
	    for(Spec spec : specs)
		spec.match(flag, args);
	}
    }
   
}
