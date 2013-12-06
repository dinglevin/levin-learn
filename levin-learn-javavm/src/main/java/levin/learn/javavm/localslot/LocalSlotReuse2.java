package levin.learn.javavm.localslot;

/* Run with parameter: -verbose:gc
 * 
 * GC Output:
 * [GC 66647K->66016K(106432K), 0.0012230 secs]
 * [Full GC 66016K->65830K(106432K), 0.0050630 secs]
 * 
 * allocate 64MB memory, not garbage collected
 */
public class LocalSlotReuse2 {
	public static void main(String[] args) {
		{
			@SuppressWarnings("unused")
			byte[] placeholder = new byte[64 * 1024 * 1024];
		}
		
		System.gc();
	}
}
