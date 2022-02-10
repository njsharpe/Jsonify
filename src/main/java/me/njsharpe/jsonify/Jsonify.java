package me.njsharpe.jsonify;

import me.njsharpe.jsonify.json.*;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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

    private static <T> IJson objectToJson(T object) {
        Class<?> clazz = object.getClass();

        try {
            // String: char[], Integer: int, etc...
            if(clazz.isPrimitive() || isPrimitive(clazz) || clazz.isEnum())
                return new JsonPoint<>(object);
            if(isList(clazz)) return listToJsonList((List<?>) object);
            if(isSet(clazz)) return setToJsonSet((Set<?>) object);
            if(isMap(clazz)) return mapToJsonObject((Map<String, ?>) object);
            if(clazz.isArray()) return arrayToJsonList((Object[]) object);
            JsonObject json = new JsonObject();
            for(Field field : clazz.getDeclaredFields()) {
                if(field.isAnnotationPresent(JsonIgnore.class)) continue;
                field.setAccessible(true);
                json.append(new JsonPair(field.getName(), objectToJson(field.get(object))));
            }
            return json;
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private static JsonList listToJsonList(List<?> list) {
        JsonList json = new JsonList();
        list.forEach(obj -> {
            json.add(objectToJson(obj));
        });
        return json;
    }

    private static JsonSet setToJsonSet(Set<?> set) {
        JsonSet json = new JsonSet();
        set.forEach(obj -> {
            json.add(objectToJson(obj));
        });
        return json;
    }

    private static JsonObject mapToJsonObject(Map<String, ?> map) {
        JsonObject json = new JsonObject();
        map.forEach((k, v) -> {
            json.append(new JsonPair(k, objectToJson(v)));
        });
        return json;
    }

    private static JsonList arrayToJsonList(Object[] array) {
        JsonList json = new JsonList();
        for(Object obj : array) {
            json.add(objectToJson(obj));
        }
        return json;
    }

    public static JsonObject toJson(Map<String, ?> map) {
        JsonObject json = new JsonObject();
        map.forEach((k, v) -> {
            json.append(new JsonPair(k, objectToJson(v)));
        });
        return json;
    }

    public static JsonObject toJson(Object object) {
        JsonObject json = new JsonObject();
        if(object == null) return null;
        Class<?> clazz = object.getClass();

        try {
            for(Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(JsonIgnore.class)) continue;
                field.setAccessible(true);
                json.append(new JsonPair(field.getName(), objectToJson(field.get(object))));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        return json;
    }

    public static JsonList toJson(Object[] array) {
        JsonList json = new JsonList();
        for(Object obj : array) {
            json.add(objectToJson(obj));
        }
        return json;
    }

    public static JsonList toJson(Collection<?> collection) {
        JsonList json = new JsonList();
        collection.forEach(obj -> {
            json.add(objectToJson(obj));
        });
        return json;
    }

    private static <T> T jsonToObject(Class<T> clazz, IJson json) {
        if((clazz.isPrimitive() || isPrimitive(clazz) || clazz.isEnum()) && json instanceof JsonPoint point)
            return (T) point.getValue();
        if(isList(clazz) && json instanceof JsonList list) return clazz.cast(jsonListToList(clazz, list));
        if(isSet(clazz) && json instanceof JsonSet set) return clazz.cast(jsonSetToSet(clazz, set));
        if(isMap(clazz) && json instanceof JsonObject map) return clazz.cast(jsonObjectToMap(clazz, map));
        if(clazz.isArray() && json instanceof JsonList array) return clazz.cast(jsonListToArray(clazz, array));
        try {
            JsonObject object = (JsonObject) json;
            ReflectionFactory factory = ReflectionFactory.getReflectionFactory();
            Constructor<?> constructor = factory.newConstructorForSerialization(clazz,
                    Object.class.getDeclaredConstructor());
            T t = clazz.cast(constructor.newInstance());
            for(JsonPair obj : object.getObjects()) {
                Field field = clazz.getDeclaredField(obj.getKey());
                field.setAccessible(true);
                field.set(t, jsonToObject(field.getType(), obj.getValue()));
            }
            return t;
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static List<Object> jsonListToList(Class<?> clazz, JsonList json) {
        List<Object> list = new ArrayList<>();
        json.getList().forEach(obj -> {
            list.add(jsonToObject(clazz, obj));
        });
        return list;
    }

    private static Set<Object> jsonSetToSet(Class<?> clazz, JsonSet json) {
        Set<Object> set = new HashSet<>();
        json.getSet().forEach(obj -> {
            set.add(jsonToObject(clazz, obj));
        });
        return set;
    }

    private static Map<String, Object> jsonObjectToMap(Class<?> clazz, JsonObject json) {
        Map<String, Object> map = new HashMap<>();
        json.getObjects().forEach(obj -> {
            map.put(obj.getKey(), jsonToObject(clazz, obj.getValue()));
        });
        return map;
    }

    private static Object[] jsonListToArray(Class<?> clazz, JsonList json) {
        List<IJson> list = json.getList();
        Object[] array = new Object[list.size()];
        for(int i = 0; i < list.size(); i++) {
            array[i] = jsonToObject(clazz, list.get(i));
        }
        return array;
    }

    public static <T> Map<String, T> asMap(Class<T> clazz, JsonObject json) {
        Map<String, T> map = new HashMap<>();
        json.getObjects().forEach(obj -> {
            map.put(obj.getKey(), jsonToObject(clazz, obj.getValue()));
        });
        return map;
    }

    public static <T> T fromJson(Class<T> clazz, JsonObject json) {
        try {
            ReflectionFactory factory = ReflectionFactory.getReflectionFactory();
            Constructor<?> constructor = factory.newConstructorForSerialization(clazz,
                    Object.class.getDeclaredConstructor());
            T t = clazz.cast(constructor.newInstance());
            for(JsonPair obj : json.getObjects()) {
                Field field = clazz.getDeclaredField(obj.getKey());
                field.setAccessible(true);
                field.set(t, jsonToObject(field.getType(), obj.getValue()));
            }
            return t;
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static <T> Collection<T> asCollection(Class<T> clazz, JsonList json) {
        Collection<T> collection = new ArrayList<>();
        json.getList().forEach(obj -> {
            collection.add(jsonToObject(clazz, obj));
        });
        return collection;
    }

    public static <T> T[] asArray(Class<T> clazz, JsonList json) {
        List<IJson> list = json.getList();
        Object array = Array.newInstance(clazz, list.size());
        for(int i = 0; i < list.size(); i++) {
            Array.set(array, i, jsonToObject(clazz, list.get(i)));
        }
        return (T[]) array;
    }

}
