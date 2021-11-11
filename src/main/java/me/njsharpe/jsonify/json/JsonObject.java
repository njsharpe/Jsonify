package me.njsharpe.jsonify.json;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JsonObject implements IJsonObject {

    private final Map<String, JsonPoint<?>> map;

    public JsonObject() {
        map = new HashMap<>();
    }

    public <T> void append(String key, T value) {
        if(value == null) {
            this.map.put(key, null);
            return;
        }
        this.map.put(key, new JsonPoint<>(value));
    }

    public Map<String, JsonPoint<?>> getData() {
        return this.map;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("{");
        Iterator<Map.Entry<String, JsonPoint<?>>> it = this.map.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, JsonPoint<?>> value = it.next();
            builder.append(String.format("\"%s\": %s", value.getKey(),
                    value.getValue() == null ? null : value.getValue().toString()));
            if(it.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("}");
        return builder.toString();
    }
}
