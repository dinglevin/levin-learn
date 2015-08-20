package org.jcyclone.core.queue;

import org.jcyclone.core.profiler.IProfilable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * A IBlockingQueue implementation that may have higher throughput
 * than array-based queues when there is a lot of contention, because
 * it does not pre-allocate fixed storage for elements.
 *
 * @author Jean Morissette
 */
public class LinkedBlockingQueue implements IBlockingQueue, IProfilable {

	private class LinkedNode {
		LinkedNode next;
		IElement value;

		LinkedNode() {
		}

		LinkedNode(IElement x) {
			value = x;
		}

		LinkedNode(IElement x, LinkedNode n) {
			value = x;
			next = n;
		}
	}

	/**
	 * Dummy header node of list. The first actual node, if it exists, is always
	 * at head.next. After each take, the old first node becomes the head.
	 */
	protected LinkedNode head;

	/**
	 * The last node of list. Put() appends to list, so modifies last
	 */
	protected LinkedNode last;

	/**
	 * Helper monitor. Ensures that only one put at a time executes.
	 */
	protected final Object putGuard = new Object();

	/**
	 * Helper monitor. Protects and provides wait queue for takes
	 */
	protected final Object takeGuard = new Object();

	/**
	 * Number of elements allowed
	 */
	protected int capacity;

	// transactional map
	protected Map txnMap = Collections.synchronizedMap(new WeakHashMap());


	/**
	 * One side of a split permit count.
	 * The counts represent permits to do a put. (The queue is full when zero).
	 * Invariant: putSidePutPermits + takeSidePutPermits = capacity - length.
	 * (The length is never separately recorded, so this cannot be
	 * checked explicitly.)
	 * To minimize contention between puts and takes, the
	 * put side uses up all of its permits before transfering them from
	 * the take side. The take side just increments the count upon each take.
	 * Thus, most puts and take can run independently of each other unless
	 * the queue is empty or full.
	 * Initial value is queue capacity.
	 */

	protected int putSidePutPermits;

	/**
	 * Number of takes since last reconcile
	 */
	protected int takeSidePutPermits = 0;


	/**
	 * Create a queue with the given capacity
	 *
	 * @throws IllegalArgumentException if capacity less or equal to zero
	 */
	public LinkedBlockingQueue(int capacity) {
		if (capacity <= 0) throw new IllegalArgumentException();
		this.capacity = capacity;
		putSidePutPermits = capacity;
		head = new LinkedNode(null);
		last = head;
	}

	/**
	 * Create a queue with the current default capacity
	 */

	public LinkedBlockingQueue() {
		this(Integer.MAX_VALUE);
	}

	/**
	 * Return the current capacity of this queue
	 */
	public synchronized int capacity() {
		return capacity;
	}

	public void enqueue(IElement x) throws SinkException {
		if (x == null) throw new NullPointerException();
		synchronized (putGuard) {
			if (putSidePutPermits <= 0) {
				synchronized (this) {
					if (reconcilePutPermits() <= 0)
						throw new SinkFullException();
				}
			}
			insert(x);
		}
		// call outside of lock to loosen put/take coupling
		allowTake();
	}

	public boolean enqueueLossy(IElement x) {
		if (x == null) throw new IllegalArgumentException();
		synchronized (putGuard) {
			if (putSidePutPermits <= 0) {
				synchronized (this) {
					if (reconcilePutPermits() <= 0)
						return false;
				}
			}
			insert(x);
		}
		// call outside of lock to loosen put/take coupling
		allowTake();
		return true;
	}

	public void enqueueMany(List list) throws SinkException {
		if (list == null) throw new IllegalArgumentException();
		int size = list.size();
		synchronized (putGuard) {
			if (putSidePutPermits < size) {
				synchronized (this) {
					if (reconcilePutPermits() <= size)
						throw new SinkFullException();
				}
			}
			insertMany(list);
		}
		// call outside of lock to loosen put/take coupling
		allowAllTake();
	}

	public ITransaction enqueuePrepare(List elements) throws SinkException {
		if (elements == null) throw new IllegalArgumentException();
		int size = elements.size();
		synchronized (putGuard) {
			if (putSidePutPermits < size) {
				synchronized (this) {
					if (reconcilePutPermits() <= 0)
						throw new SinkFullException();
				}
			}
			putSidePutPermits -= size;
		}
		Txn key = new Txn(size);
		txnMap.put(key, elements);
		return key;
	}


	public void blockingEnqueue(IElement x) throws InterruptedException {
		if (x == null) throw new IllegalArgumentException();
		if (Thread.interrupted()) throw new InterruptedException();

		synchronized (putGuard) {
			if (putSidePutPermits <= 0) { // wait for permit.
				synchronized (this) {
					if (reconcilePutPermits() <= 0) {
						try {
							for (; ;) {
								wait();
								if (reconcilePutPermits() > 0) {
									break;
								}
							}
						} catch (InterruptedException ex) {
							notify();
							throw ex;
						}
					}
				}
			}
			insert(x);
		}
		// call outside of lock to loosen put/take coupling
		allowTake();
	}

	public boolean enqueueLossy(IElement x, int msecs)
	    throws InterruptedException {
		if (x == null) throw new IllegalArgumentException();
		if (Thread.interrupted()) throw new InterruptedException();

		synchronized (putGuard) {

			if (putSidePutPermits <= 0) {
				synchronized (this) {
					if (reconcilePutPermits() <= 0) {
						if (msecs <= 0)
							return false;
						else {
							try {
								long waitTime = msecs;
								long start = System.currentTimeMillis();

								for (; ;) {
									wait(waitTime);
									if (reconcilePutPermits() > 0) {
										break;
									} else {
										waitTime = msecs - (System.currentTimeMillis() - start);
										if (waitTime <= 0) {
											return false;
										}
									}
								}
							} catch (InterruptedException ex) {
								notify();
								throw ex;
							}
						}
					}
				}
			}

			insert(x);
		}

		allowTake();
		return true;
	}

	public boolean isEmpty() {
		synchronized (head) {
			return head.next == null;
		}
	}

	public int profileSize() {
		return size();
	}

	public void enqueuePrepare(List elements, ITransaction txn) throws SinkException {
		txn.join(enqueuePrepare(elements));
	}

	/**
	 * Return the number of elements in the queue.
	 * This is only a snapshot value, that may be in the midst
	 * of changing. The returned value will be unreliable in the presence of
	 * active puts and takes, and should only be used as a heuristic
	 * estimate, for example for resource monitoring purposes.
	 */
	public synchronized int size() {
		/*
		This should ideally synch on putGuard, but
		doing so would cause it to block waiting for an in-progress
		put, which might be stuck. So we instead use whatever
		value of putSidePutPermits that we happen to read.
		*/
		return capacity - (takeSidePutPermits + putSidePutPermits);
	}

	/**
	 * Reset the capacity of this queue.
	 * If the new capacity is less than the old capacity,
	 * existing elements are NOT removed, but
	 * incoming puts will not proceed until the number of elements
	 * is less than the new capacity.
	 *
	 * @throws IllegalArgumentException if capacity less or equal to zero
	 */
	public void setCapacity(int newCapacity) {
		if (newCapacity <= 0) throw new IllegalArgumentException();
		synchronized (putGuard) {
			synchronized (this) {
				takeSidePutPermits += (newCapacity - capacity);
				capacity = newCapacity;

				// Force immediate reconcilation.
				reconcilePutPermits();
				notifyAll();
			}
		}
	}

	public int blockingDequeueAll(List list, int msecs) throws InterruptedException {
		if (msecs < 0)
			return takeAll(list);

		if (Thread.interrupted()) throw new InterruptedException();
		if (list == null) throw new NullPointerException();
		int n = extractAll(list);
		if (n > 0)
			return n;
		else {
			synchronized (takeGuard) {
				try {
					long waitTime = msecs;
					long start = (msecs <= 0) ? 0 : System.currentTimeMillis();
					for (; ;) {
						n = extractAll(list);
						if (n > 0 || waitTime <= 0) {
							return n;
						} else {
							takeGuard.wait(waitTime);
							waitTime = msecs - (System.currentTimeMillis() - start);
						}
					}
				} catch (InterruptedException ex) {
					takeGuard.notify();
					throw ex;
				}
			}
		}
	}

	public IElement peek() {
		synchronized (head) {
			LinkedNode first = head.next;
			if (first != null)
				return first.value;
			else
				return null;
		}
	}

	public IElement dequeue() {
		return extract();
	}

	public int dequeueAll(List list) {
		if (list == null) throw new NullPointerException();
		return extractAll(list);
	}

	public int dequeue(List list, int maxElements) {
		if (list == null) throw new NullPointerException();
		return extract(list, maxElements);
	}

	public IElement blockingDequeue(int timeout_millis)
	    throws InterruptedException {
		if (timeout_millis < 0)
			return blocking_dequeue();
		else
			return blocking_dequeue(timeout_millis);
	}

	public int blockingDequeue(List list, int msecs, int maxElements) throws InterruptedException {
		if (Thread.interrupted()) throw new InterruptedException();
		int n = extract(list, maxElements);
		if (n > 0)
			return n;
		else {
			synchronized (takeGuard) {
				try {
					long waitTime = msecs;
					long start = (msecs <= 0) ? 0 : System.currentTimeMillis();
					for (; ;) {
						n = extract(list, maxElements);
						if (n > 0 || waitTime <= 0) {
							return n;
						} else {
							takeGuard.wait(waitTime);
							waitTime = msecs - (System.currentTimeMillis() - start);
						}
					}
				} catch (InterruptedException ex) {
					takeGuard.notify();
					throw ex;
				}
			}
		}
	}

	/**
	 * Notify a waiting take if needed
	 */
	protected final void allowTake() {
		synchronized (takeGuard) {
			takeGuard.notify();
		}
	}

	/**
	 * Notify all waiting take if needed
	 */
	protected final void allowAllTake() {
		// TODO: keep track of the number of waiters and call notify only for these waiters
		synchronized (takeGuard) {
			takeGuard.notifyAll();
		}
	}

	/**
	 * Create and insert a node.
	 * Call only under synch on putGuard
	 */
	protected void insert(IElement x) {
		--putSidePutPermits;
		LinkedNode p = new LinkedNode(x);
		synchronized (last) {
			last.next = p;
			last = p;
		}
	}

	protected void insertMany(List list) {
		int size = list.size();
		putSidePutPermits -= size;
		LinkedNode n = new LinkedNode((IElement) list.get(0));
		LinkedNode last = n;
		for (int i = 1; i < size; i++) {
			last.next = new LinkedNode((IElement) list.get(i));
			last = last.next;
		}
		synchronized (this.last) {
			this.last.next = n;
			this.last = n;
		}
	}

	/**
	 * Move put permits from take side to put side;
	 * return the number of put side permits that are available.
	 * Call only under synch on putGuard AND this.
	 */
	protected final int reconcilePutPermits() {
		putSidePutPermits += takeSidePutPermits;
		takeSidePutPermits = 0;
		return putSidePutPermits;
	}

	private IElement blocking_dequeue() throws InterruptedException {
		if (Thread.interrupted()) throw new InterruptedException();
		IElement x = extract();
		if (x != null)
			return x;
		else {
			synchronized (takeGuard) {
				try {
					for (; ;) {
						x = extract();
						if (x != null) {
							return x;
						} else {
							takeGuard.wait();
						}
					}
				} catch (InterruptedException ex) {
					takeGuard.notify();
					throw ex;
				}
			}
		}
	}

	private IElement blocking_dequeue(int msecs) throws InterruptedException {
		if (Thread.interrupted()) throw new InterruptedException();
		IElement x = extract();
		if (x != null)
			return x;
		else {
			synchronized (takeGuard) {
				try {
					long waitTime = msecs;
					long start = (msecs <= 0) ? 0 : System.currentTimeMillis();
					for (; ;) {
						x = extract();
						if (x != null || waitTime <= 0) {
							return x;
						} else {
							takeGuard.wait(waitTime);
							waitTime = msecs - (System.currentTimeMillis() - start);
						}
					}
				} catch (InterruptedException ex) {
					takeGuard.notify();
					throw ex;
				}
			}
		}
	}

	private int takeAll(List list) throws InterruptedException {
		if (Thread.interrupted()) throw new InterruptedException();
		if (list == null) throw new NullPointerException();
		int n = extractAll(list);
		if (n > 0)
			return n;
		else {
			synchronized (takeGuard) {
				try {
					for (; ;) {
						n = extractAll(list);
						if (n > 0) {
							return n;
						} else {
							takeGuard.wait();
						}
					}
				} catch (InterruptedException ex) {
					takeGuard.notify();
					throw ex;
				}
			}
		}
	}

	/**
	 * Main mechanics for take/poll
	 */
	protected synchronized IElement extract() {
		synchronized (head) {
			IElement x = null;
			LinkedNode first = head.next;
			if (first != null) {
				x = first.value;
				first.value = null;
				head = first;
				++takeSidePutPermits;
				notify();
			}
			return x;
		}
	}

	private int extractAll(List list) {

		LinkedNode first;
		LinkedNode tail;
		int count = 0;

		synchronized (this) {
			int maxElements = this.size();
			first = head;
			tail = head;
			while (count < maxElements) {
				synchronized (tail) {
					if (tail.next == null) break;
					tail = tail.next;
				}
				count++;
			}
			head = tail;
			takeSidePutPermits += count;
			notify();
		}

		// Transfer the elements outside of locks
		LinkedNode p = first;
		while (p != tail) {
			p = p.next;
			list.add(p.value);
			p.value = null;
		}
		return count;
	}

	private int extract(List list, int maxElements) {
		if (maxElements <= 0)
			return 0;

		LinkedNode first;
		LinkedNode tail;
		int count = 0;

		synchronized (this) {
			int sizeBound = this.size();
			if (maxElements > sizeBound) {
				maxElements = sizeBound;
			}
			first = head;
			tail = head;
			while (count < maxElements) {
				synchronized (tail) {
					if (tail.next == null) break;
					tail = tail.next;
				}
				count++;
			}
			head = tail;
			takeSidePutPermits += count;
			notify();
		}

		// Transfer the elements outside of locks
		LinkedNode p = first;
		while (p != tail) {
			p = p.next;
			list.add(p.value);
			p.value = null;
		}
		return count;
	}

	private void enqueueCommit(Txn key) {
		List list = (List) txnMap.remove(key);
		if (list == null) throw new IllegalArgumentException("unknown key " + key);
		if (list.size() != key.reservedSize) {
			// abort
			abort(key.reservedSize);
			throw new IllegalStateException("transaction aborted: the size of the provisionally enqueued list has been modified");
		}
		synchronized (putGuard) {
			LinkedNode n = new LinkedNode((IElement) list.get(0));
			LinkedNode last = n;
			for (int i = 1; i < list.size(); i++) {
				last.next = new LinkedNode((IElement) list.get(i));
				last = last.next;
			}
			synchronized (this.last) {
				this.last.next = n;
				this.last = n;
			}
		}
		// call outside of lock to loosen put/take coupling
		allowAllTake();
	}

	private void enqueueAbort(Txn key) {
		List list = (List) txnMap.remove(key);
		if (list == null) throw new IllegalArgumentException("unknown key " + key);
		abort(key.reservedSize);
	}

	private synchronized void abort(int releasedPermits) {
		takeSidePutPermits += releasedPermits;
		// TODO: keep track of the number of waiters and call notify only for these waiters
		notifyAll();
	}

	class Txn extends ITransaction.AbstractTransaction {
		int reservedSize;

		public Txn(int size) {
			this.reservedSize = size;
		}

		protected void doCommit() {
			LinkedBlockingQueue.this.enqueueCommit(this);
		}

		protected void doAbort() {
			LinkedBlockingQueue.this.enqueueAbort(this);
		}
	}

}
