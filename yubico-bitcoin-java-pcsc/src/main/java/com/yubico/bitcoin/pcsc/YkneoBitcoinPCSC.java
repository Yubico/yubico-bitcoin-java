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

package com.yubico.bitcoin.pcsc;

import com.yubico.bitcoin.api.*;
import com.yubico.bitcoin.util.AbstractYkneoBitcoin;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import java.io.IOException;

/**
 * YkneoBitcoin implementation that uses javax.smartcardio to talk to a YubiKey NEO over PCSC.
 */
public class YkneoBitcoinPCSC extends AbstractYkneoBitcoin {
    private final CardChannel channel;

    public YkneoBitcoinPCSC(CardChannel channel) throws CardException, IOException {
        this.channel = channel;

        select();
    }

    @Override
    protected byte[] send(int cla, int ins, int p1, int p2, byte[] data) throws IOException {
        try {
            return channel.transmit(new CommandAPDU(cla, ins, p1, p2, data)).getBytes();
        } catch (CardException e) {
            throw new IOException(String.format("The operation was interrupted by the wrapped cause: %s", e), e);
        }
    }
}