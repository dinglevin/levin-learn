package levin.learn.ehcache.disk;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;

public class DiskStoreFactoryTest {
    public static void main(String[] args) {
        String name = "DiskStoreFactoryTestCache";
        CacheConfiguration cacheConfig = new CacheConfiguration()
                .persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP))
                .diskAccessStripes(3);

        DiskStoreFactoryTestEhcache ehcache = new DiskStoreFactoryTestEhcache(name, cacheConfig);
    }
}
