// BitMap.java
//	Class to manage a bitmap -- an array of bits each of which
//	can be either on or off.  
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.filesys;

import nachos.Debug;

/**
 * This class defines a "bitmap" -- an array of bits,
 * each of which can be independently set, cleared, and tested.
 *
 * Most useful for managing the allocation of the elements of an array --
 * for instance, disk sectors, or main memory pages.
 * Each bit represents whether the corresponding sector or page is
 * in use or free.
 *
 * We represent a bitmap as an array of unsigned integers, on which we do
 * modulo arithmetic to find the bit we are interested in.
 * The bitmap can be parameterized by the number of bits being managed.
 * It is also equipped with fetchFrom() and writeBack() methods for
 * reading a bitmap from, and writing a bit map to, a Nachos file.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class BitMap {
    // Definitions helpful for representing a bitmap as an array of integers

    /** Number of bits in a byte. */
    static final int BitsInByte = 8;

    /** Number of bytes in an integer. */
    private static final int BitsInWord = 32;

    /** Number of bits in the bitmap. */
    private int numBits;

    /**
     * Number of words of bitmap storage (rounded up if numBits is not a
     * multiple of the number of bits in a word).
     */
    private int numWords;

    /** Bit storage. */
    private int map[];

    /**
     * Initialize a bitmap with "nitems" bits, so that every bit is clear.
     * it can be added somewhere on a list.
     *
     * @param nitems The number of bits in the bitmap.
     */
    public BitMap(int nitems) { 
	numBits = nitems;
	numWords = numBits / BitsInWord;
	if (numBits % BitsInWord != 0) numWords++;

	map = new int[numWords];
	for (int i = 0; i < numBits; i++) 
	    clear(i);
    }

    /**
     * Set the "nth" bit in a bitmap.
     *
     * @param which  The number of the bit to be set.
     */
    public void mark(int which) { 
	Debug.ASSERT(which >= 0 && which < numBits);
	map[which / BitsInWord] |= 1 << (which % BitsInWord);
    }

    /**
     * Clear the "nth" bit in a bitmap.
     *
     * @param which The number of the bit to be cleared.
     */
    public void clear(int which) {
	Debug.ASSERT(which >= 0 && which < numBits);
	map[which / BitsInWord] &= ~(1 << (which % BitsInWord));
    }

    /**
     * Test if the "nth" bit is set.
     *
     * @param which The number of the bit to be tested.
     * @return true if the indicated bit is set, otherwise false.
     */
    public boolean test(int which) {
	Debug.ASSERT(which >= 0 && which < numBits);

	if ((map[which / BitsInWord] & (1 << (which % BitsInWord))) != 0)
	    return true;
	else
	    return false;
    }

    /**
     * Find the first bit that is clear.
     * As a side effect, set the bit (mark it as in use).
     * (In other words, find and allocate a bit.)
     *
     * @return the bit set, if one was found, otherwise -1.
     *
     */
    public int find() {
	for (int i = 0; i < numBits; i++)
	    if (!test(i)) {
		mark(i);
		return i;
	    }
	return -1;
    }

    /**
     * Return the number of clear bits in the bitmap.
     * (In other words, how many bits are unallocated?)
     *
     * @return the number of clear bits in the bitmap.
     */
    public int numClear() {
	int count = 0;

	for (int i = 0; i < numBits; i++)
	    if (!test(i)) count++;
	return count;
    }

    /**
     * Print the contents of the bitmap, for debugging.
     *
     * Could be done in a number of ways, but we just print the #'s of
     * all the bits that are set in the bitmap.
     */
    public void print() {
	String toPrint = "";
	toPrint += "Bitmap set: "; 
	for (int i = 0; i < numBits; i++)
	    if (test(i))
		toPrint += (i + ", ");
	Debug.println('+', toPrint);
    }

    // These aren't needed until the FILESYS assignment

    /**
     * Initialize the contents of a bitmap from a Nachos file.
     *
     * @param file The file to read the bitmap from.
     */
    public void fetchFrom(OpenFile file) {
	byte buffer[] = new byte[numWords*4];
	// read bitmap
	file.readAt(buffer, 0, numWords * 4, 0);
	// unmarshall
	for (int i = 0; i < numWords; i++)
	    map[i] = FileSystem.bytesToInt(buffer, i*4);
    }

    /**
     * Store the contents of a bitmap to a Nachos file.
     *
     * @param file The file to write the bitmap to.
     */
    public void writeBack(OpenFile file) {
	byte buffer[] = new byte[numWords*4];
	// marshall
	for (int i = 0; i < numWords; i++)
	    FileSystem.intToBytes(map[i], buffer, i*4);
	// write bitmap
	file.writeAt(buffer, 0, numWords * 4, 0);
    }

}
