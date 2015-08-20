package org.jcyclone.core.cfg;

import org.jcyclone.core.stage.IStage;
import org.jcyclone.core.stage.IStageManager;
import org.jcyclone.core.stage.NoSuchStageException;

import java.util.Enumeration;

/**
 * This class provide a 'viewport' on IGlobalConfig,
 * so the config changes are seen immediately.
 *
 * @author Jean Morissette and others
 */
public class ConfigDataProxy implements IConfigData {

	private IStageManager mgr;
	private ISystemConfig cfg;
	private String stagename;
	private String prefix;

	public ConfigDataProxy(IStageManager mgr, String stagename) {
		this.mgr = mgr;
		this.cfg = mgr.getConfig();
		this.stagename = stagename;

		// XXX This class should be more generic: that is, not usable only with EventHandler
		this.prefix = "stages." + stagename + ".initargs.";
	}

	/**
	 * Returns true if the given key is set.
	 */
	public boolean contains(String key) {
		return cfg.contains(prefix + key);
	}

	/**
	 * Returns the key set.
	 */
	public Enumeration getKeys() {
		return cfg.getKeys(prefix);
	}

	/**
	 * Get the string value corresponding to the given key.
	 * Returns null if not set.
	 */
	public String getString(String key) {
		String s = prefix + key;
		return cfg.getString(s);
	}

	public String getString(String key, String def) {
		return cfg.getString(prefix + key, def);
	}

	/**
	 * Get the integer value corresponding to the given key.
	 * Returns -1 if not set or if the value is not an integer.
	 */
	public int getInt(String key) {
		return cfg.getInt(prefix + key);
	}

	public int getInt(String key, int def) {
		return cfg.getInt(prefix + key, def);
	}

	/**
	 * Get the long value corresponding to the given key.
	 * Returns -1 if not set or if the value is not an integer.
	 */
	public long getLong(String key) {
		return cfg.getLong(prefix + key);
	}

	public long getLong(String key, long def) {
		return cfg.getLong(prefix + key, def);
	}

	/**
	 * Get the double value corresponding to the given key.
	 * Returns -1.0 if not set or if the value is not a double.
	 */
	public double getDouble(String key) {
		return cfg.getDouble(prefix + key);
	}

	public double getDouble(String key, double def) {
		return cfg.getDouble(prefix + key, def);
	}

	/**
	 * Get the boolean value corresponding to the given key.
	 * Returns false if not set.
	 */
	public boolean getBoolean(String key) {
		return cfg.getBoolean(prefix + key);
	}

	public boolean getBoolean(String key, boolean def) {
		return cfg.getBoolean(prefix + key, def);
	}

	public Object getObject(String key) {
		return cfg.getObject(prefix + key);
	}

	/**
	 * Get the string list value corresponding to the given key.
	 * Returns null if not set.
	 */
	public String[] getStringList(String key) {
		return cfg.getStringList(prefix + key);
	}

	public IStageManager getManager() {
		return mgr;
	}

	public IStage getStage() {
		try {
			return mgr.getStage(stagename);
		} catch (NoSuchStageException e) {
			throw new IllegalStateException("cannot find original stage [" + stagename + "]");
		}
	}

}
