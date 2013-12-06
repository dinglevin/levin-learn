package levin.learn.simplecache;

public class CacheEntryNotExistsException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private final Object noEntryKey;
	
	@SuppressWarnings("rawtypes")
	public CacheEntryNotExistsException(Cache cache, Object key) {
		super("Cache " + cache.getName() + " doesn't have item for " + key);
		this.noEntryKey = key;
	}
	
	public Object getNoEntryKey() {
		return noEntryKey;
	}

}
