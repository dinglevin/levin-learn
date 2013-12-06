package levin.learn.ehcache.disk;

import net.sf.ehcache.config.CacheConfiguration;
import levin.learn.ehcache.EhcacheAdaptor;

public class DiskStoreFactoryTestEhcache extends EhcacheAdaptor {
	private String name;
	private CacheConfiguration cacheConfiguration;
	
	public DiskStoreFactoryTestEhcache(String name, CacheConfiguration cacheConfiguration) {
		this.name = name;
		this.cacheConfiguration = cacheConfiguration;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public CacheConfiguration getCacheConfiguration() {
		return cacheConfiguration;
	}

}
