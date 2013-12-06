package levin.learn.simplecache;

import levin.learn.commons.model.Book;
import levin.learn.commons.utils.StaticBookFactory;

import org.junit.Test;

public class CacheTest {
	private static CacheFactory cacheFactory = new CacheFactory();
	private static StaticBookFactory bookFactory = new StaticBookFactory();
	
	@Test
	public void testCacheSimpleUsage() {
		Book uml = bookFactory.createUMLDistilled();
		Book derivatives = bookFactory.createDerivatives();
		
		String umlBookISBN = uml.getIsbn();
		String derivativesBookISBN = derivatives.getIsbn();
		
		Cache<String, Book> cache = cacheFactory.create("book-cache");
		cache.put(umlBookISBN, uml);
		cache.put(derivativesBookISBN, derivatives);
		
		Book fetchedBackUml = cache.get(umlBookISBN);
		System.out.println(fetchedBackUml);
		
		Book fetchedBackDerivatives = cache.get(derivativesBookISBN);
		System.out.println(fetchedBackDerivatives);
	}
}
