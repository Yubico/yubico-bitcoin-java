package com.yubico.bitcoin.api;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/20/13
 * Time: 10:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class OperationNotSupportedException extends IOException {
    private final String existingVersion;
    private final String requiredVersion;

    public OperationNotSupportedException(String existingVersion, String requiredVersion) {
        super(String.format("The performed operation is not supported on this version of the ykneo-bitcoin applet. Required version: %s, existing version: %s", requiredVersion, existingVersion));

        this.existingVersion = existingVersion;
        this.requiredVersion = requiredVersion;
    }

    public String getExistingVersion() {
        return existingVersion;
    }

    public String getRequiredVersion() {
        return requiredVersion;
    }
}
