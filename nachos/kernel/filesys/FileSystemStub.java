// FileSystemStub.java
//	Implementation of the Nachos file system in terms of the native
//	filesystem on the host platform.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.filesys;

import java.io.*;

/**
 * This "filesystem stub" class implements a Nachos filesystem by simply
 * passing the filesystem operations through to the native filesystem on
 * the host platform.  This is provided in case the multiprogramming and
 * virtual memory assignments (which make use of the file system) are done
 * before the file system assignment.
 * 
 * @author Peter Druschel (Rice University)
 * @author Eugene W. Stark (Stony Brook University)
 */
class FileSystemStub extends FileSystem {

    /**
     * Constructor is protected so that all creations are funneled through
     * the init() factory method of the super class.
     */
    protected FileSystemStub() { 
	super(); 
    }

    /**
     * Create a new file with a specified name and size.
     *
     * @param name The name of the file.
     * @param initialSize The size of the file.
     * @return true if the operation was successful, otherwise false.
     */
    public boolean create(String name, long initialSize) { 
	FileOutputStream fsFile;

	try {
	    fsFile = new FileOutputStream(name);
	    fsFile.close();    
	} catch (IOException e) {
	    return false;
	}

	return true; 
    }

    /**
     * Open the file with the specified name and return an OpenFile
     * object that provides access to the file contents.
     *
     * @param name The name of the file.
     * @return An OpenFile object that provides access to the file contents,
     * if the file was successfully opened, otherwise null.
     */
    public OpenFile open(String name) {
	RandomAccessFile file;

	if (!new File(name).exists())
	    return null;
	try {
	    file = new RandomAccessFile(name, "rw");
	}
	catch (IOException e) {
	    return null;
	}

	return new OpenFileStub(file);
    }

    /**
     * Remove the file with the specified name.
     *
     * @param name The name of the file.
     * @return true if the operation was successful, otherwise false.
     */
    public boolean remove(String name) { 
	File file;

	file = new File(name);
	return file.delete();
    }

    @Override
    public boolean mkdir(String name) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean rmdir(String name) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean mkfile(String name, int size) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public OpenFile openFile(String name) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public void start() {
	// TODO Auto-generated method stub
	
    }

}
