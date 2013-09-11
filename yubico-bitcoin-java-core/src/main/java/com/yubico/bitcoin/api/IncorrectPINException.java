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
 * Time: 10:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class IncorrectPINException extends Exception {

    private final int triesRemaining;
    private final PinMode mode;

    public IncorrectPINException(PinMode pinMode, int triesRemaining) {
        super(String.format("Incorrect PIN for %s, %d attempts remaining", pinMode, triesRemaining));
        this.mode = pinMode;
        this.triesRemaining = triesRemaining;
    }

    public int getTriesRemaining() {
        return triesRemaining;
    }

    public PinMode getPinMode() {
        return mode;
    }
}
