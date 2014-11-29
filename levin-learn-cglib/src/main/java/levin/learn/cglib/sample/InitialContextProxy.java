package levin.learn.cglib.sample;

import java.lang.reflect.Method;
import java.util.Arrays;

import javax.naming.InitialContext;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.NoOp;

import org.apache.log4j.FileAppender;
import org.easymock.EasyMock;

public class InitialContextProxy {
    public static void main(String[] args) throws Exception {
        //testRealClass();
        testMockClass();
    }
    
    public static void testMockClass() throws Exception {
        final InitialContext mockContext = EasyMock.createMock(InitialContext.class);
        EasyMock.expect(mockContext.lookup("levinJndi")).andReturn(new FileAppender());
        
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(InitialContext.class);
        enhancer.setCallbackFilter(new CallbackFilter() {
            public int accept(Method method) {
                if(method.getName().equals("lookup") && method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0] == String.class) {
                    return 0;
                }
                return 1;
            }
        });
        enhancer.setCallbacks(new Callback[] {new MethodInterceptor() {
            public Object intercept(Object obj, Method method, Object[] args,
                    MethodProxy proxy) throws Throwable {
                System.out.println("Before method call: " + method.getName() + ", args: " + 
                    Arrays.toString(args));
                Object ret = method.invoke(mockContext, args);
                System.out.println("After method call: " + method.getName() + ", args: " + 
                    Arrays.toString(args));
                return ret;
            }
        }, NoOp.INSTANCE});
        
        InitialContext contextProxy = (InitialContext)enhancer.create();
        FileAppender appender = (FileAppender)contextProxy.lookup("levinJndi");
        System.out.println(appender);
    }
    
    public static void testRealClass() throws Exception {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(InitialContext.class);
        enhancer.setCallbackFilter(new CallbackFilter() {
            public int accept(Method method) {
                if(method.getName().equals("lookup") && method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0] == String.class) {
                    return 0;
                }
                return 1;
            }
        });
        enhancer.setCallbacks(new Callback[] {new MethodInterceptor() {
            public Object intercept(Object obj, Method method, Object[] args,
                    MethodProxy proxy) throws Throwable {
                System.out.println("Before method call: " + method.getName() + ", args: " + 
                    Arrays.toString(args));
                Object ret = proxy.invokeSuper(obj, args);
                System.out.println("After method call: " + method.getName() + ", args: " + 
                    Arrays.toString(args));
                return ret;
            }
        }, NoOp.INSTANCE});
        
        InitialContext contextProxy = (InitialContext)enhancer.create();
        Factory factory = (Factory)contextProxy;
        System.out.println(factory.getCallback(0));
        FileAppender appender = (FileAppender)contextProxy.lookup("levinJndi");
        System.out.println(appender);
    }
}
