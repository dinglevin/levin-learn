package org.jcyclone.core.queue;

import java.util.List;


public class QueueProxy implements IQueue {

	IQueue q;

	public QueueProxy(IQueue q) {
		this.q = q;
	}

	public IQueue getQueue() {
		return q;
	}

	public void setQueue(IQueue queue) {
		this.q = queue;
	}

	public IElement dequeue() {
		return q.dequeue();
	}

	public int dequeueAll(List list) {
		return q.dequeueAll(list);
	}

	public int dequeue(List list, int maxElements) {
		return dequeue(list, maxElements);
	}

	public IElement blockingDequeue(int timeout_millis) throws InterruptedException {
		return q.blockingDequeue(timeout_millis);
	}

	public int blockingDequeueAll(List list, int msecs) throws InterruptedException {
		return q.blockingDequeueAll(list, msecs);
	}

	public int blockingDequeue(List list, int msecs, int maxElements) throws InterruptedException {
		return q.blockingDequeue(list, msecs, maxElements);
	}

	public int size() {
		return q.size();
	}

	public void setCapacity(int newCapacity) {
		q.setCapacity(newCapacity);
	}

	public int capacity() {
		return q.capacity();
	}

	public void enqueue(IElement element)
	    throws SinkException {
		q.enqueue(element);
	}

	public boolean enqueueLossy(IElement element) {
		return q.enqueueLossy(element);
	}

	public void enqueueMany(List list) throws SinkException {
		q.enqueueMany(list);
	}

	public ITransaction enqueuePrepare(List elements) throws SinkException {
		return q.enqueuePrepare(elements);
	}

	public void enqueuePrepare(List elements, ITransaction txn) throws SinkException {
		q.enqueuePrepare(elements, txn);
	}

}
