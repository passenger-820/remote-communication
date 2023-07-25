package cn.edu.cqu.exceptions;

public class SerializerException extends RuntimeException{
    public SerializerException() {
    }

    public SerializerException(String message) {
        super(message);
    }

    public SerializerException(Throwable cause) {
        super(cause);
    }
}
