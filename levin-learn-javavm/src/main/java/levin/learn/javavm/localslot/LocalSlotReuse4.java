package levin.learn.javavm.localslot;

/* Run with parameter: -verbose:gc
 * 
 * GC Output:
 * [GC 66649K->65968K(106688K), 0.0103290 secs]
 * [Full GC 65968K->294K(106688K), 0.0050990 secs]
 * 
 * allocate 64MB memory, not garbage collected
 */
public class LocalSlotReuse4 {
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		byte[] placeholder = new byte[64 * 1024 * 1024];
		placeholder = null;

		System.gc();
	}
}
