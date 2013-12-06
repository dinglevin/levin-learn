package levin.learn.ehcache.sizeof;

import net.sf.ehcache.pool.sizeof.AgentSizeOf;
import net.sf.ehcache.pool.sizeof.ReflectionSizeOf;
import net.sf.ehcache.pool.sizeof.SizeOf;
import net.sf.ehcache.pool.sizeof.UnsafeSizeOf;

public class EhcacheSizeOfTest {
	public static void main(String[] args) {
		MyClass ins = new MyClass();
		
		System.out.println("ReflectionSizeOf: " + calculate(new ReflectionSizeOf(), ins));
		System.out.println("UnsafeSizeOf: " + calculate(new UnsafeSizeOf(), ins));
		System.out.println("AgentSizeOf: " + calculate(new AgentSizeOf(), ins));
	}
	
	private static long calculate(SizeOf sizeOf, Object instance) {
		return sizeOf.sizeOf(instance);
	}
	
	public static class MyClass {
	    byte a;
	    int c;
	    boolean d;
	    long e;
	    Object f;
	}
}
