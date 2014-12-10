package edu.upc.eetac.dsa.mleiva.beeter.api;

/**
 * Created by Administrador on 17/11/2014.
 */
public class AppException extends Exception {
    public AppException() {
        super();
    }

    public AppException(String detailMessage) {
        super(detailMessage);
    }
}
