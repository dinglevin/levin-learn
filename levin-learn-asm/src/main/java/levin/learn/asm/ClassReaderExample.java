package levin.learn.asm;

import java.util.Arrays;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassReaderExample {
    public static void main(String[] args) throws Exception {
        //ClassReader classReader = new ClassReader("java.lang.String");
        ClassReader classReader = new ClassReader("levin.learn.asm.ClassReaderExample");
        classReader.accept(new ClassVisitor() {
            public void visit(int version, int access, String name,
                    String signature, String superName, String[] interfaces) {
                System.out.println("class name:" + name);
                System.out.println("super class name:" + superName);
                System.out.println("class version:" + version);
                System.out.println("class access:" + access);
                System.out.println("class signature:" + signature);
                if (interfaces != null && interfaces.length > 0) {
                    for (String str : interfaces) {
                        System.out.println("implemented interface name:" + str);
                    }
                }
            }

            public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
                System.out.println("Annotation: " + arg0 + ", " + arg1);
                return null;
            }

            public void visitAttribute(Attribute arg0) {
                System.out.println("Attribute: " + arg0);
            }

            public FieldVisitor visitField(int arg0, String arg1, String arg2,
                    String arg3, Object arg4) {
                System.out.println("Field: " + arg0 + ", " + arg1 + ", " + 
                    arg2 + ", " + arg3 + ", " + arg4);
                return null;
            }

            public void visitInnerClass(String arg0, String arg1, String arg2,
                    int arg3) {
                System.out.println("InnerClass: " + arg0 + ", " + arg1 + ", " + 
                    arg2 + ", " + arg3);
            }

            public MethodVisitor visitMethod(int arg0, String arg1,
                    String arg2, String arg3, String[] arg4) {
                System.out.println("Method: " + arg0 + ", " + arg1 + ", " + 
                    arg2 + ", " + arg3 + ", " + Arrays.toString(arg4));
                return null;
            }

            public void visitOuterClass(String arg0, String arg1, String arg2) {
                System.out.println("OuterClass: " + arg0 + ", " + arg1 + ", " + arg2);
            }

            public void visitSource(String arg0, String arg1) {
                System.out.println("Source: " + arg0 + ", " + arg1);
            }
            
            public void visitEnd() {
                System.out.println("END");
            }

        }, 0);
    }
}
