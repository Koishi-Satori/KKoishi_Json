import org.jetbrains.annotations.NotNull;
import top.kkoishi.json.*;
import top.kkoishi.json.annotation.DeserializationIgnored;
import top.kkoishi.json.annotation.FactoryGetter;
import top.kkoishi.json.annotation.FieldJsonName;
import top.kkoishi.json.annotation.SerializationIgnored;
import top.kkoishi.json.io.*;
import top.kkoishi.json.parse.Factorys;
import top.kkoishi.json.reflect.Type;
import top.kkoishi.json.reflect.TypeResolver;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.System.out;

public final class Test {
    public static void main (String[] args) throws Exception {
        //testRef();
        //testArrayRef();
        //testMapRef();
        //testGetter();
        test();
    }

    private static void testGetter() {
        KsonBuilder builder = new KsonBuilder();
        Kson kson = builder.prettyFormat().create();
        out.println((TestGetter) kson.fromJsonString(TestGetter.class, "{\"a\": 114, \"b\": []}"));
    }

    private static void test () throws Exception {
        // test Node.class
        final Kson test = new Kson();
        OutputStream oos = new FileOutputStream("./clz.json");
        JsonWriter writer = test.writer(new OutputStreamWriter(oos));
        final Node testNode = new Node(114, true, "ee", new Node(514, false, "aa", null));
        writer.write(test.toJson(Node.class, testNode));
        writer.flush();
        writer.close();
        oos.close();

        // test array.
        InputStream ins = new FileInputStream("./arr.json");
        JsonReader reader = test.reader(new InputStreamReader(ins));
        JsonElement ele = reader.read();
        String[] arr = test.fromJson(String[].class, ele);
        out.println(Arrays.toString(arr));
        reader.close();
        ins.close();

        // test map.
        ins = new FileInputStream("./jo.json");
        reader = test.reader(new InputStreamReader(ins));
        ele = reader.read();
        final LinkedHashMap<String, Node> map = test.fromJson(new TypeResolver<LinkedHashMap<String, Node>>() {
        }, ele);
        out.println(map);
        reader.close();
        ins.close();

        // test KsonBuilder.
        KsonBuilder builder = new KsonBuilder(test);
        final var kson = builder.prettyFormat().create();
        out.println(kson.toJson(testNode));
        out.println(kson.toJson(arr));
    }

    private static void testMapRef () throws Exception {
        final InputStream ins = new FileInputStream("./jo.json");
        final JsonReader reader = new JsonReader(new InputStreamReader(ins));
        final TypeResolver<HashMap<String, Node>> resolver = new TypeResolver<HashMap<String, Node>>() {
        };
        final MapTypeParser<String, Node> parser = Factorys.getMapTypeFactory().create(resolver);
        final JsonElement jo = reader.read();
        final Map<String, Node> map = parser.fromJson(jo);
        out.println(map.get("114514").context);
        out.println(map);
        reader.close();
        ins.close();
    }

    private static void testArrayRef () throws Exception {
        final ArrayTypeParser<String[]> parser = Factorys.getArrayTypeFactory().create(new Type<String[]>(String[].class));
        final String[] arr = {"114", "514", "1919810", "test", "fk", "Touhou Project", "Stellaris"};
        final JsonArray ja = parser.toJson(arr);
        out.println(ja);
        ja.add(new JsonString("Heart of Iron IV"));
        out.println(Arrays.toString(parser.getArray(ja)));
    }

    private static void testRef () throws Exception {
        final OutputStream oos = new FileOutputStream("./clz.json");
        final JsonWriter writer = new BasicJsonWriter(new OutputStreamWriter(oos));
        final Node testNode = new Node(114, true, "ee", new Node(514, false, "aa", null));
        final JsonElement ele = Factorys.getFieldTypeFactory().fieldParser().create(new Type<Node>(Node.class)).toJson(testNode);
        out.println(ele);
        writer.write(ele);
        writer.flush();
        writer.close();
        oos.close();
        final InputStream ins = new FileInputStream("./clz.json");
        final JsonReader reader = new JsonReader(new InputStreamReader(ins));
        final Node elem = Factorys.getFactoryFromClass(Node.class).create(new Type<>(Node.class)).fromJson(reader.read());
        out.println(elem == testNode);
        out.println("qaq: " + elem + "\nqwq: " + testNode);
        out.println(elem.context);
        reader.close();
        ins.close();
    }

    @FactoryGetter(getterDescriptor = "getFactory()Ltop/kkoishi/json/io/TypeParserFactory;")
    static class TestGetter {
        int a;
        String[] b;

        public TestGetter (int a, String[] b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString () {
            return "TestGetter{" +
                    "a=" + a +
                    ", b=" + Arrays.toString(b) +
                    '}';
        }

        private static class TestTypeParser extends TypeParser<TestGetter> {
            public TestTypeParser (@NotNull Type<TestGetter> type) {
                super(type);
            }

            @Override
            public TestGetter fromJson (@NotNull JsonElement json) {
                assert json.isJsonObject();
                final var obj = json.toJsonObject();
                throw new IllegalArgumentException();
            }

            @NotNull
            @Override
            public JsonElement toJson (TestGetter testGetter) {
                throw new IllegalArgumentException();
            }
        }

        private static TypeParserFactory getFactory () {
            return new TypeParserFactory() {
                @NotNull
                @Override
                public FieldTypeParserFactory fieldParser () {
                    throw new IllegalArgumentException();
                }

                @NotNull
                @Override
                @SuppressWarnings("unchecked")
                public <T> TypeParser<T> create (@NotNull Type<T> type) {
                    if (type.rawType() == TestGetter.class)
                        return (TypeParser<T>) new TestTypeParser((Type<TestGetter>) type);
                    throw new IllegalArgumentException();
                }
            };
        }

        @org.junit.jupiter.api.Test
        void annotationTest () {

        }
    }

    static class Node {
        static final int A = 0;
        @FieldJsonName(name = "data")
        int a;

        @SerializationIgnored
        @DeserializationIgnored
        boolean flag;

        String context;
        Node next;

        public Node (int a, boolean flag, String context, Node next) {
            this.a = a;
            this.flag = flag;
            this.context = context;
            this.next = next;
        }

        @Override
        public String toString () {
            return "Node{" +
                    "a=" + a +
                    ", flag=" + flag +
                    ", context='" + context + '\'' +
                    ", next=" + next +
                    '}';
        }
    }
}
