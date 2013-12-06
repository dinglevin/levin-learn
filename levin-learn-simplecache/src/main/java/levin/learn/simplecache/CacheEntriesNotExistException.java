package levin.learn.simplecache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CacheEntriesNotExistException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final List<Object> noEntryKeys;
	
	@SuppressWarnings("rawtypes")
	public CacheEntriesNotExistException(Cache cache, Object... keys) {
		super("Cache " + cache.getName() + " doesn't have items for " + Arrays.toString(keys));
		this.noEntryKeys = Arrays.asList(keys);
	}
	
	@SuppressWarnings("rawtypes")
	public CacheEntriesNotExistException(Cache cache, List<? extends Object> keys) {
		super("Cache " + cache.getName() + " doesn't have items for " + keys);
		this.noEntryKeys = new ArrayList<Object>(keys);
	}
	
	public List<Object> getNoEntryKeys() {
		return Collections.unmodifiableList(noEntryKeys);
	}
}
