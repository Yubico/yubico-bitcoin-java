/*
 * Copyright (C) 2013 Yubico AB.
 * All rights reserved.
 * Proprietary code owned by Yubico AB.
 * No rights to modifications or redistribution.
 */

package com.yubico.bitcoin.api;

import java.util.concurrent.ExecutionException;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 11:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class UnusableIndexException extends YkneoBitcoinException {
    private final int index;

    public UnusableIndexException(int index) {
        super(String.format("The index: %d cannot be used with this extended key pair as it results in an invalid sub key", index));
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
