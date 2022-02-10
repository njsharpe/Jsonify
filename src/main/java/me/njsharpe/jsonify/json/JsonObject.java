package me.njsharpe.jsonify.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class JsonObject implements IJson {

    private final Collection<JsonPair> objects;

    public JsonObject() {
        this.objects = new ArrayList<>();
    }

    public void append(JsonPair json) {
        this.objects.add(json);
    }

    public Collection<JsonPair> getObjects() {
        return this.objects;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        Iterator<JsonPair> it = this.objects.iterator();
        while(it.hasNext()) {
            builder.append(it.next().toString());
            if(it.hasNext()) builder.append(", ");
        }
        builder.append("}");
        return builder.toString();
    }

}
