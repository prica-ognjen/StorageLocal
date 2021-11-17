package exceptions;

public class ConnectionException extends RuntimeException {

    public ConnectionException() {
    }

    public ConnectionException(String reason) {
        super(reason);
    }

}
