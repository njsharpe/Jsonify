package me.njsharpe.jsonify.json;

public class JsonPoint<T> implements IJson {

    private final Class<?> type;
    private final T t;

    public JsonPoint(T t) {
        this.type = t.getClass();
        this.t = t;
    }

    public Class<?> getType() {
        return this.type;
    }

    public T getValue() {
        return this.t;
    }

    public String toString() {
        boolean q = this.t instanceof String || this.t instanceof Character || this.t instanceof Enum;
        return String.format("%s", q ? String.format("\"%s\"", this.t) : this.t);
    }

}
