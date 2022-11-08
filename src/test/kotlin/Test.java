import top.kkoishi.json.annotation.FieldJsonName;
import top.kkoishi.json.annotation.SerializationIgnored;
import top.kkoishi.json.io.BasicJsonWriter;
import top.kkoishi.json.io.JsonReader;
import top.kkoishi.json.io.JsonWriter;
import top.kkoishi.json.parse.Factorys;
import top.kkoishi.json.reflect.Type;

import java.io.*;

public final class Test {
    public static void main (String[] args) throws Exception {
        testRef();
    }

    private static void testRef () throws Exception {
        final OutputStream oos = new FileOutputStream("./clz.json");
        final JsonWriter writer = new BasicJsonWriter(new OutputStreamWriter(oos));
        final Node testNode = new Node(114, true, "ee", new Node(514, false, "aa", null));
        writer.write(Factorys.getFieldTypeFactory().fieldParser().create(new Type<Node>(Node.class)).toJson(testNode));
        writer.close();
        oos.close();
    }

    static class Node {
        static final int A = 0;
        @FieldJsonName(name = "data")
        int a;

        @SerializationIgnored
        boolean flag = true;

        String context;
        Node next;

        public Node (int a, boolean flag, String context, Node next) {
            this.a = a;
            this.flag = flag;
            this.context = context;
            this.next = next;
        }
    }
}
