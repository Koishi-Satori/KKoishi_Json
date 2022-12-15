package top.kkoishi.json.reflect;

import java.lang.reflect.ParameterizedType;

/**
 * An interface used to resolve the generic parameters' actual types.
 *
 * @author KKoishi_
 */
@SuppressWarnings("unused")
public interface TypeResolver<T> {
    default java.lang.reflect.Type resolve() {
        return ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
    }
}
