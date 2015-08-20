package org.jcyclone.core.queue;

import org.jcyclone.core.profiler.IProfilable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * IQueue implementation that use an array, so no memory
 * allocation occur on queue operations apart from situations
 * where array needs to be resized.
 *
 * @author Jean Morissette
 */
public class DynamicArrayBlockingQueue implements IBlockingQueue, IProfilable {

	private static final boolean DEBUG = false;

	protected IElement[] array;     // the elements

	protected int takePtr = 0;            // circular indices
	protected int putPtr = 0;

	protected int usedSlots = 0;          // length
	protected int emptySlots;             // capacity - length

	protected int waitingTakes;           // counts of waiting threads
	protected int waitingPuts;

	protected volatile int capacity;      // number of elements allowed

	/**
	 * Helper monitor to handle puts.
	 */
	protected final Object putMonitor = new Object();

	/**
	 * Helper monitor to handle takes.
	 */
	protected final Object takeMonitor = new Object();

	// transactional map
	protected Map txnMap = Collections.synchronizedMap(new WeakHashMap());

	/**
	 * Create a queue with the default capacity
	 */
	public DynamicArrayBlockingQueue() {
		this(10, Integer.MAX_VALUE);
	}

	/**
	 * Create a queue with the given capacity.
	 *
	 * @throws IllegalArgumentException if capacity less or equal to zero
	 */
	public DynamicArrayBlockingQueue(int initialCapacity, int maxCapacity) throws IllegalArgumentException {
		if (maxCapacity <= 0) throw new IllegalArgumentException();
		array = new IElement[initialCapacity];
		emptySlots = initialCapacity;
		this.capacity = maxCapacity;
	}


// --------------------- Interface IBlockingSink ---------------------

	public int enqueueWait;

	public void blockingEnqueue(IElement x) throws InterruptedException {
		if (x == null) throw new IllegalArgumentException();
		if (Thread.interrupted()) throw new InterruptedException();
		synchronized (putMonitor) {
			while (emptySlots <= 0) {
				ensureCapacity();
				if (emptySlots > 0) break;
				++waitingPuts;
				enqueueWait++;
				try {
//					long t1 = System.nanoTime();
					putMonitor.wait();
//					long t2 = System.nanoTime();
				} catch (InterruptedException ex) {
					putMonitor.notify();
					throw ex;
				} finally {
					--waitingPuts;
				}
			}
			insert(x);
		}
		incUsedSlots();
	}

	public boolean enqueueLossy(IElement x, int msecs) throws InterruptedException {
		if (x == null) throw new IllegalArgumentException();
		if (Thread.interrupted()) throw new InterruptedException();

		synchronized (putMonitor) {
			long start = (msecs <= 0) ? 0 : System.currentTimeMillis();
			long waitTime = msecs;
			while (emptySlots <= 0) {
				// XXX: The needed capacity increment should be specified,
				// else this method may return even if max capacity
				// is not reached.
				ensureCapacity();
				if (emptySlots > 0) break;
				if (waitTime <= 0) return false;
				++waitingPuts;
				try {
					putMonitor.wait(waitTime);
				} catch (InterruptedException ex) {
					putMonitor.notify();
					throw ex;
				} finally {
					--waitingPuts;
				}
				waitTime = msecs - (System.currentTimeMillis() - start);
			}
			insert(x);
		}
		incUsedSlots();
		return true;
	}

// --------------------- Interface IBlockingSource ---------------------


	public int dequeueAll(List list) {
		int n;
		synchronized (takeMonitor) {
			if (usedSlots <= 0)
				return 0;
			n = extractMany(list);
		}
		incEmptySlots(n);
		return n;
	}

	public int dequeue(List list, int maxElements) {
		if (maxElements <= 0) return 0;
		int n;
		synchronized (takeMonitor) {
			if (usedSlots <= 0)
				return 0;
			n = extractMany(list, maxElements);
		}
		incEmptySlots(n);
		return n;
	}

	public IElement blockingDequeue(int timeout_millis) throws InterruptedException {
		if (timeout_millis == -1)
			return take();
		else
			return poll(timeout_millis);
	}

	public int blockingDequeueAll(List list, int msecs) throws InterruptedException {
		if (msecs < 0)
			return takeMany(list);

		if (Thread.interrupted()) throw new InterruptedException();
		int n;
		synchronized (takeMonitor) {
			long start = (msecs <= 0) ? 0 : System.currentTimeMillis();
			long waitTime = msecs;

			while (usedSlots <= 0) {
				if (waitTime <= 0) return 0;
				++waitingTakes;
				try {
					takeMonitor.wait(waitTime);
				} catch (InterruptedException ex) {
					takeMonitor.notify();
					throw ex;
				} finally {
					--waitingTakes;
				}
				waitTime = msecs - (System.currentTimeMillis() - start);
			}
			n = extractMany(list);
		}
		incEmptySlots(n);
		return n;
	}

	public int blockingDequeue(List list, int timeout_millis, int maxElements)
	    throws InterruptedException {
		if (timeout_millis < 0)
			return takeMany(list, maxElements);
		else
			return pollMany(list, timeout_millis, maxElements);
	}

// --------------------- Interface ISink ---------------------


	public void enqueue(IElement element) throws SinkException {
		try {
			if (!enqueueLossy(element, 0))
				throw new SinkFullException();
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new SinkFullException();
		}
	}

	public boolean enqueueLossy(IElement element) {
		try {
			return enqueueLossy(element, 0);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void enqueueMany(List elements) throws SinkException {
		int size = elements.size();
		synchronized (putMonitor) {
			if (emptySlots < size) {
				ensureCapacity(size);
				if (emptySlots < size)
					throw new SinkFullException();
			}
			insertMany(elements);
		}
		incUsedSlots(size);
	}

	public ITransaction enqueuePrepare(List elements) throws SinkException {
		int size = elements.size();
		synchronized (putMonitor) {
			if (emptySlots < size) {
				ensureCapacity(size);
				if (emptySlots < size)
					throw new SinkFullException();
			}
			emptySlots -= size;
		}
		Txn key = new Txn(size);
		txnMap.put(key, elements);
		return key;
	}

	public void enqueuePrepare(List elements, ITransaction txn) throws SinkException {
		txn.join(enqueuePrepare(elements));
	}

	class Txn extends ITransaction.AbstractTransaction {
		int reservedSize;

		public Txn(int size) {
			this.reservedSize = size;
		}

		protected void doCommit() {
			DynamicArrayBlockingQueue.this.enqueueCommit(this);
		}

		protected void doAbort() {
			DynamicArrayBlockingQueue.this.enqueueAbort(this);
		}
	}

	private void enqueueCommit(Txn key) {
		List list = (List) txnMap.remove(key);
		if (list == null) throw new IllegalArgumentException("unknown key " + key);
		if (list.size() != key.reservedSize) {
			incEmptySlots(key.reservedSize);  // abort
			throw new IllegalStateException("transaction aborted: the size of the provisionally enqueued list has been modified");
		}
		synchronized (putMonitor) {
			for (int i = 0; i < key.reservedSize; i++) {
				array[putPtr] = (IElement) list.get(i);
				if (++putPtr >= array.length) putPtr = 0;
			}
		}
		incUsedSlots(key.reservedSize);
	}

	private void enqueueAbort(Txn key) {
		List list = (List) txnMap.remove(key);
		if (list == null) throw new IllegalArgumentException("unknown key " + key);
		incEmptySlots(key.reservedSize);
	}


// --------------------- Interface ISource ---------------------


	public IElement dequeue() {
		IElement old = null;
		synchronized (takeMonitor) {
			if (usedSlots <= 0) {
				return null;
			}
			old = extract();
		}
		incEmptySlots();
		return old;
	}

	/**
	 * Return the number of elements in the buffer.
	 * This is only a snapshot value, that may change
	 * immediately after returning.
	 */
	public int size() {
		synchronized (takeMonitor) {
			return usedSlots;
		}
	}

// -------------------------- OTHER METHODS --------------------------


	public int capacity() {
		return capacity;
	}

	public void setCapacity(int newCapacity) {
		if (newCapacity <= 0) throw new IllegalArgumentException();
		synchronized (putMonitor) {
			emptySlots += newCapacity - capacity;
			if (newCapacity > capacity) {
				if (waitingPuts > 0)
					putMonitor.notifyAll();
			}
			capacity = newCapacity;
		}
	}

	public IElement peek() {
		synchronized (takeMonitor) {
			if (usedSlots > 0)
				return array[takePtr];
			else
				return null;
		}
	}

	public void trimToSize() {
		synchronized (putMonitor) {
			int oldLength = array.length;
			int newLength;
			synchronized (takeMonitor) {
				IElement[] newArray;

				if (takePtr < putPtr) {
					newLength = putPtr - takePtr;
					newArray = new IElement[newLength];
					System.arraycopy(array, takePtr, newArray, 0, newLength);
				} else if (takePtr == putPtr) {
					// is full?
					if (array[takePtr] != null) {
//            assert isFull();
						// do nothing
						return;
					}
					// the queue is empty
//            assert isEmpty();
					// XXX parametrize the minimal capacity
					newLength = 10;
					newArray = new IElement[newLength];
				} else {
					int rightSideLength = oldLength - takePtr;
					newLength = rightSideLength + putPtr;
					newArray = new IElement[newLength];
					System.arraycopy(array, takePtr, newArray, 0, rightSideLength);
					System.arraycopy(array, 0, newArray, rightSideLength, putPtr);
				}

				putPtr = 0;
				takePtr = 0;
				array = newArray;
				// XXX: write this statement outside of the synchronized (takeMonitor) block?
				emptySlots += newLength - oldLength;
			}
		}
	}

	public int profileSize() {
		return size();
	}

	protected void incEmptySlots() {
		synchronized (putMonitor) {
			++emptySlots;
			if (waitingPuts > 0)
				putMonitor.notify();
		}
	}

	protected void incEmptySlots(int slotCount) {
		synchronized (putMonitor) {
			emptySlots += slotCount;
			if (waitingPuts > 0) {
				if (slotCount >= waitingPuts) {
					putMonitor.notifyAll();
				} else {
					for (int i = 0; i < waitingPuts; i++) {
						putMonitor.notify();
					}
				}
			}
		}
	}

	protected void incUsedSlots() {
		synchronized (takeMonitor) {
			++usedSlots;
			if (waitingTakes > 0)
				takeMonitor.notify();
		}
	}

	protected void incUsedSlots(int slotCount) {
		synchronized (takeMonitor) {
			usedSlots += slotCount;
			if (waitingTakes > 0) {
				if (slotCount >= waitingTakes) {
					takeMonitor.notifyAll();
				} else {
					int waitingTakes = this.waitingTakes;
					for (int i = 0; i < waitingTakes; i++) {
						takeMonitor.notify();
					}
				}
			}
		}
	}

	protected final void insert(IElement x) { // mechanics of put
		--emptySlots;
		array[putPtr] = x;
		if (++putPtr >= array.length) putPtr = 0;
	}

	protected final void insertMany(List elements) {
		int size = elements.size();
		emptySlots -= size;
//		XXX Use a double-for-loop to avoid the usually-false 'if' test
//		if (putPtr + elements.length <= array.length) {
//			for (int i = 0; i < elements.length; i++) {
//				array[putPtr++] = elements[i];
//			}
//		}
//		else {
//			int i = 0;
//			while (putPtr < array.length) {
//				array[putPtr++] = elements[i++];
//			}
//			putPtr = 0;
//			while (i < elements.length) {
//				array[putPtr++] = elements[i++];
//			}
//		}
		for (int i = 0; i < size; i++) {
			array[putPtr] = (IElement) elements.get(i);
			if (++putPtr >= array.length) putPtr = 0;
		}
	}

	protected final IElement extract() { // mechanics of take
		--usedSlots;
		IElement old = array[takePtr];
		array[takePtr] = null;
		if (++takePtr >= array.length) takePtr = 0;
		return old;
	}

	protected final int extractMany(List list) { // mechanics of take
		int n = usedSlots;
		usedSlots = 0;
//	XXX Use a double-for-loop to avoid the usually-false 'if' test
//		if (takePtr + elements.length <= array.length) {
//			for (int i = 0; i < elements.length; i++) {
//				elements[i] = array[takePtr];
//				array[takePtr++] = null;
//			}
//		}
//		else {
//			int i = 0;
//			while (takePtr < array.length) {
//				elements[i] = array[takePtr];
//				array[takePtr++] = null;
//			}
//			takePtr = 0;
//			while (i < elements.length) {
//				elements[i] = array[takePtr];
//				array[takePtr++] = null;
//			}
//		}
		for (int i = 0; i < n; i++) {
			list.add(array[takePtr]);
			array[takePtr] = null;
			if (++takePtr >= array.length) takePtr = 0;
		}
		return n;
	}

	protected final int extractMany(List list, int maxElements) { // mechanics of take
		int count = Math.min(usedSlots, maxElements);
		usedSlots -= count;
//	XXX Use a double-for-loop to avoid the usually-false 'if' test
		for (int i = 0; i < count; i++) {
			list.add(array[takePtr]);
			array[takePtr] = null;
			if (++takePtr >= array.length) takePtr = 0;
		}
		return count;
	}

	public int takeWait;

	public IElement take() throws InterruptedException {
		if (Thread.interrupted()) throw new InterruptedException();
		IElement old = null;
		synchronized (takeMonitor) {
			while (usedSlots <= 0) {
				++waitingTakes;
				takeWait++;
				try {
//					long t1 = System.nanoTime();
					takeMonitor.wait();
//					long t2 = System.nanoTime();
//					takeWaitTime += t2-t1;
				} catch (InterruptedException ex) {
					takeMonitor.notify();
					throw ex;
				} finally {
					--waitingTakes;
				}
			}
			old = extract();
		}
		incEmptySlots();
		return old;
	}

	private IElement poll(int msecs) throws InterruptedException {
		if (Thread.interrupted()) throw new InterruptedException();
		IElement old = null;
		synchronized (takeMonitor) {
			long start = (msecs <= 0) ? 0 : System.currentTimeMillis();
			long waitTime = msecs;

			while (usedSlots <= 0) {
				if (waitTime <= 0) return null;
				++waitingTakes;
				try {
					takeMonitor.wait(waitTime);
				} catch (InterruptedException ex) {
					takeMonitor.notify();
					throw ex;
				} finally {
					--waitingTakes;
				}
				waitTime = msecs - (System.currentTimeMillis() - start);
			}
			old = extract();
		}
		incEmptySlots();
		return old;
	}

	private int pollMany(List list, int msecs, int maxElements) throws InterruptedException {
		if (maxElements <= 0) return 0;
		if (Thread.interrupted()) throw new InterruptedException();
		int n;
		synchronized (takeMonitor) {
			long start = (msecs <= 0) ? 0 : System.currentTimeMillis();
			long waitTime = msecs;

			while (usedSlots <= 0) {
				if (waitTime <= 0) return 0;
				++waitingTakes;
				try {
					takeMonitor.wait(waitTime);
				} catch (InterruptedException ex) {
					takeMonitor.notify();
					throw ex;
				} finally {
					--waitingTakes;
				}
				waitTime = msecs - (System.currentTimeMillis() - start);
			}
			n = extractMany(list, maxElements);
		}
		incEmptySlots(n);
		return n;
	}

	private int takeMany(List list) throws InterruptedException {
		if (Thread.interrupted()) throw new InterruptedException();
		int n;
		synchronized (takeMonitor) {
			while (usedSlots <= 0) {
				++waitingTakes;
				try {
					takeMonitor.wait();
				} catch (InterruptedException ex) {
					takeMonitor.notify();
					throw ex;
				} finally {
					--waitingTakes;
				}
			}
			n = extractMany(list);
		}
		incEmptySlots(n);
		return n;
	}

	private int takeMany(List list, int maxElements) throws InterruptedException {
		if (maxElements <= 0) return 0;
		if (Thread.interrupted()) throw new InterruptedException();
		int n;
		synchronized (takeMonitor) {
			while (usedSlots <= 0) {
				++waitingTakes;
				try {
					takeMonitor.wait();
				} catch (InterruptedException ex) {
					takeMonitor.notify();
					throw ex;
				} finally {
					--waitingTakes;
				}
			}
			n = extractMany(list, maxElements);
		}
		incEmptySlots(n);
		return n;
	}

	private void ensureCapacity(int minIncrement) {
		if (array.length < capacity) {
			int oldLength = array.length;
			// XXX the increment factor should be parametrizable
			int newLength = (int) Math.max(oldLength + minIncrement, (oldLength * 3) / 2 + 1);
			if (newLength > capacity) {
				newLength = capacity;
			}
			doEnsureCapacity(newLength);
		}
	}

	private void ensureCapacity() {
		if (array.length < capacity) {
			// XXX the increment factor should be parametrizable
			int newLength = (int) Math.min(capacity, (array.length * 3) / 2 + 1);
			doEnsureCapacity(newLength);
		}
	}

	private void doEnsureCapacity(int newLength) {
		int oldLength = array.length;
		synchronized (takeMonitor) {
			IElement[] newArray;

			if (takePtr < putPtr) {
				newArray = new IElement[newLength];
				int length = putPtr - takePtr;
				System.arraycopy(array, takePtr, newArray, 0, length);
				putPtr -= takePtr;
			} else if (takePtr == putPtr) {
				// is empty?
				if (array[takePtr] == null) {
//            assert isEmpty();
					newArray = new IElement[newLength];
					putPtr = 0;
				} else {
					// the queue is full
//          assert isFull();
					newArray = new IElement[newLength];
					int rightSideLength = oldLength - takePtr;
					System.arraycopy(array, takePtr, newArray, 0, rightSideLength);
					System.arraycopy(array, 0, newArray, rightSideLength, putPtr);
					putPtr += rightSideLength;   // XXX putPtr = oldLength?
				}
			} else {
				newArray = new IElement[newLength];
				int rightSideLength = oldLength - takePtr;
				System.arraycopy(array, takePtr, newArray, 0, rightSideLength);
				System.arraycopy(array, 0, newArray, rightSideLength, putPtr);
				putPtr += rightSideLength;   // XXX putPtr = oldLength?
			}

			takePtr = 0;
			emptySlots += newLength - oldLength;
			array = newArray;
//			assert (array.length <= capacity);
		}
	}

	// only for debugging purpose
	private boolean isFull() {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == null)
				return false;
		}
		return true;
	}

	// only for debugging purpose
	private boolean isEmpty() {
		for (int i = 0; i < array.length; i++) {
			if (array[i] != null)
				return false;
		}
		return true;
	}

}

