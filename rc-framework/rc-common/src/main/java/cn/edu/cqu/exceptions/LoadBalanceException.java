package cn.edu.cqu.exceptions;

public class LoadBalanceException extends RuntimeException{
    public LoadBalanceException() {
    }

    public LoadBalanceException(String message) {
        super(message);
    }

    public LoadBalanceException(Throwable cause) {
        super(cause);
    }
}
