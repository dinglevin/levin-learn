package org.jcyclone.core.queue;

/**
 * @author Jean Morissette
 */
public interface ITransaction {

	/**
	 * Commit a previously prepared provisional enqueue operation (from
	 * the <tt>enqueuePrepare()</tt> method). Causes the provisionally
	 * enqueued elements to appear on the queue for future dequeue operations.
	 */
	void commit();

	/**
	 * Abort a previously prepared provisional enqueue operation (from
	 * the <tt>enqueuePrepare()</tt> method). Causes the queue to discard
	 * the provisionally enqueued elements.
	 */
	void abort();

	/**
	 * Combine the given transaction to the current one. So, committing
	 * the current transaction would also commit the given one, but
	 * not the inverse.
	 * It's provided mainly for internal purpose.
	 */
	void join(ITransaction txn);


	abstract class AbstractTransaction implements ITransaction {

		private ITransaction joinedTxn;

		public void commit() {
			doCommit();
			if (joinedTxn != null)
				joinedTxn.commit();
		}

		public void abort() {
			doAbort();
			if (joinedTxn != null)
				joinedTxn.abort();
		}

		public void join(ITransaction txn) {   // combine transaction
			if (joinedTxn == null)
				joinedTxn = txn;
			else
				joinedTxn.join(txn);
		}

		protected void finalize() throws Throwable {
			abort();
		}

		protected abstract void doCommit();

		protected abstract void doAbort();
	}
}
