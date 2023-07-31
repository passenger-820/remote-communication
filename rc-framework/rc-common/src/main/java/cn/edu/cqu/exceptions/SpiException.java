package cn.edu.cqu.exceptions;

public class SpiException extends RuntimeException{
    public SpiException() {
    }

    public SpiException(String message) {
        super(message);
    }

    public SpiException(Throwable cause) {
        super(cause);
    }
}
