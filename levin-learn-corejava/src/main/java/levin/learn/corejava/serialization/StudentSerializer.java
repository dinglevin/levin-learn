package levin.learn.corejava.serialization;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

public class StudentSerializer {
    public static void main(String[] args) throws Exception {
        Student s = new Student("Levin", 10);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(stream);
        output.writeObject(s);
        
        System.out.println(stream.toString());
    }
}
