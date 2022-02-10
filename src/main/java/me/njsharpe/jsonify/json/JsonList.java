package me.njsharpe.jsonify.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonList implements IJson {

    private final List<IJson> list;

    public JsonList() {
        this.list = new ArrayList<>();
    }

    public void add(IJson json) {
        this.list.add(json);
    }

    public List<IJson> getList() {
        return this.list;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        Iterator<IJson> it = this.list.iterator();
        while(it.hasNext()) {
            builder.append(it.next().toString());
            if(it.hasNext()) builder.append(", ");
        }
        builder.append("]");
        return builder.toString();
    }

}
