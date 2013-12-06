package levin.learn.ehcache.disk;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import levin.learn.commons.model.Book;
import levin.learn.commons.utils.StaticBookFactory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;

public class OverflowToDisk {
	public static void main(String[] args) throws Exception {
		Configuration config = new Configuration()
				.diskStore(new DiskStoreConfiguration().path("overflow"))
				.name("overflow-cache-config");
		CacheManager cacheManager = CacheManager.create(config);
		
		PersistenceConfiguration persistConfig = new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP);
		CacheConfiguration cacheConfig = new CacheConfiguration()//.maxEntriesLocalHeap(2)
																 .persistence(persistConfig)
																 .name("book-cache")
																 .maxBytesLocalHeap(1000, MemoryUnit.BYTES);
		cacheManager.addCache(new Cache(cacheConfig));
		
		Cache bookCache = cacheManager.getCache("book-cache");
		Book bookToChange = addBooks(bookCache);
		bookCache.flush();
		
		bookToChange.setIsbn("DUMMY");
		
		Thread.sleep(TimeUnit.SECONDS.toMillis(30));
		
		Book fetch = (Book)bookCache.get("UMD Distilled").getObjectValue();
		System.out.println(fetch);
		
		System.exit(0);
	}
	
	private static Book addBooks(Cache cache) {
		StaticBookFactory bookFactory = new StaticBookFactory();
		List<Book> books = Lists.newArrayList();
		bookFactory.init(books);
		
		Book bookToRet = null;
		for(Book book : books) {
			cache.put(new Element(book.getName(), book));
			
			if("UMD Distilled".equalsIgnoreCase(book.getName())) {
				bookToRet = book;
			}
		}
		
		return bookToRet;
	}
}
