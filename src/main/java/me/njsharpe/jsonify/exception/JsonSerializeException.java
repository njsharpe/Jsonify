package me.njsharpe.jsonify.exception;

public class JsonSerializeException extends RuntimeException {

    public JsonSerializeException(String message) {
        super(message);
    }

    public JsonSerializeException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonSerializeException(Throwable cause) {
        super(cause);
    }

}
