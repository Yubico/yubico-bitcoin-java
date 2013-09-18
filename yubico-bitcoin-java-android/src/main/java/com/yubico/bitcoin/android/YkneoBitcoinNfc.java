/*
 * Copyright 2013 Yubico AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yubico.bitcoin.android;

import android.nfc.tech.IsoDep;
import com.yubico.bitcoin.api.*;
import com.yubico.bitcoin.util.AbstractYkneoBitcoin;

import java.io.IOException;

/**
 * YkneoBitcoin implementation for Android using NFC for communication with a YubiKey NEO.
 */
public class YkneoBitcoinNfc extends AbstractYkneoBitcoin {
    private final IsoDep nfc;

    public YkneoBitcoinNfc(IsoDep nfc) throws IOException {
        this.nfc = nfc;

        nfc.connect();
        select();
    }

    @Override
    protected byte[] send(int cla, int ins, int p1, int p2, byte[] data) throws IOException {
        byte[] apdu = new byte[data.length + 5];
        apdu[0] = (byte) cla;
        apdu[1] = (byte) ins;
        apdu[2] = (byte) p1;
        apdu[3] = (byte) p2;
        apdu[4] = (byte) data.length;
        System.arraycopy(data, 0, apdu, 5, data.length);
        return nfc.transceive(apdu);
    }
}
