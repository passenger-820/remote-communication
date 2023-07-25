package cn.edu.cqu.exceptions;

public class CompressorException extends RuntimeException{
    public CompressorException() {
    }

    public CompressorException(String message) {
        super(message);
    }

    public CompressorException(Throwable cause) {
        super(cause);
    }
}
