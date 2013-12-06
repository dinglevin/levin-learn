package levin.learn.simplecache;

public class CacheFactory {
	public <K, V> Cache<K, V> create(String cacheName) {
		return new CacheImpl<K, V>(cacheName);
	}
}
