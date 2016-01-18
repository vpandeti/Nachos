// OpenFileStub.java
//	Class for reading and writing to individual files.
//	The operations supported are similar to	the UNIX ones.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.filesys;

import java.io.*;

/**
 * This "stub" class implements file operations for Nachos by simply
 * passing the filesystem operations through to the native filesystem on
 * the host platform.  This is provided in case the multiprogramming and
 * virtual memory assignments (which make use of the file system) are done
 * before the file system assignment.
 * 
 * @author Peter Druschel (Rice University)
 * @author Eugene W. Stark (Stony Brook University)
 */
class OpenFileStub implements OpenFile {

  /** The underlying file on the host platform. */
  private RandomAccessFile file;

  /** The current file position. */
  private long currentOffset;

  /**
   * Open a file.  This constructor is not exported outside the package,
   * because users of the filesystem should be using the methods of the
   * FileSystem class to obtain an OpenFile.
   *
   * @param f The underlying file on the host filesystem.
   */
  OpenFileStub(RandomAccessFile f) {
    file = f; 
    currentOffset = 0; 
  }

  /**
   * Set the position from which to
   * start reading/writing -- UNIX lseek
   *
   * @param position The desired position in the file, in bytes from
   * the start of file.
   */
  public void seek(long position) {
    currentOffset = position;
  }    

  /**
   * Read bytes from the file, bypassing the implicit position.
   *
   * @param into Buffer into which to read data.
   * @param index Starting position in the buffer.
   * @param numBytes Number of bytes desired.
   * @param position Starting position in the file.
   * @return The number of bytes actually read (0 in case of an error).
   */
  public int readAt(byte into[], int index, int numBytes, long position) { 
    int len;

    try {
      file.seek(position);
      len = file.read(into, index, numBytes);
    } catch (IOException e) {
      return 0;
    }
    return len;
  }

  /**
   * Write bytes to the file, bypassing the implicit position.
   *
   * @param from Buffer containing the data to be written.
   * @param index Starting position in the buffer.
   * @param numBytes Number of bytes to write.
   * @param position Starting position in the file.
   * @return The number of bytes actually written (0 in case of an error).
   */
  public int writeAt(byte from[], int index, int numBytes, long position) { 
    try {
      file.seek(position);
      file.write(from, index, numBytes);
    } catch (IOException e) {
      return 0;
    }
    return numBytes;
  }	

  /**
   * Read bytes from the file, starting at the implicit position.
   * The position is incremented by the number of bytes read.
   *
   * @param into Buffer into which to read data.
   * @param index Starting position in the buffer.
   * @param numBytes Number of bytes desired.
   * @return The number of bytes actually read (0 in case of an error).
   */
  public int read(byte into[], int index, int numBytes) {
    int numRead = readAt(into, index, numBytes, currentOffset); 
    if (numRead > 0) currentOffset += numRead;
    return numRead;
  }

  /**
   * Write bytes to the file, starting at the implicit position.
   * The position is incremented by the number of bytes read.
   *
   * @param from Buffer containing the data to be written.
   * @param index Starting position in the buffer.
   * @param numBytes Number of bytes to write.
   * @return The number of bytes actually written (0 in case of an error).
   */
  public int write(byte from[], int index, int numBytes) {
    int numWritten = writeAt(from, index, numBytes, currentOffset); 
    if (numWritten > 0) currentOffset += numWritten;
    return numWritten;
  }

  /**
   * Determine the number of bytes in the file
   * (this interface is simpler than the UNIX idiom -- lseek to
   * end of file, tell, lseek back).
   *
   * @return the length of the file in bytes.
   */
  public long length() {
    long len;

    try {
      len = file.length();
    } catch (IOException e) {
      return 0;
    }
    return len;
  }

  /**
   * Close the file, releasing any resources held in kernel memory.
   * Subsequent attempts to access the file will fail.
   *
   * @return 0 if an error occurred while closing the file, otherwise
   * nonzero.
   */
  public int close() {
      try {
	  file.close();
      } catch (IOException e) {
	  return(0);
      }
      return(1);
  }
}
