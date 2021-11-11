package me.njsharpe.jsonify;

import me.njsharpe.jsonify.exception.JsonDeserializeException;
import me.njsharpe.jsonify.exception.JsonSerializeException;
import me.njsharpe.jsonify.json.*;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.*;
import java.util.*;

public class Jsonify {

    private static final List<Class<?>> LIST_TYPES = Arrays.asList(
            List.class, ArrayList.class, Collection.class
    );

    private static final List<Class<?>> SET_TYPES = Arrays.asList(
            Set.class, HashSet.class
    );

    private static final List<Class<?>> MAP_TYPES = Arrays.asList(
            Map.class, HashMap.class
    );

    private static final List<Class<?>> PRIMITIVES = Arrays.asList(
            Boolean.class, Byte.class, Character.class, Double.class, Float.class,
            Integer.class, Long.class, Short.class, Void.class, String.class
    );

    private static boolean isList(Class<?> clazz) {
        return LIST_TYPES.contains(clazz);
    }

    private static boolean isSet(Class<?> clazz) { return SET_TYPES.contains(clazz); }

    private static boolean isMap(Class<?> clazz) {
        return MAP_TYPES.contains(clazz);
    }

    private static boolean isPrimitive(Class<?> clazz) {
        return PRIMITIVES.contains(clazz);
    }

    public static boolean isAccepted(Class<?> clazz) {
        return isList(clazz) || isSet(clazz) || isMap(clazz) || isPrimitive(clazz);
    }

    @SuppressWarnings("unchecked")
    public static IJsonObject toJson(Object data) {
        JsonObject root = new JsonObject();
        if(data == null) return null;
        try {
            Class<?> clazz = data.getClass();
            Field[] fields = clazz.getDeclaredFields();
            for(Field field : fields) {
                Class<?> ft = field.getType();
                if(field.isAnnotationPresent(JsonIgnore.class)) continue;
                field.setAccessible(true);
                Object value = field.get(data) == null ? null : field.get(data);
                if(value == null) {
                    root.append(field.getName(), null);
                    continue;
                }
                if(ft.isPrimitive() || isAccepted(ft) || ft.isEnum()) {
                    if(isList(ft)) {
                        JsonList list = new JsonList();
                        ((List<?>) value).forEach(i -> {
                            list.append(i != null ? toJson(i) : null);
                        });
                        root.append(field.getName(), list);
                    } else if(isSet(ft)) {
                        JsonList list = new JsonList();
                        ((Set<?>) value).forEach(i -> {
                            list.append(i != null ? toJson(i) : null);
                        });
                        root.append(field.getName(), list);
                    } else if(isMap(ft)) {
                        JsonObject obj = new JsonObject();
                        ParameterizedType type = (ParameterizedType) field.getGenericType();
                        Class<?> key = Class.forName(type.getActualTypeArguments()[0].getTypeName());
                        if(!key.equals(String.class))
                            throw new JsonSerializeException("Cannot serialize map with non-string key type!");
                        ((Map<String, ?>) value).forEach((k, v) -> {
                            obj.append(k, v != null ? toJson(v) : null);
                        });
                        root.append(field.getName(), obj);
                    } else {
                        root.append(field.getName(), value);
                    }
                } else if(ft.isArray()) {
                    JsonList list = new JsonList();
                    for(Object obj : ((Object[]) value)) {
                        list.append(obj != null ? toJson(obj) : null);
                    }
                    root.append(field.getName(), list);
                } else {
                    root.append(field.getName(), toJson(value));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return root;
    }

    public static <T> T fromJson(Class<T> from, IJsonObject json) {
        if(json == null) return null;
        try {
            if(json instanceof JsonObject obj) {
                ReflectionFactory factory = ReflectionFactory.getReflectionFactory();
                Constructor<?> constructor = factory.newConstructorForSerialization(from,
                        Object.class.getDeclaredConstructor());
                T t = from.cast(constructor.newInstance());

                Map<String, JsonPoint<?>> data = obj.getData();
                for(Map.Entry<String, JsonPoint<?>> entry : data.entrySet()) {
                    Field field = from.getDeclaredField(entry.getKey());
                    if(field.isAnnotationPresent(JsonIgnore.class)) continue;
                    field.setAccessible(true);
                    Class<?> ft = field.getType();
                    Object value = entry.getValue() == null ? null : entry.getValue().getValue();
                    if(value == null) {
                        field.set(t, null);
                        continue;
                    }
                    if(ft.isPrimitive() || isAccepted(ft) || ft.isEnum()) {
                        if(isList(ft)) {
                            JsonList list = (JsonList) value;
                            List<Object> array = new ArrayList<>();
                            for(JsonPoint<?> v : list.getData()) {
                                array.add(v == null ? null : fromJson(v.getType(), (JsonObject) v.getValue()));
                            }
                            field.set(t, array);
                        } else if(isSet(ft)) {
                            JsonList list = (JsonList) value;
                            Set<Object> set = new HashSet<>();
                            for(JsonPoint<?> v : list.getData()) {
                                set.add(v == null ? null : fromJson(v.getType(), (JsonObject) v.getValue()));
                            }
                            field.set(t, set);
                        } else if(isMap(ft)) {
                            JsonObject object = (JsonObject) value;
                            Map<String, Object> map = new HashMap<>();
                            object.getData().forEach((k, v) -> {
                                map.put(k, v == null ? null : fromJson(v.getType(), (JsonObject) v.getValue()));
                            });
                            field.set(t, map);
                        } else {
                            field.set(t, value);
                        }
                    } else if(ft.isArray()) {
                        JsonList list = (JsonList) value;
                        int length = list.getData().size();
                        Object array = Array.newInstance(ft.getComponentType(), length);
                        for(int i = 0; i < length; i++) {
                            Object v = list.getData().get(i) == null ? null : list.getData().get(i).getValue();
                            Array.set(array, i, v);
                        }
                        field.set(t, array);
                    } else {
                        field.set(t, fromJson(ft, (JsonObject) value));
                    }
                }

                return t;
            }

            if(json instanceof JsonList) {
                throw new UnsupportedOperationException();
            }

            throw new JsonDeserializeException("Cannot deserialize invalid JSON definition!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

}
