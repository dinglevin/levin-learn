package levin.learn.aspectj.helloworld;

public class HelloWorld {
	public static void main(String[] args) {
		new HelloWorld().greeting("Levin");
	}
	
	public void greeting(String name) {
		System.out.println("Hello " + name + ", welcome to the AspectJ world!~");
	}
}
