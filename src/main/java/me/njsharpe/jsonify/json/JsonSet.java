package me.njsharpe.jsonify.json;

import java.util.*;

public class JsonSet implements IJson {

    private final Set<IJson> set;

    public JsonSet() {
        this.set = new HashSet<>();
    }

    public void add(IJson json) {
        this.set.add(json);
    }

    public Set<IJson> getSet() {
        return this.set;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        Iterator<IJson> it = this.set.iterator();
        while(it.hasNext()) {
            builder.append(it.next().toString());
            if(it.hasNext()) builder.append(", ");
        }
        builder.append("]");
        return builder.toString();
    }

}
