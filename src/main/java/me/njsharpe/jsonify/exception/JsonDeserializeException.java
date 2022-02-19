package me.njsharpe.jsonify.exception;

public class JsonDeserializeException extends RuntimeException {

    public JsonDeserializeException(String message) {
        super(message);
    }

    public JsonDeserializeException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonDeserializeException(Throwable cause) {
        super(cause);
    }

}
