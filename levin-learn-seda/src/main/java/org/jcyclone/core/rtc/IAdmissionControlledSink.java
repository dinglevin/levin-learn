package org.jcyclone.core.rtc;

import org.jcyclone.core.queue.IBlockingSink;

public interface IAdmissionControlledSink extends IBlockingSink {

	/**
	 * Set the enqueue predicate for this sink. This mechanism allows
	 * user to define a method that will 'screen' QueueElementIF's during
	 * the enqueue procedure to either accept or reject them. The enqueue
	 * predicate runs in the context of the <b>caller of enqueue()</b>,
	 * which means it must be simple and fast. This can be used to implement
	 * many interesting queue-thresholding policies, such as simple count
	 * threshold, credit-based mechanisms, and more.
	 */
	void setEnqueuePredicate(IEnqueuePredicate pred);

	/**
	 * Return the enqueue predicate for this sink.
	 */
	IEnqueuePredicate getEnqueuePredicate();

}
