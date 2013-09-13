/*
 * Copyright (C) 2013 Yubico AB.
 * All rights reserved.
 * Proprietary code owned by Yubico AB.
 * No rights to modifications or redistribution.
 */

package com.yubico.bitcoin.api;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 11:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class PinModeLockedException extends YkneoBitcoinException {
    private final PinMode pinMode;

    public PinModeLockedException(PinMode pinMode) {
        super(String.format("The requested action requires mode %s to be unlocked", pinMode));
        this.pinMode = pinMode;
    }

    public PinMode getPinMode() {
        return pinMode;
    }
}
