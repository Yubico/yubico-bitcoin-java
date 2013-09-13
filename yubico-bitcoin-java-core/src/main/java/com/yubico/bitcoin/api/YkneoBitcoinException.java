package com.yubico.bitcoin.api;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/13/13
 * Time: 11:54 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class YkneoBitcoinException extends Exception {
    public YkneoBitcoinException(String message) {
        super(message);
    }

    public YkneoBitcoinException(String message, Throwable cause) {
        super(message, cause);
    }
}
