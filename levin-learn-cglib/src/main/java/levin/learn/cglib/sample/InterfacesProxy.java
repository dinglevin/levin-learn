package levin.learn.cglib.sample;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class InterfacesProxy {
    public static interface Executor {
        public void execute();
    }
    
    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Executor.class);
        enhancer.setCallback(new MethodInterceptor() {
            public Object intercept(Object obj, Method method, Object[] args,
                    MethodProxy proxy) throws Throwable {
                System.out.println("Method call: " + method.getName());
                return null;
            }
        });
        
        Executor executor = (Executor)enhancer.create();
        executor.execute();
    }
}
