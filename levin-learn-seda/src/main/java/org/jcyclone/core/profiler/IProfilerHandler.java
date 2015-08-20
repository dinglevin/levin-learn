package org.jcyclone.core.profiler;


import org.jcyclone.core.stage.IStageManager;

/**
 * A <tt>IProfilerHandler</tt> object receives values from a
 * <tt>IProfiler</tt> and exports them.  It might for example,
 * show them graphically on the screen or write them to a file.
 */
public interface IProfilerHandler {

	void init(IStageManager mgr);

	/**
	 * This method is called when a profilable object has been
	 * added by the IProfiler.
	 */
	void profilableAdded(String name);

	/**
	 * This method is called when a profilable object has been
	 * removed by the IProfiler.
	 */
	void profilableRemoved(String name);

	void sampleDelayChanged(int newDelay);

	/**
	 * This method is called when the system profiler has profiled
	 * the added profilable objects.  The given array is sorted in
	 * order of which profilable objects has been added.
	 */
	void profilablesSnapshot(int[] sizes);

	void destroy();

}
