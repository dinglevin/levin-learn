package org.jcyclone.core.timer;

import org.jcyclone.core.queue.IElement;
import org.jcyclone.core.queue.ISink;

import java.util.Date;

/**
 * The Timer class provides a mechanism for registering
 * timer events that will go off at some future time.  The future time
 * can be specified in absolute or relative terms.  When the timer goes
 * off, an element is placed on a queue.  There is no way to unregister
 * a timer.  Events will be delivered guaranteed, but the time that they
 * are delivered may slip depending on stuff like how loaded the system
 * is.
 * <P>
 * WARNING: you should use cancelEvent to cancel timers that you no longer
 * need, otherwise you will waste many, many cycles on unneeded timer
 * firings.
 *
 * @author Matt Welsh and Steve Gribble
 */
public interface ITimer {

	ITimerEvent registerEvent(long millis, IElement evt, ISink queue);

	ITimerEvent registerEvent(Date the_date, IElement evt, ISink queue);

	int size();

	void cancelAll();

	void cancelEvent(ITimerEvent timerEvt);
}
