package com.yubico.bitcoin.pcsc;

import com.yubico.bitcoin.api.*;
import com.yubico.bitcoin.util.AbstractYkneoBitcoin;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 12:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class YkneoBitcoinPCSC extends AbstractYkneoBitcoin {
    private final CardChannel channel;

    public YkneoBitcoinPCSC(CardChannel channel) throws CardException, OperationInterruptedException {
        this.channel = channel;

        select();
    }

    @Override
    protected byte[] send(int cla, int ins, int p1, int p2, byte[] data) throws OperationInterruptedException {
        try {
            return channel.transmit(new CommandAPDU(cla, ins, p1, p2, data)).getBytes();
        } catch (CardException e) {
            throw new OperationInterruptedException(e);
        }
    }
}