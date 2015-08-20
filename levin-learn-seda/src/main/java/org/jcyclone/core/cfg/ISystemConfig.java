package org.jcyclone.core.cfg;

import java.util.Enumeration;


public interface ISystemConfig {

	/**
	 * Returns true if the given key is set in the configuration.
	 */
	boolean contains(String key);

	/**
	 * Return the configuration option associated with the given key
	 * as a String. Returns null if not set.
	 */
	String getString(String key);

	/**
	 * Return the configuration option associated with the given key
	 * as a String. Returns default if not set.
	 */
	String getString(String key, String defaultval);

	/**
	 * Return the configuration option associated with the given key
	 * as a boolean. Returns false if not set.
	 */
	boolean getBoolean(String key);

	/**
	 * Return the configuration option associated with the given key
	 * as a boolean. Returns default if not set.
	 */
	boolean getBoolean(String key, boolean defaultval);

	/**
	 * Return the configuration option associated with the given key
	 * as an int. Returns -1 if not set or if the value of the key cannot
	 * be expressed as an int.
	 */
	int getInt(String key);

	/**
	 * Return the configuration option associated with the given key
	 * as an int. Returns default if not set or if the value of the
	 * key cannot be expressed as an int.
	 */
	int getInt(String key, int defaultval);

	/**
	 * Return the configuration option associated with the given key
	 * as a long. Returns -1 if not set or if the value of the key cannot
	 * be expressed as a long.
	 */
	long getLong(String key);

	/**
	 * Return the configuration option associated with the given key
	 * as a long. Returns default if not set or if the value of the
	 * key cannot be expressed as a long.
	 */
	long getLong(String key, long defaultval);

	/**
	 * Return the configuration option associated with the given key
	 * as a double. Returns -1 if not set or if the value of the key cannot
	 * be expressed as a double.
	 */
	double getDouble(String key);

	/**
	 * Return the configuration option associated with the given key
	 * as a double. Returns default if not set or if the value of the
	 * key cannot be expressed as a double.
	 */
	double getDouble(String key, double defaultval);

	/**
	 * Get the string list value corresponding to the given key.
	 * Returns null if not set.
	 */
	String[] getStringList(String key);

	/**
	 * Return the configuration option associated with the given key
	 * as an object. Returns null if not set.
	 */
	Object getObject(String key);

	/**
	 * Return the configuration option associated with the given key
	 * as an object. Returns default if not set.
	 */
	Object getObject(String key, Object defaultval);

	/**
	 * Return an enumeration of the keys matching the given prefix.
	 * A given key maps onto a set of child keys if it ends in a
	 * "." character (that is, it is an internal node within the tree).
	 * A key not ending in "." is a terminal node and maps onto a
	 * value that may be obtained using getString, getInt, or getDouble.
	 */
	Enumeration getKeys(String prefix);

	/**
	 * Return an enumeration of the top-level keys in this configuration.
	 */
	Enumeration getKeys();

	/**
	 * Return a copy of this object.
	 */
	ISystemConfig getCopy();

	/**
	 * Return an array of the stage names specified by this IGlobalConfig.
	 */
	String[] getStageNames();

}
