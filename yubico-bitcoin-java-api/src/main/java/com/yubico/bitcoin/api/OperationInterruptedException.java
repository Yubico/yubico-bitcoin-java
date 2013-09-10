package com.yubico.bitcoin.api;

import java.util.concurrent.ExecutionException;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 11:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class OperationInterruptedException extends ExecutionException {
    public OperationInterruptedException(Throwable cause) {
        super(String.format("The operation was interrupted by the wrapped cause: %s", cause), cause);
    }
}
