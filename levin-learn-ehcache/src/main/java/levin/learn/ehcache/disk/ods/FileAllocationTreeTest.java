package levin.learn.ehcache.disk.ods;

import java.util.List;
import java.util.Random;

import net.sf.ehcache.store.disk.ods.FileAllocationTree;
import net.sf.ehcache.store.disk.ods.Region;

import com.google.common.collect.Lists;

public class FileAllocationTreeTest {
	public static void main(String[] args) {
		final int count = 5;
		Random random = new Random();
		
		FileAllocationTree alloc = new FileAllocationTree(Long.MAX_VALUE, null);
		List<Region> allocated = Lists.newArrayList();
		for(int i = 0; i < count; i++) {
			int size = random.nextInt(1000);
			Region region = alloc.alloc(size);
			System.out.println("new size: " + size + ", " + toString(region) + ", filesize: " + alloc.getFileSize() + ", allocator: " + toString(alloc));
			allocated.add(region);
		}
		
		for(int i = 0; i < count; i++) {
			int size = random.nextInt(1000);
			Region region = alloc.alloc(size);
			System.out.println("new size: " + size + ", " + toString(region) + ", filesize: " + alloc.getFileSize() + ", allocator: " + toString(alloc));
			allocated.add(region);
			region = allocated.get(random.nextInt(allocated.size()));
			alloc.free(region);
			allocated.remove(region);
			System.out.println("Freed region: " + toString(region) + ", after file size: " + alloc.getFileSize() + ", allocator: " + toString(alloc));
		}
	}
	
	private static String toString(FileAllocationTree alloc) {
		StringBuilder builder = new StringBuilder("[");
		for(Region region : alloc) {
			builder.append(toString(region)).append(", ");
		}
		builder.replace(builder.length() - 2, builder.length() - 1, "]");
		return builder.toString();
	}
	
	private static String toString(Region region) {
		return "Regin(" + region.start() + ", " + region.end() + ")";
	}
}
