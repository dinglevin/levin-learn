package levin.learn.aspectj.helloworld;

public aspect GreetingAspect {
	pointcut greetingPointcut():
		call(void HelloWorld.greeting(..));
		
	after(): greetingPointcut() {
		System.out.println("After Greeting...");
	}
	
	before(): greetingPointcut() {
		System.out.println("Before Greeting...");
	}
	
	void around(): greetingPointcut() {
		System.out.println("around before...");
		proceed();
		System.out.println("around after...");
	}
}
