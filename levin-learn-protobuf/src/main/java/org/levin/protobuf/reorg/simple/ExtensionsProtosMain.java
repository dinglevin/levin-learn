package org.levin.protobuf.reorg.simple;

import org.levin.protobuf.generated.simple.ExtensionsProtos;
import org.levin.protobuf.generated.simple.ExtensionsProtos.Foo;

import com.google.protobuf.ExtensionRegistry;

public class ExtensionsProtosMain {
    public static void main(String[] args) throws Exception {
        Foo foo = Foo.newBuilder().setField1(10)
                     .setExtension(ExtensionsProtos.bar, 20)
                     .build();
        System.out.println(foo);
        // field1: 10
        // [levin.protobuf.bar]: 20
        
        ExtensionRegistry registry = ExtensionRegistry.newInstance();
        ExtensionsProtos.registerAllExtensions(registry);
        
        Foo foo2 = Foo.newBuilder()
                      .mergeFrom(foo.toByteArray(), registry)
                      .build();
        System.out.println(foo2);
        // field1: 10
        // [levin.protobuf.bar]: 20
    }
}
