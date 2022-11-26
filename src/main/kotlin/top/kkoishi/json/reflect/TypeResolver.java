package top.kkoishi.json.reflect;

import java.lang.reflect.ParameterizedType;

public interface TypeResolver<T> {
    default java.lang.reflect.Type resolve() {
        return ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
    }
}
