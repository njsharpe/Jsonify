package me.njsharpe.jsonify.json;

public class JsonPair implements IJson {

    private final String key;
    private final IJson value;

    public JsonPair(String key, IJson value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return this.key;
    }

    public IJson getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return String.format("\"%s\": %s", this.key, this.value.toString());
    }
}
