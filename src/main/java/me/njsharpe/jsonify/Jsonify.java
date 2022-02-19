package me.njsharpe.jsonify;

import me.njsharpe.jsonify.exception.JsonDeserializeException;
import me.njsharpe.jsonify.exception.JsonSerializeException;
import me.njsharpe.jsonify.json.*;
import sun.reflect.ReflectionFactory;

import java.io.Serializable;
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

    private static class Impl<T extends Serializable> {

        private JsonObject search(Class<T> clazz, Object object) {
            JsonObject json = new JsonObject();
            try {
                for(Field field : clazz.getDeclaredFields()) {
                    Class<?> type = field.getType();
                    if(!Serializable.class.isAssignableFrom(type) && !type.isPrimitive() && !isPrimitive(type)
                            && !type.isEnum()) continue;
                    try { field.setAccessible(true); } catch (Exception ignore) { continue; }
                    if(field.isAnnotationPresent(JsonIgnore.class)) continue;
                    if(field.get(object) != null)
                        json.append(new JsonPair(field.getName(), this.objectToJson((T) field.get(object))));
                }
            } catch (IllegalAccessException ex) {
                // Fall through
            } catch (Exception ex) {
                throw new JsonSerializeException("Could not serialize type: %s".formatted(clazz.getName()), ex);
            }
            return json;
        }

        private IJson objectToJson(T object) {
            Class<T> clazz = (Class<T>) object.getClass();
            // String: char[], Integer: int, etc...
            if(clazz.isPrimitive() || isPrimitive(clazz) || clazz.isEnum())
                return new JsonPoint<>(object);
            if(isList(clazz)) return this.listToJsonList((List<T>) object);
            if(isSet(clazz)) return this.setToJsonSet((Set<T>) object);
            if(isMap(clazz)) return this.mapToJsonObject((Map<String, T>) object);
            if(clazz.isArray()) return this.arrayToJsonList((T[]) object);
            return this.search(clazz, object);
        }

        private JsonList listToJsonList(List<T> list) {
            JsonList json = new JsonList();
            list.forEach(obj -> {
                json.add(this.objectToJson(obj));
            });
            return json;
        }

        private JsonSet setToJsonSet(Set<T> set) {
            JsonSet json = new JsonSet();
            set.forEach(obj -> {
                json.add(this.objectToJson(obj));
            });
            return json;
        }

        private JsonObject mapToJsonObject(Map<String, T> map) {
            JsonObject json = new JsonObject();
            map.forEach((k, v) -> {
                json.append(new JsonPair(k, this.objectToJson(v)));
            });
            return json;
        }

        private JsonList arrayToJsonList(T[] array) {
            JsonList json = new JsonList();
            for(T obj : array) {
                json.add(this.objectToJson(obj));
            }
            return json;
        }

        private T jsonToObject(Class<T> clazz, IJson json) {
            if((clazz.isPrimitive() || isPrimitive(clazz) || clazz.isEnum()) && json instanceof JsonPoint)
                return (T) ((JsonPoint) json).getValue();
            if(isList(clazz) && json instanceof JsonList) return clazz.cast(this.jsonListToList(clazz, (JsonList) json));
            if(isSet(clazz) && json instanceof JsonSet) return clazz.cast(this.jsonSetToSet(clazz, (JsonSet) json));
            if(isMap(clazz) && json instanceof JsonObject) return clazz.cast(this.jsonObjectToMap(clazz, (JsonObject) json));
            if(clazz.isArray() && json instanceof JsonList) return clazz.cast(this.jsonListToArray(clazz, (JsonList) json));
            try {
                JsonObject object = (JsonObject) json;
                ReflectionFactory factory = ReflectionFactory.getReflectionFactory();
                Constructor<?> constructor = factory.newConstructorForSerialization(clazz,
                        Object.class.getDeclaredConstructor());
                T t = clazz.cast(constructor.newInstance());
                for(JsonPair obj : object.getObjects()) {
                    Field field = clazz.getDeclaredField(obj.getKey());
                    field.setAccessible(true);
                    field.set(t, this.jsonToObject((Class<T>) field.getType(), obj.getValue()));
                }
                return t;
            } catch(Exception ex) {
                throw new JsonDeserializeException("Could not deserialize type: %s".formatted(clazz.getName()), ex);
            }
        }

        private List<T> jsonListToList(Class<T> clazz, JsonList json) {
            List<T> list = new ArrayList<>();
            json.getList().forEach(obj -> {
                list.add(this.jsonToObject(clazz, obj));
            });
            return list;
        }

        private Set<T> jsonSetToSet(Class<T> clazz, JsonSet json) {
            Set<T> set = new HashSet<>();
            json.getSet().forEach(obj -> {
                set.add(this.jsonToObject(clazz, obj));
            });
            return set;
        }

        private Map<String, T> jsonObjectToMap(Class<T> clazz, JsonObject json) {
            Map<String, T> map = new HashMap<>();
            json.getObjects().forEach(obj -> {
                map.put(obj.getKey(), this.jsonToObject(clazz, obj.getValue()));
            });
            return map;
        }

        private Object[] jsonListToArray(Class<T> clazz, JsonList json) {
            List<IJson> list = json.getList();
            Object[] array = new Object[list.size()];
            for(int i = 0; i < list.size(); i++) {
                array[i] = this.jsonToObject(clazz, list.get(i));
            }
            return array;
        }

    }

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

    public static <T extends Serializable> JsonObject toJson(Map<String, T> map) {
        Impl<T> impl = new Impl<>();
        JsonObject json = new JsonObject();
        map.forEach((k, v) -> {
            json.append(new JsonPair(k, impl.objectToJson(v)));
        });
        return json;
    }

    public static <T extends Serializable> JsonObject toJson(T object) {
        Impl<T> impl = new Impl<>();
        if(object == null) return null;
        Class<T> clazz = (Class<T>) object.getClass();

        JsonObject json;

        try {
            json = impl.search(clazz, object);
        } catch(Exception ex) {
            throw new JsonSerializeException(ex);
        }

        return json;
    }

    public static <T extends Serializable> JsonList toJson(T[] array) {
        Impl<T> impl = new Impl<>();
        JsonList json = new JsonList();
        for(T obj : array) {
            if(obj != null) json.add(impl.objectToJson(obj));
        }
        return json;
    }

    public static <T extends Serializable> JsonList toJson(Collection<T> collection) {
        Impl<T> impl = new Impl<>();
        JsonList json = new JsonList();
        collection.forEach(obj -> {
            if(obj != null) json.add(impl.objectToJson(obj));
        });
        return json;
    }

    public static <T extends Serializable> Map<String, T> asMap(Class<T> clazz, JsonObject json) {
        Impl<T> impl = new Impl<>();
        Map<String, T> map = new HashMap<>();
        json.getObjects().forEach(obj -> {
            map.put(obj.getKey(), impl.jsonToObject(clazz, obj.getValue()));
        });
        return map;
    }

    public static <T extends Serializable> T fromJson(Class<T> clazz, JsonObject json) {
        Impl<T> impl = new Impl<>();
        try {
            ReflectionFactory factory = ReflectionFactory.getReflectionFactory();
            Constructor<?> constructor = factory.newConstructorForSerialization(clazz,
                    Object.class.getDeclaredConstructor());
            T t = clazz.cast(constructor.newInstance());
            for(JsonPair obj : json.getObjects()) {
                Field field = clazz.getDeclaredField(obj.getKey());
                field.setAccessible(true);
                field.set(t, impl.jsonToObject((Class<T>) field.getType(), obj.getValue()));
            }
            return t;
        } catch(Exception ex) {
            throw new JsonDeserializeException(ex);
        }
    }

    public static <T extends Serializable> Collection<T> asCollection(Class<T> clazz, JsonList json) {
        Impl<T> impl = new Impl<>();
        Collection<T> collection = new ArrayList<>();
        json.getList().forEach(obj -> {
            collection.add(impl.jsonToObject(clazz, obj));
        });
        return collection;
    }

    public static <T extends Serializable> T[] asArray(Class<T> clazz, JsonList json) {
        Impl<T> impl = new Impl<>();
        List<IJson> list = json.getList();
        Object array = Array.newInstance(clazz, list.size());
        for(int i = 0; i < list.size(); i++) {
            Array.set(array, i, impl.jsonToObject(clazz, list.get(i)));
        }
        return (T[]) array;
    }

}
