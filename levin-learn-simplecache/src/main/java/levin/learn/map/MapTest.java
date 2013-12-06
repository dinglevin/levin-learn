package levin.learn.map;

public class MapTest {
	public static void main(String[] args) {
		int concurrencyLevel = 16;
		
		int sshift = 0;
        int ssize = 1;
        
        while (ssize < concurrencyLevel) {
            ++sshift;
            ssize <<= 1;
        }
        
        int segmentShift = 32 - sshift;
        int segmentMask = ssize - 1;
        
        System.out.println(segmentShift);
        System.out.println(segmentMask);
	}
}