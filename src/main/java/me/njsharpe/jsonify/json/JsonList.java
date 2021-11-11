package me.njsharpe.jsonify.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonList implements IJsonObject {

    private final List<JsonPoint<?>> list;

    public JsonList() {
        list = new ArrayList<>();
    }

    public <T> void append(T value) {
        if(value == null) {
            this.list.add(null);
            return;
        }
        this.list.add(new JsonPoint<>(value));
    }

    public List<JsonPoint<?>> getData() {
        return this.list;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("[");
        Iterator<JsonPoint<?>> it = this.list.iterator();
        while(it.hasNext()) {
            JsonPoint<?> value = it.next();
            builder.append(value == null ? null : value.toString());
            if(it.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("]");
        return builder.toString();
    }

}
