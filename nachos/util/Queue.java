package nachos.util;

/**
 * This is a restricted version of java.util.Queue that only specifies
 * the methods that are actually used in Nachos.
 * 
 * @author Eugene W. Stark
 * @version 20140117
 */

public interface Queue<T> {
    
    /**
     * Adds an element to this queue, if it is possible to do so immediately
     * without violating capacity restrictions.
     * 
     * @param e  The element to add.
     * @return  true if the element was successfully added, false if the element
     * was not added.
     */
    public boolean offer(T e);
    
    /**
     * Retrieves, but does not remove, the head of this queue, or returns null
     * if this queue is empty.
     * 
     * @return  The element at the head of the queue, or null if the queue is
     * empty.
     */
    public T peek();
    
    /**
     * Retrieves and removes the head of this queue, or returns null if this queue is empty.
     * 
     * @return  the head of this queue, or null if this queue is empty.
     */
    public T poll();
    
    /**
     * Test whether this queue is currently empty.
     * 
     * @return true if this queue is currently empty.
     */
    public boolean isEmpty();
    
}
