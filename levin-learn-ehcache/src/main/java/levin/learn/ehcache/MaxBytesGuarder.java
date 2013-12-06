package levin.learn.ehcache;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;

public interface MaxBytesGuarder {
	public int getCurrentSize();
	public void setMaxSize(int maxSize);
	public int getMaxSize();
	
	public int addWithEvict(Element element);
	public boolean canAddWithoutEvict(Element element);
	public void delete(int size);
	public ElementEvictor getEvictor();
	public Store getStore();
}

interface ElementEvictor {
	
}
