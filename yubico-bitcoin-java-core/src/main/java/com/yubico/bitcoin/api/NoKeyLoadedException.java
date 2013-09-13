package com.yubico.bitcoin.api;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/13/13
 * Time: 11:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class NoKeyLoadedException extends IOException {
    public NoKeyLoadedException() {
        super("No master key had been loaded on the device");
    }
}
