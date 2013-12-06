package levin.learn.javavm.localslot;

/* Run with parameter: -verbose:gc
 * 
 * GC Output:
 * [GC 66649K->66016K(106560K), 0.0015150 secs]
 * [Full GC 66016K->65830K(106560K), 0.0054200 secs]
 * 
 * allocate 64MB memory, not garbage collected
 */
public class LocalSlotReuse1 {
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		byte[] placeholder = new byte[64 * 1024 * 1024]; // 64MB
		System.gc();
	}
}
