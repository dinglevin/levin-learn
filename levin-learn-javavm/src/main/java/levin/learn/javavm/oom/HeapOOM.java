package levin.learn.javavm.oom;

public class HeapOOM {
	static class OOMObject {
		private int data;
		
		public OOMObject(int data) {
			this.data = data;
		}
		
		public int getData() {
			return data;
		}
	}
}
