package nachos.kernel.userprog;

import java.util.Arrays;
import java.util.HashMap;

import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

public class PhysicalMemoryManager {

    static PhysicalMemoryManager pmm = null;

    private int[] pages;

    private HashMap<String, AddrSpace> spaceIdTracker = new HashMap<String, AddrSpace>();

    private int maxPages;

    /**
     * Synchronized Physical Memory Manager constructor
     * 
     * @return instance of Physical Memory Manager class
     */
    public synchronized static PhysicalMemoryManager getInstance() {
	if (pmm == null)
	    pmm = new PhysicalMemoryManager();
	return pmm;
    }

    /**
     * Initializes page tracker.
     */
    public void setPages() {
	int totalPages = Machine.mainMemory.length / Machine.PageSize;
	maxPages = totalPages;
	pages = new int[totalPages];
	Arrays.fill(pages, 0, totalPages, 0);
    }

    /**
     * Returns unused page
     * 
     * @return unused page
     */
    public int getPage() {
	int i = 0;
	for (; i < pages.length; i++) {
	    if (pages[i] == 0) {
		pages[i] = 1;
		return i;
	    }
	}
	return -1;
    }

    /**
     * Keeps track of the processes and the corresponding address spaces
     * 
     * @param pId
     *            Process id
     * @param space
     *            Address space of the process
     */
    public void mapProcessIdToAddressSpace(String pId, AddrSpace space) {
	spaceIdTracker.put(pId, space);
    }

    /**
     * Returns the address space of the process identified by it's id
     * 
     * @param pId
     *            Process id
     * @return address space
     */
    public AddrSpace getAddressSpaceFromProcessId(String pId) {
	return spaceIdTracker.get(pId);
    }

    /**
     * Returns the total number of physical pages (used + unused) available in
     * the memory
     * 
     * @return total number of physical pages
     */
    public int getMaxPages() {
	return maxPages;
    }

    /**
     * Returns the Physical page tracker list
     * 
     * @return physical page tracker
     */
    public int[] getPages() {
	return pages;
    }

    /**
     * Destroys the book keeping data such as Page tracker, etc. Needs to be
     * called when any process is being destroyed.
     */
    public void destroy() {
	pages = null;
	spaceIdTracker = null;
	pmm = null;
    }

    public void destroySpace(TranslationEntry[] pageTable) {
	for (int i = 0; i < pageTable.length; i++) {
	    if(pageTable[i].physicalPage < 0)
	    	continue;
	    pages[pageTable[i].physicalPage] = 0;
	}
    }

    public int getAvailablePages() {
	int counter = 0;
	for (int i = 0; i < pages.length; i++) {
	    if (pages[i] == 1) {
		++counter;
	    }
	}
	return counter;
    }
}
