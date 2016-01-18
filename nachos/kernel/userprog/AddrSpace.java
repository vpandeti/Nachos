// AddrSpace.java
//	Class to manage address spaces (executing user programs).
//
//	In order to run a user program, you must:
//
//	1. link with the -N -T 0 option 
//	2. run coff2noff to convert the object file to Nachos format
//		(Nachos object code format is essentially just a simpler
//		version of the UNIX executable object code format)
//	3. load the NOFF file into the Nachos file system
//		(if you haven't implemented the file system yet, you
//		don't need to do this last step)
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import nachos.Debug;
import nachos.kernel.filesys.OpenFile;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.NachosThread;
import nachos.machine.TranslationEntry;
import nachos.noff.NoffHeader;

/**
 * This class manages "address spaces", which are the contexts in which user
 * programs execute. For now, an address space contains a "segment descriptor",
 * which describes the the virtual-to-physical address mapping that is to be
 * used when the user program is executing. As you implement more of Nachos, it
 * will probably be necessary to add other fields to this class to keep track of
 * things like open files, network connections, etc., in use by a user program.
 *
 * NOTE: Most of what is in currently this class assumes that just one user
 * program at a time will be executing. You will have to rewrite this code so
 * that it is suitable for multiprogramming.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class AddrSpace {

    /** Page table that describes a virtual-to-physical address mapping. */
    private TranslationEntry pageTable[];

    /** Default size of the user stack area -- increase this as necessary! */
    private static final int UserStackSize = 1024;

    /** Maintains a tracker for Address spaces */
    private static int spaceId = 0;
    
    private int numPhysicalPages = 0;
    
    public OpenFile pageFaultFile;
    public String pageFaultFileName = "";
    public HashMap<Integer, Integer> pageFaultMapping = new HashMap<Integer, Integer>();
    public HashMap<String, OpenFile> fileMapping = new HashMap<String, OpenFile>();
    public HashMap<String, String> fileMappingWithAddress = new HashMap<String, String>();
    public HashMap<Integer, ArrayList<Integer>> pageListMapping = new HashMap<Integer, ArrayList<Integer>>();
    public HashMap<Integer, String> pageFaultFileNameMapping = new HashMap<Integer, String>();
    /**
     * Create a new address space.
     */
    public AddrSpace() {
	++spaceId;
    }

    /**
     * Load the program from a file "executable", and set everything up so that
     * we can start executing user instructions.
     *
     * Assumes that the object code file is in NOFF format.
     *
     * First, set up the translation from program memory to physical memory. For
     * now, this is really simple (1:1), since we are only uniprogramming.
     *
     * @param executable
     *            The file containing the object code to load into memory
     * @return -1 if an error occurs while reading the object file, otherwise 0.
     */
    NoffHeader noffH = null;
    long size;
    public int exec(OpenFile executable) {

	if ((noffH = NoffHeader.readHeader(executable)) == null)
	    return (-1);

	// how big is address space?
	size = roundToPage(noffH.code.size)
		+ roundToPage(noffH.initData.size + noffH.uninitData.size)
		+ UserStackSize; // we need to increase the size
				 // to leave room for the stack
	int numPages = (int) (size / Machine.PageSize);
	numPhysicalPages = numPages;
	Debug.ASSERT((numPages <= Machine.NumPhysPages),// check we're not
							// trying
		"AddrSpace constructor: Not enough memory!");
	// to run anything too big --
	// at least until we have
	// virtual memory

	Debug.println('a', "Initializing address space, numPages=" + numPages
		+ ", size=" + size);
	PhysicalMemoryManager pmm = PhysicalMemoryManager.getInstance();
	//pmm.setPages();
	// first, set up the translation
	pageTable = new TranslationEntry[numPages];
	for (int i = 0; i < numPages; i++) {
	    pageTable[i] = new TranslationEntry();
	    pageTable[i].virtualPage = i;
	    pageTable[i].physicalPage = pmm.getPage();
	    if(pageTable[i].physicalPage == -1)
		pageTable[i].valid = false;
	    else
		pageTable[i].valid = true;
	    pageTable[i].use = false;
	    pageTable[i].dirty = false;
	    // if code and data segments live on
	    // separate pages, we could set code
	    // pages to be read-only
	    pageTable[i].readOnly = false;
	}

	byte[] tempBuffer = new byte[(int) size];

	// then, copy in the code and data segments into memory
	if (noffH.code.size > 0) {
	    Debug.println('a', "Initializing code segment, at "
		    + noffH.code.virtualAddr + ", size " + noffH.code.size);

	    executable.seek(noffH.code.inFileAddr);
	    executable
		    .read(tempBuffer, noffH.code.virtualAddr, noffH.code.size);
	}

	if (noffH.initData.size > 0) {
	    Debug.println('a', "Initializing data segment, at "
		    + noffH.initData.virtualAddr + ", size "
		    + noffH.initData.size);

	    executable.seek(noffH.initData.inFileAddr);
	    executable.read(tempBuffer, noffH.initData.virtualAddr,
		    noffH.initData.size);
	}

	// Starts paging
	int ppn = 0, offset = 0;
	int index = 0;
	for (int i = 0; i < size; i++) {
	    if (i != 0 && i % Machine.PageSize == 0) {
		ppn++;
		offset = 0;
	    }
	    index = (pageTable[ppn].physicalPage * Machine.PageSize) + (offset++);
	    if(index < 0) {
		break;
	    }
	    Machine.mainMemory[index] = tempBuffer[i];
	}

	pmm.mapProcessIdToAddressSpace(NachosThread.currentThread().name, this);
	return (0);
    }

    /**
     * Releases the memory. Destroy all the pages that have been created.
     */
    public void destroy() {
	// Destroy each object inside the array for better garbage collection
	// It may improve the performance
	for(int i = 0; i < pageTable.length; i++) {
	    int m = 0;
	    for(int j = 0; j < Machine.PageSize; j++) {
		m = (pageTable[i].physicalPage * Machine.PageSize) + j;
		if(m < 0)
		    break;
		Machine.mainMemory[m] = 0;
	    }
	}
	// Destroys the Physical Memory Manager instance
	PhysicalMemoryManager.getInstance().destroySpace(pageTable);
	for (int i = 0; i < pageTable.length; i++) {
	    pageTable[i] = null;
	}
	// Freeing the memory
	pageTable = null;
    }

    /**
     * Initialize the user-level register set to values appropriate for starting
     * execution of a user program loaded in this address space.
     *
     * We write these directly into the "machine" registers, so that we can
     * immediately jump to user code.
     */
    public void initRegisters() {
	int i;

	for (i = 0; i < MIPS.NumTotalRegs; i++)
	    CPU.writeRegister(i, 0);

	// Initial program counter -- must be location of "Start"
	CPU.writeRegister(MIPS.PCReg, 0);

	// Need to also tell MIPS where next instruction is, because
	// of branch delay possibility
	CPU.writeRegister(MIPS.NextPCReg, 4);

	// Set the stack register to the end of the segment.
	// NOTE: Nachos traditionally subtracted 16 bytes here,
	// but that turns out to be to accommodate compiler convention that
	// assumes space in the current frame to save four argument registers.
	// That code rightly belongs in start.s and has been moved there.
	int sp = pageTable.length * Machine.PageSize;
	CPU.writeRegister(MIPS.StackReg, sp);
	Debug.println('a', "Initializing stack register to " + sp);
    }

    /**
     * On a context switch, save any machine state, specific to this address
     * space, that needs saving.
     *
     * For now, nothing!
     */
    public void saveState() {
    }

    /**
     * On a context switch, restore any machine state specific to this address
     * space.
     *
     * For now, just tell the machine where to find the page table.
     */
    public void restoreState() {
	CPU.setPageTable(pageTable);
    }

    /**
     * Utility method for rounding up to a multiple of CPU.PageSize;
     */
    private long roundToPage(long size) {
	return (Machine.PageSize * ((size + (Machine.PageSize - 1)) / Machine.PageSize));
    }

    /**
     * Returns the page table
     * @return TranslationEntry[]
     */
    public TranslationEntry[] getPageTable() {
	return pageTable;
    }
    
    /**
     * Returns the address space id
     * @return space id
     */
    public int getSpaceId() {
	return spaceId;
    }
    
    public AddrSpace clone() {
	AddrSpace space = new AddrSpace();
	//space.setSpaceId(spaceId+1);
	space.numPhysicalPages = numPhysicalPages;
	space.pageTable = new TranslationEntry[numPhysicalPages];
	for(int i = 0; i < pageTable.length; i++) {
	    space.pageTable[i] = new TranslationEntry();
	    space.pageTable[i].virtualPage = pageTable[i].virtualPage;
	    space.pageTable[i].physicalPage = PhysicalMemoryManager.getInstance().getPage();
	    //space.pageTable[i].physicalPage = pageTable[i].physicalPage;
	    space.pageTable[i].valid = pageTable[i].valid;
	    space.pageTable[i].use = pageTable[i].use;
	    space.pageTable[i].dirty = pageTable[i].dirty;
	    pageTable[i].readOnly = pageTable[i].readOnly;
	    int m = 0, n = 0;
	    for(int j = 0; j < Machine.PageSize; j++) {
		m = (pageTable[i].physicalPage * Machine.PageSize) + j;
		n = (space.pageTable[i].physicalPage * Machine.PageSize) + (j);
		if(j >= noffH.code.size + noffH.initData.size)
		    break;
		Machine.mainMemory[n] = Machine.mainMemory[m];
	    }
	}
	
	return space;
    }
    
    public void setSpaceId(int spaceId) {
	this.spaceId = spaceId;
    }
    
    public int handleMmap(int size, String fileName, OpenFile file) {
	if(fileMapping.containsKey(fileName)) {
	    Debug.println('+', "File is already mapped");
	    String pair = fileMappingWithAddress.get(fileName);
	    if(pair == null || pair == "")
		return 0;
	    int pAddr = CPU.readRegister(5);
	    int start = Integer.valueOf(pair.split(",")[0]);
	    int end = Integer.valueOf(pair.split(",")[1]);
	    Machine.mainMemory[pAddr] = (byte) ((end - start + 1)/Machine.PageSize);
	    return start;
	}
	int pages = (size / Machine.PageSize);
	if (size % Machine.PageSize > 0)
	    pages++;
	pageFaultFile = file;
	pageFaultFileName = fileName;
	fileMapping.put(fileName, file);
	int vAddress = extendPageTable(pages);
	pageFaultMapping.put(vAddress, pages * Machine.PageSize);
	fileMappingWithAddress.put(fileName, vAddress + "," + ((vAddress + (pages*Machine.PageSize))-1));
	pageFaultFileNameMapping.put(vAddress, fileName);
	return vAddress;
    }
    
    private int extendPageTable(int pages) {
	int size = pageTable.length + pages;
	TranslationEntry[] temp = new TranslationEntry[size];
	int i = 0;
	int vAddress = pageTable.length * Machine.PageSize;
	ArrayList<Integer> pageList;
	if(pageListMapping.containsKey(vAddress)) {
	    pageList = pageListMapping.get(vAddress);
	} else {
	    pageList = new ArrayList<Integer>();
	}
	for(; i < pageTable.length; i++) {
	    temp[i] = new TranslationEntry();
	    temp[i].virtualPage = pageTable[i].virtualPage;
	    temp[i].physicalPage = pageTable[i].physicalPage;
	    temp[i].valid = pageTable[i].valid;
	    temp[i].use = pageTable[i].use;
	    temp[i].dirty = pageTable[i].dirty;
	    temp[i].readOnly = pageTable[i].readOnly;
	}
	int vpn = pageTable[i-1].virtualPage;
	for(int j = i; j < i+pages; j++) {
	    temp[j] = new TranslationEntry();
	    temp[j].virtualPage = ++vpn;
	    temp[j].physicalPage = -1;
	    temp[j].valid = false;
	    temp[j].use = false;
	    temp[j].dirty = false;
	    temp[j].readOnly = false;
	    pageList.add(vpn);
	}
	pageListMapping.put(vAddress, pageList);
	pageTable = temp.clone();
	restoreState();
	temp = null;
	int pAddr = CPU.readRegister(5);
	Machine.mainMemory[pAddr] = (byte) pages;
	size = pageTable.length * Machine.PageSize;
	return vAddress;
    }

    public void handlePageFault(int vAddress) {
	int vpn = vAddress / Machine.PageSize;
	for(TranslationEntry entry: pageTable) {
	    if(entry.virtualPage == vpn) {
		if(entry.physicalPage != -1) {
		    Debug.println('+', "Memory is already mapped");
		    return;
		}
		entry.physicalPage = PhysicalMemoryManager.getInstance().getPage();
		entry.readOnly = true;
		entry.use = true;
		entry.valid = true;
		entry.dirty = false;
		int j = entry.physicalPage * Machine.PageSize;
		byte[] buf = new byte[Machine.PageSize];
		pageFaultFile.readAt(buf, 0, Machine.PageSize, 0);
		int k = 0;
		for(int i = j; i < j + Machine.PageSize; i++) {
		    Machine.mainMemory[i] = buf[k++];
		}
		restoreState();
		break;
	    }
	}
    }

    public void handleMunmap(int vAddress) {
	int vpn = vAddress / Machine.PageSize;
	Iterator<Entry<Integer, Integer>> it = pageFaultMapping.entrySet().iterator();
	Entry<Integer, Integer> mapping;
	while (it.hasNext()) {
	    mapping = it.next();
	    if(mapping.getKey() == vAddress) {
		int expandedMemory = mapping.getValue();
		int endVPN = (vAddress + expandedMemory) / Machine.PageSize;
		TranslationEntry entry;
		for(int i = vpn; i < endVPN; i++) {
		    entry = pageTable[i];
		    if(entry.physicalPage == -1)
			continue;
		    entry.readOnly = false;
		    entry.use = false;
		    entry.valid = false;
		    byte[] buf = new byte[Machine.PageSize];
		    if(entry.dirty) {
			System.arraycopy(Machine.mainMemory, entry.physicalPage*Machine.PageSize,
				buf, 0, Machine.PageSize);
			ArrayList<Integer> pList = pageListMapping.get(vAddress);
			for(int k = 0; k < pList.size(); k++) {
			    if(entry.virtualPage == pList.get(k)) {
				pageFaultFile.writeAt(buf, 0, Machine.PageSize, k * Machine.PageSize);
			    }
			}
		    }
		    entry.dirty = false;
		    entry.physicalPage = -1;
		}
		for(int i = vAddress; i < vAddress + expandedMemory; i++) {
		    Machine.mainMemory[i] = 0;
		}
		restoreState();
		byte[] buf = new byte[1024];
		pageFaultFile.readAt(buf, 0, 1024, 0);
		Debug.println('+', new String(buf));
		
		pageFaultMapping.remove(vAddress);
		String fileName = pageFaultFileNameMapping.get(vAddress);
		fileMapping.remove(fileName);
		fileMappingWithAddress.remove(fileName);
		pageListMapping.remove(vAddress);
		
		break;
	    }
	}
    }

    public void handleMunmap() {
	if(fileMappingWithAddress.isEmpty())
	    return;
	Iterator<Entry<String, String>> it = fileMappingWithAddress.entrySet().iterator();
	Entry<String, String> mapping;
	int vAddress, endVAddress, vpn;
	while (it.hasNext()) {
	    mapping = it.next();
	    String[] pair = mapping.getValue().split(",");
	    vAddress = Integer.valueOf(pair[0]);
	    endVAddress = Integer.valueOf(pair[1]);
	    vpn = vAddress/Machine.PageSize;
		int expandedMemory = endVAddress - vAddress + 1;
		int endVPN = (vAddress + expandedMemory) / Machine.PageSize;
		TranslationEntry entry;
		for(int i = vpn; i < endVPN; i++) {
		    entry = pageTable[i];
		    if(entry.physicalPage == -1)
			continue;
		    entry.readOnly = false;
		    entry.use = false;
		    entry.valid = false;
		    byte[] buf = new byte[Machine.PageSize];
		    if(entry.dirty) {
			System.arraycopy(Machine.mainMemory, entry.physicalPage*Machine.PageSize,
				buf, 0, Machine.PageSize);
			ArrayList<Integer> pList = pageListMapping.get(vAddress);
			for(int k = 0; k < pList.size(); k++) {
			    if(entry.virtualPage == pList.get(k)) {
				pageFaultFile.writeAt(buf, 0, Machine.PageSize, k * Machine.PageSize);
			    }
			}
		    }
		    entry.dirty = false;
		    entry.physicalPage = -1;
		}
		for(int i = vAddress; i < vAddress + expandedMemory; i++) {
		    Machine.mainMemory[i] = 0;
		}
		restoreState();
		byte[] buf = new byte[1024];
		pageFaultFile.readAt(buf, 0, 1024, 0);
		Debug.println('+', new String(buf));
		break;
	}
    }
    
    public void handleReadOnlyException(int vAddress) {
	int vpn = vAddress / Machine.PageSize;
	for (TranslationEntry entry : pageTable) {
	    if(entry.virtualPage == vpn) {
		entry.dirty = true;
		entry.readOnly = false;
		break;
	    }
	}
	restoreState();
    }

}
