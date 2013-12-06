package levin.learn.javavm.localslot;

/* Run with parameter: -verbose:gc
 * 
 * GC Output:
 * [GC 66649K->65984K(106560K), 0.0012330 secs]
 * [Full GC 65984K->294K(106560K), 0.0080790 secs]
 * 
 * allocate 64MB memory, not garbage collected
 */
public class LocalSlotReuse3 {
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		{
			byte[] placeholder = new byte[64 * 1024 * 1024];
		}
		int a = 0;
		
		System.gc();
	}
}
