package org.jcyclone.core.queue;


/**
 * A IQueue that additionally supports operations that wait for
 * the queue to become non-empty when retrieving an element, and
 * wait for space to become available in the queue when storing
 * an element.
 *
 * @author Jean Morissette
 */
public interface IBlockingQueue extends IQueue, IBlockingSink, IBlockingSource {
}
