package com.yubico.bitcoin.android;

import android.nfc.tech.IsoDep;
import com.yubico.bitcoin.api.*;
import com.yubico.bitcoin.util.AbstractYkneoBitcoin;
import com.yubico.bitcoin.util.YkneoConstants;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 4:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class YkneoBitcoinNfc extends AbstractYkneoBitcoin {
    private static final byte[] SELECT_CMD = new byte[]{0x00, (byte) 0xa4, 0x04, 0x00};

    private final IsoDep nfc;

    public YkneoBitcoinNfc(IsoDep nfc) throws IOException, OperationInterruptedException {
        this.nfc = nfc;

        nfc.connect();
        select();
    }

    @Override
    protected byte[] send(int cla, int ins, int p1, int p2, byte[] data) throws OperationInterruptedException {
        byte[] apdu = new byte[data.length + 5];
        apdu[0] = (byte) cla;
        apdu[1] = (byte) ins;
        apdu[2] = (byte) p1;
        apdu[3] = (byte) p2;
        apdu[4] = (byte) data.length;
        System.arraycopy(data, 0, apdu, 5, data.length);
        try {
            return nfc.transceive(apdu);
        } catch (IOException e) {
            throw new OperationInterruptedException(e);
        }
    }
}
