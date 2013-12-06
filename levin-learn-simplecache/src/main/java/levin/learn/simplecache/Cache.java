package levin.learn.simplecache;

import java.util.Iterator;
import java.util.Map;

public interface Cache<K, V> {
	public String getName();
	public V get(K key);
	public Map<? extends K, ? extends V> getAll(Iterator<? extends K> keys);
	public boolean isPresent(K key);
	public void put(K key, V value);
	public void putAll(Map<? extends K, ? extends V> entries);
	public void invalidate(K key);
	public void invalidateAll(Iterator<? extends K> keys);
	public void invalidateAll();
	public boolean isEmpty();
	public int size();
	public void clear(); 
	public Map<? extends K, ? extends V> asMap();
}
