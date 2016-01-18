package nachos.kernel.devices.test;

import nachos.Debug;
import nachos.Options;
import nachos.machine.Packet;
import nachos.machine.MalformedPacketException;
import nachos.kernel.Nachos;

/**
 * Class for testing the nachos network.
 *
 * Two (or more) copies of nachos must be running, with machine ID's 0,
 * 1, ...  For example, with three copies of Nachos, 
 * <br>
 * ./run -d n -m 0 -nt 3 & ./run -d n -m 1 -nt 3 & ./run -d n -m 2 -nt 3 & 
 * <br>
 *
 * All the copies of nachos should be started at approximately the
 * same time (before the 5 second delay in the first machine's Network
 * constructor finishes); otherwise, messages may be lost.  So, it's
 * convenient to use a single command-line to start all the copies of
 * nachos, as shown above.  On the other hand, it's easier to see
 * what's going on if you run each nachos in a separate terminal
 * window (on the same computer). 
 *
 * When using the network, nachos never terminates automatically,
 * because it doesn't know whether it might receive another message.
 * It will terminate only if a user program calls Halt or if you
 * kill it externally (e.g., using the Unix or cygwin "kill" command, 
 * or using Windows's Task Manager).
 * 
 * @author Scott Stoller
 */
public class NetworkTest {

  /** number of nachos machines in the network, including this one.
   */
  private static int numMach = 0;

  /** network address of this nachos machine. */
  private static byte me = 0;

  /**
   * Entry point for the Network test.  If "-nt" is included in the
   * command-line arguments, then run the network test; otherwise, do
   * nothing.
   *
   * The network test sends a greeting to each other machine, and 
   * receives a greeting from each other machine.
   */
  public static void start() {
	Debug.println('+', "Entering NetworkTest");
	Debug.ASSERT(Nachos.networkDriver != null);
     
	Nachos.options.processOptions
	(new Options.Spec[] {
		new Options.Spec
			("-nt",  // run network test
			 new Class[] {Integer.class},
			 "Usage: -nt <numMach>",
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				numMach = (Integer)params[0];
			    }
			 }),
		new Options.Spec
			("-m",  // set machine's network id
			 new Class[] {Integer.class},
			 "Usage: -m <id>",
			 new Options.Action() {
			    public void processOption(String flag, Object[] params) {
				me = (byte)((Integer)params[0]).intValue();
			    }
			 })
	});

	// send a greeting to each other machine
	for (int dest = 0; dest < numMach; dest++) {
	    if (dest == me) continue;
	    String msg = "Greetings from " + me + " to " + dest + ".";
	    try {
		Packet p = new Packet(dest, me, msg.getBytes());
		Nachos.networkDriver.send(p); 
		Debug.println('+', 
			"NetworkTest: machine " + me + " sent message: " + msg);
	    } catch (MalformedPacketException e) {
		Debug.ASSERT(false, "NetworkTest: MalformedPacketException");
	    }
	};

	// receive a greeting from each other machine
	for (int i=0; i < numMach-1; i++) {
	    Debug.println('+', 
		    "NetworkTest: machine " + me + " waiting to receive a message");
	    Packet p = Nachos.networkDriver.receive();
	    Debug.println('+', 
		    "NetworkTest: machine " + me + " received message: "
			    + new String(p.contents));
	};
  }

}
