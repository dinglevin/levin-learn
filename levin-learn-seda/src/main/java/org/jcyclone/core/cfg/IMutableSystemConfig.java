package org.jcyclone.core.cfg;


public interface IMutableSystemConfig extends ISystemConfig {

	void putString(String key, String val);

	void putBoolean(String key, boolean val);

	void putInt(String key, int val);

	void putLong(String key, long val);

	void putDouble(String key, double val);

	void putObject(String key, Object val);

	void putStringList(String key, String valarr[]);
}
