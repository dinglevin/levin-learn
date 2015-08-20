package org.jcyclone.core.queue;


/**
 * A sink that additionally supports operations that wait for
 * space to become available in the sink when storing an element.
 *
 * @author Jean Morissette
 */
public interface IBlockingSink extends ISink {

	/**
	 * Adds the specified element to this sink, waiting if necessary for
	 * space to become available.
	 *
	 * @param element the event to enqueue
	 */
	void blockingEnqueue(IElement element) throws InterruptedException;

	/**
	 * Adds the specified element in this sink, waiting if necessary
	 * up to the specified wait time for space to become available.
	 *
	 * @param element        the event to enqueue.
	 * @param timeout_millis the number of milliseconds to wait.
	 * @return true if successful, or false if the specified waiting
	 *         time elapses before space is available.
	 * @throws InterruptedException if interrupted while waiting.
	 */
	boolean enqueueLossy(IElement element, int timeout_millis) throws InterruptedException;

}
