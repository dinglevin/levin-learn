package org.jcyclone.core.profiler;

import org.jcyclone.core.cfg.ISystemConfig;

/**
 * A IProfilerFilter can be used to provide control over what
 * is profiled.
 * <p/>
 * The system IProfiler can have a filter associated with it.
 * The IProfiler will call the isProfilable method to check if
 * the given profilable should be profiled.
 *
 * @author Jean Morissette
 */
public interface IProfilerFilter {

	void init(ISystemConfig config);

	/**
	 * Check if a given profilable should be profiled.
	 *
	 * @param name the name of the object being profiled.
	 * @return true if the value should be published.
	 */
	boolean isProfilable(String name);

}