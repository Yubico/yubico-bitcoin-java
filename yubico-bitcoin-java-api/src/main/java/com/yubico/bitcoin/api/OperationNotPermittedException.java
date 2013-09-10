package com.yubico.bitcoin.api;

import java.util.concurrent.ExecutionException;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 11:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class OperationNotPermittedException extends ExecutionException {
    public OperationNotPermittedException() {
        super("The performed operation is not allowed");
    }
}
