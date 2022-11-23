import java.util.Arrays;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.HashSet;

import static java.lang.System.*;

public final class TypeTest {
    interface TypeResolver<T> {
        default Type resolve () {
            return ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
        }
    }

    public static void main (String[] args) {
        final var resolver = new TypeResolver<HashSet<HashMap<HashSet<Integer>, Integer>>>() {};
        final var resolve = ((ParameterizedType) resolver.resolve()).getActualTypeArguments()[0];
        out.println(((ParameterizedType) resolve).getActualTypeArguments()[0]);
        out.println(new top.kkoishi.json.reflect.Type<>(int.class));
    }
}
