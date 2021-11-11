package me.njsharpe.jsonify.json;

import me.njsharpe.jsonify.exception.JsonSerializeException;
import me.njsharpe.jsonify.Jsonify;

public class JsonPoint<T> implements IJsonObject {

    private final T value;
    private final Class<T> clazz;

    public JsonPoint(T value) {
        this.clazz = (Class<T>) value.getClass();
        if(!(this.clazz.isPrimitive() || Jsonify.isAccepted(this.clazz) ||
                IJsonObject.class.isAssignableFrom(this.clazz) || this.clazz.isEnum()))
            throw new JsonSerializeException("Cannot serialize invalid class type: " + this.clazz.getSimpleName());
        this.value = value;
    }

    public Class<T> getType() {
        return this.clazz;
    }

    public T getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        boolean quotes = this.clazz.equals(String.class) || this.clazz.equals(Character.class) || this.clazz.isEnum();
        return String.format("%s", quotes ? String.format("\"%s\"", this.value) : this.value);
    }
}
