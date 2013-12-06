package levin.learn.ehcache.pool.sizeof;

import levin.learn.commons.model.Book;
import levin.learn.commons.utils.StaticBookFactory;
import net.sf.ehcache.pool.sizeof.AgentSizeOf;
import net.sf.ehcache.pool.sizeof.ReflectionSizeOf;
import net.sf.ehcache.pool.sizeof.UnsafeSizeOf;
import net.sf.ehcache.pool.sizeof.filter.PassThroughFilter;

import org.junit.Test;

public class AgentSizeofTest {
	@Test
	public void testBookAgentSizeOf() {
		Book book = new StaticBookFactory().createUMLDistilled();
		AgentSizeOf sizer = new AgentSizeOf(new PassThroughFilter());
		System.out.println(sizer.sizeOf(book));
	}
	
	@Test
	public void testBookUnsafeSizeOf() {
		Book book = new StaticBookFactory().createDerivatives();
		UnsafeSizeOf sizer = new UnsafeSizeOf(new PassThroughFilter());
		System.out.println(sizer.deepSizeOf(100, true, book).getCalculated());
	}
	
	@Test
	public void testStringUnsafeSizeOf() {
		String str = "unsafe";
		UnsafeSizeOf sizer = new UnsafeSizeOf(new PassThroughFilter());
		System.out.println(sizer.sizeOf(str));
	}
	
	@Test
	public void testBookReflectionSizeOf() {
		Book book = createLargeBook();
		ReflectionSizeOf sizer = new ReflectionSizeOf(new PassThroughFilter());
		System.out.println(sizer.deepSizeOf(100, true, book).getCalculated());
	}
	
	private static Book createLargeBook() {
		Book book = new Book();
		book.setIsbn(repeat("Large-Book-ISBN:", 10000));
		book.setLocation(repeat("Large-Book-Location:", 1000));
		book.setName(repeat("Large-Book-Name:", 10000));
		book.setPrice(Double.MAX_VALUE);
		return book;
	}
	
	private static String repeat(String str, int repeatTime) {
		StringBuilder builder = new StringBuilder(str);
		for(int i = 0; i < repeatTime - 1; i++) {
			builder.append(str);
		}
		return builder.toString();
	}
}
