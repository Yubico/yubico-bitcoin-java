/*
 * Copyright (C) 2013 Yubico AB.
 * All rights reserved.
 * Proprietary code owned by Yubico AB.
 * No rights to modifications or redistribution.
 */

package com.yubico.bitcoin.api;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 11:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class OperationNotPermittedException extends IOException {
    public OperationNotPermittedException() {
        super("The performed operation is not allowed");
    }
}
