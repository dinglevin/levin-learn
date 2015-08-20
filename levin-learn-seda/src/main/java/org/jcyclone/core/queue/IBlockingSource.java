package org.jcyclone.core.queue;

import java.util.List;

/**
 * A ISource that additionally supports operations that wait
 * for the source to become non-empty when retrieving an element.
 *
 * @author Matt Welsh and Jean Morissette
 */
public interface IBlockingSource extends ISource {

	/**
	 * Just like blocking_dequeue_all, but returns only a single element.
	 */
	IElement blockingDequeue(int timeout_millis) throws InterruptedException;

	/**
	 * This method blocks on the queue up until a timeout occurs or
	 * until an element appears on the queue. It returns all elements waiting
	 * on the queue at that time.
	 *
	 * @param msecs if msecs is <code>0</code>, this method
	 *              will be non-blocking and will return right away, whether or not
	 *              any elements are pending on the queue.  If timeout_millis is
	 *              <code>-1</code>, this method blocks forever until something is
	 *              available.  If timeout_millis is positive, this method will wait
	 *              about that number of milliseconds before returning, but possibly a
	 *              little more.
	 * @return an array of <code>IElement</code>'s.  This array will
	 *         be null if no elements were pending.
	 */
	int blockingDequeueAll(List list, int msecs) throws InterruptedException;


	/**
	 * This method blocks on the queue up until a timeout occurs or
	 * until an element appears on the queue. It returns at most
	 * <code>maxElements</code> elements waiting on the queue at that time.
	 */
	int blockingDequeue(List list, int msecs, int maxElements) throws InterruptedException;

}
