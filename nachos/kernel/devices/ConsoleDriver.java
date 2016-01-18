// ConsoleDriver.java
//
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.devices;

import java.util.ArrayList;
import java.util.Arrays;

import nachos.kernel.threads.Lock;
import nachos.kernel.threads.Semaphore;
import nachos.machine.Console;
import nachos.machine.InterruptHandler;

/**
 * This class provides for the initialization of the NACHOS console, and gives
 * NACHOS user programs a capability of outputting to the console. This driver
 * does not perform any input or output buffering, so a thread performing output
 * must block waiting for each individual character to be printed, and there are
 * no input-editing (backspace, delete, and the like) performed on input typed
 * at the keyboard.
 * 
 * Students will rewrite this into a full-fledged interrupt-driven driver that
 * provides efficient, thread-safe operation, along with echoing and
 * input-editing features.
 * 
 * @author Eugene W. Stark
 */
public class ConsoleDriver {

    /** Raw console device. */
    private Console console;

    /** Lock used to ensure at most one thread trying to input at a time. */
    private Lock inputLock;

    /** Lock used to ensure at most one thread trying to output at a time. */
    private Lock outputLock;

    /** Semaphore used to indicate that an input character is available. */
    private Semaphore charAvail = new Semaphore("Console char avail", 0);

    /**
     * Semaphore used to indicate that output is ready to accept a new
     * character.
     */
    private Semaphore outputDone = new Semaphore("Console output done", 0);

    /** Interrupt handler used for console keyboard interrupts. */
    private InterruptHandler inputHandler;

    /** Interrupt handler used for console output interrupts. */
    private InterruptHandler outputHandler;

    private static final int MAX_BUFFER_SIZE = 10000;

    private static final int OUTPUT_BUFFER_CAPACITY = 10;

    char[] buffer = new char[MAX_BUFFER_SIZE];

    ArrayList<Character> consoleBuffer = new ArrayList<Character>();

    int charactersRead = 0;

    int charactersWritten = 0;
    
    private boolean isOutputDone = true;

    private boolean isBackspace = false;
    private boolean isCTRL_U = false;
    private boolean isCTRL_R = false;
    private boolean isCTRL_R_Type = false;
    private int CTRL_U_Index = 0;
    private int CTRL_R_Index = 0;
    
    private char[] newLineBuffer = new char[]{'\n','\r'};
    private boolean isNewLine = false;
    private int newLineIndex = 0;
    
    private char[] backspaceBuffer = new char[]{'\b',' ','\b'};
    private int backspaceBufferIndex = 0;

    private int getCharIndex = 0;
    
    private char[] outputBuffer = new char[OUTPUT_BUFFER_CAPACITY];
    private int outputBufferIndex = 0;
    
    private boolean isReadOperation = false;
    /**
     * Initialize the driver and the underlying physical device.
     * 
     * @param console
     *            The console device to be managed.
     */
    public ConsoleDriver(Console console) {
	inputLock = new Lock("console driver input lock");
	outputLock = new Lock("console driver output lock");
	this.console = console;
	Arrays.fill(outputBuffer, ' ');
	// Delay setting the interrupt handlers until first use.
    }

    /**
     * Create and set the keyboard interrupt handler, if one has not already
     * been set.
     */
    private void ensureInputHandler() {
	if (inputHandler == null) {
	    inputHandler = new InputHandler();
	    console.setInputHandler(inputHandler);
	}
    }

    /**
     * Create and set the output interrupt handler, if one has not already been
     * set.
     */
    private void ensureOutputHandler() {
	if (outputHandler == null) {
	    outputHandler = new OutputHandler();
	    console.setOutputHandler(outputHandler);
	}
    }

    /**
     * Wait for a character to be available from the console and then return the
     * character.
     */
    public char getChar() {
	inputLock.acquire();
	ensureInputHandler();
	if(getCharIndex == 0 || getCharIndex >= charactersRead)
	    charAvail.P();
	char ch = buffer[getCharIndex++];
	inputLock.release();
	return ch;
    }

    /**
     * Print a single character on the console. If the console is already busy
     * outputting a character, then wait for it to finish before attempting to
     * output the new character. A lock is employed to ensure that at most one
     * thread at a time will attempt to print.
     *
     * @param ch
     *            The character to be printed.
     */
    boolean isOutputBufferFilled = false;
    private int outputPrintIndex = 0;
    public void putChar(char ch) {
	outputLock.acquire();
	ensureOutputHandler();
	if(outputPrintIndex > outputBufferIndex)
	    outputDone.P();
	outputBuffer[outputBufferIndex++] = ch;
	if(outputBufferIndex == OUTPUT_BUFFER_CAPACITY) {
	    outputBufferIndex = 0;
	    console.putChar(outputBuffer[outputPrintIndex++]);
	    outputBuffer[outputPrintIndex-1] = ' ';
	    outputDone.P();
	}
	outputLock.release();
    }
    
    public void putCharWithBuffer(char ch, boolean lastChar) {
	try {
	    outputLock.acquire();
	    ensureOutputHandler();
	    
	    if(outputPrintIndex > outputBufferIndex || outputPrintIndex == OUTPUT_BUFFER_CAPACITY) {
		if(outputPrintIndex == OUTPUT_BUFFER_CAPACITY)
		    outputPrintIndex = 0;
		outputDone.P();
	    }
	    if(outputBufferIndex == OUTPUT_BUFFER_CAPACITY) {
		outputBufferIndex = 0;
		outputDone.P();
	    }
	    outputBuffer[outputBufferIndex++] = ch;
	    if(isOutputDone && (outputBufferIndex == OUTPUT_BUFFER_CAPACITY || (outputBufferIndex < OUTPUT_BUFFER_CAPACITY && lastChar))) {
		outputBufferIndex = 0;
		console.putChar(outputBuffer[outputPrintIndex++]);
		outputBuffer[outputPrintIndex-1] = ' ';
		outputDone.P();
	    }
	    outputLock.release();
	} catch (Exception ex) {
	    
	}
    }

    /**
     * Stop the console device. This removes the interrupt handlers, which
     * otherwise prevent the Nachos simulation from terminating automatically.
     */
    public void stop() {
	inputLock.acquire();
	console.setInputHandler(null);
	inputLock.release();
	outputLock.acquire();
	console.setOutputHandler(null);
	outputLock.release();
    }

    /**
     * Interrupt handler for the input (keyboard) half of the console.
     */
    private class InputHandler implements InterruptHandler {

	@Override
	public void handleInterrupt() {
	    char ch = console.getChar();
	    if (charactersRead == MAX_BUFFER_SIZE) {
		//flushBuffer();
		//charAvail.V();
	    } else {
		handleChars(ch);
	    }
	}

    }

    /**
     * Interrupt handler for the output (screen) half of the console.
     */
    private class OutputHandler implements InterruptHandler {

	@Override
	public void handleInterrupt() {
	    isOutputDone = false;
	    if(isBackspace) {
		if(backspaceBufferIndex == backspaceBuffer.length) {
		    backspaceBufferIndex = 0;
		    isBackspace = false;
		    if(isCTRL_R) {
			CTRL_R_Index++;
			console.putChar(backspaceBuffer[backspaceBufferIndex++]);
			return;
		    } else if(isCTRL_U) {
			CTRL_U_Index++;
			console.putChar(backspaceBuffer[backspaceBufferIndex++]);
			return;
		    }
		    return;
		}
		console.putChar(backspaceBuffer[backspaceBufferIndex++]);
	    } else if (isCTRL_R) {
		if(CTRL_R_Index == consoleBuffer.size() && !isCTRL_R_Type) {
		    CTRL_R_Index = 0;
		    isCTRL_R_Type = true;
		    console.putChar(consoleBuffer.get(CTRL_R_Index++));
		    return;
		} else if(isCTRL_R_Type) {
		    if(CTRL_R_Index == consoleBuffer.size()) {
			isCTRL_R = false;
			isCTRL_R_Type = false;
			//consoleBuffer.clear();
			backspaceBufferIndex = 0;
			CTRL_R_Index = 0;
			return;
		    }
		    console.putChar(consoleBuffer.get(CTRL_R_Index++));
		    return;
		}
		isBackspace = true;
		console.putChar(backspaceBuffer[backspaceBufferIndex++]);
	    } else if (isCTRL_U) {
		if(CTRL_U_Index == consoleBuffer.size()) {
		    CTRL_U_Index = 0;
		    isCTRL_U = false;
		    backspaceBufferIndex = 0;
		    for(int i = 0; i < consoleBuffer.size(); i++) {
			buffer[--charactersRead] = ' '; 
		    }
		    consoleBuffer.clear();
		    return;
		}
		isBackspace = true;
		if(backspaceBufferIndex == backspaceBuffer.length)
		    backspaceBufferIndex = 0;
		console.putChar(backspaceBuffer[backspaceBufferIndex++]);
	    } else if(isNewLine) {
		if(newLineIndex == newLineBuffer.length) {
		    isNewLine = false;
		    newLineIndex = 0;
		    consoleBuffer.clear();
		    charAvail.V();
		    return;
		}
		console.putChar(newLineBuffer[newLineIndex++]);
	    } else {
		if(isReadOperation)
		    return;
		boolean isBufferEmpty = true;
		for(int i = 0; i < OUTPUT_BUFFER_CAPACITY; i++) {
		    if(outputBuffer[i] != ' ') {
			isBufferEmpty = false;
			break;
		    }
		}
		if(isBufferEmpty) {
		    outputDone.V();
		    isOutputDone = true;
		    outputBufferIndex = outputPrintIndex = 0;
		    outputDone = new Semaphore("Console output done", 0);
		    return;
		}
		    
		if(outputPrintIndex == OUTPUT_BUFFER_CAPACITY) {
		    outputPrintIndex = 0;
		    //isOutputDone = true;
		}
		
		console.putChar(outputBuffer[outputPrintIndex++]);
		outputBuffer[outputPrintIndex-1] = ' ';
		outputDone.V();
	    }
	}

    }

    private void handleChars(char ch) {
	isReadOperation = true;
	// valid characters to be printed
	ensureOutputHandler();
	if (ch >= 32 && ch <= 126) {
	    buffer[charactersRead++] = ch;
	    consoleBuffer.add(ch);
	    console.putChar((char) ch);
	}
	// CTRL + U
	else if (ch == 21) {
	    if(consoleBuffer.isEmpty())
		return;
	    isCTRL_U = true;
	    console.putChar(backspaceBuffer[backspaceBufferIndex++]);
	}
	// CTRL + R
	else if (ch == 18) {
	    if(consoleBuffer.isEmpty())
		return;
	    isCTRL_R = true;
	    console.putChar(backspaceBuffer[backspaceBufferIndex++]);
	}
	// Backspace
	else if (ch == 8) {
	    // Sequence to be followed: BKSPC, SPC, BKSPC
	    isBackspace = true;
	    console.putChar(backspaceBuffer[backspaceBufferIndex++]);
	    --charactersRead;
	}
	// new line
	else if (ch == 10 || ch == 13) {
	    consoleBuffer.add(ch);
	    buffer[charactersRead++] = ch;
	    isNewLine = true;
	    console.putChar(newLineBuffer[newLineIndex++]);
	}
    }
}
