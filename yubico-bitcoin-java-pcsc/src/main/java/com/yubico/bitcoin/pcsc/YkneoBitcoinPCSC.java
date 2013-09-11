package com.yubico.bitcoin.pcsc;

import com.yubico.bitcoin.api.*;
import com.yubico.bitcoin.util.YkneoConstants;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.nio.charset.Charset;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 12:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class YkneoBitcoinPCSC implements YkneoBitcoin, YkneoConstants {

    public static final Charset ASCII = Charset.forName("US-ASCII");

    private static ResponseAPDU send(CardChannel channel, CommandAPDU apdu) throws OperationInterruptedException {
        try {
            return channel.transmit(apdu);
        } catch (CardException e) {
            throw new OperationInterruptedException(e);
        }
    }

    private static byte[] sendAndCheck(CardChannel channel, CommandAPDU apdu) throws OperationInterruptedException {
        ResponseAPDU resp = send(channel, apdu);
        if (resp.getSW() != 0x9000) {
            throw new RuntimeException(String.format("APDU error: 0x%04x", resp.getSW()));
        }
        return resp.getData();
    }

    private final CardChannel channel;
    private boolean userUnlocked = false;
    private boolean adminUnlocked = false;

    public YkneoBitcoinPCSC(CardChannel channel) throws CardException {
        this.channel = channel;
        ResponseAPDU resp = channel.transmit(new CommandAPDU(0x00, 0xa4, 0x04, 0x00, AID));
        if (resp.getSW() != 0x9000) {
            throw new CardException(String.format("Unable to select the applet, error code: 0x%04x", resp.getSW()));
        }
    }

    @Override
    public byte[] exportExtendedPublicKey() throws PinModeLockedException, OperationInterruptedException {
        if (!adminUnlocked) {
            throw new PinModeLockedException(PinMode.ADMIN);
        }

        return sendAndCheck(channel, new CommandAPDU(0x00, INS_EXPORT_EXT_PUB_KEY, 0x00, 0x00));
    }

    @Override
    public void unlockUser(String pin) throws IncorrectPINException, OperationInterruptedException {
        byte[] pinBytes = pin.getBytes(ASCII);
        ResponseAPDU resp = send(channel, new CommandAPDU(0x00, INS_VERIFY_PIN, 0x00, 0x00, pinBytes));
        if (resp.getSW() == 0x9000) {
            userUnlocked = true;
        } else if ((resp.getSW() & 0xfff0) == 0x63C0) {
            userUnlocked = false;
            throw new IncorrectPINException(PinMode.USER, resp.getSW2() & 0xf);
        } else {
            throw new RuntimeException(String.format("APDU error: 0x%04x", resp.getSW()));
        }
    }

    @Override
    public void unlockAdmin(String pin) throws IncorrectPINException, OperationInterruptedException {
        byte[] pinBytes = pin.getBytes(ASCII);
        ResponseAPDU resp = send(channel, new CommandAPDU(0x00, INS_VERIFY_PIN, 0x00, 0x01, pinBytes));
        if (resp.getSW() == 0x9000) {
            adminUnlocked = true;
        } else if ((resp.getSW() & 0xfff0) == 0x63C0) {
            adminUnlocked = false;
            throw new IncorrectPINException(PinMode.ADMIN, resp.getSW2() & 0xf);
        } else {
            throw new RuntimeException(String.format("APDU error: 0x%04x", resp.getSW()));
        }
    }

    @Override
    public boolean isUserUnlocked() {
        return userUnlocked;
    }

    @Override
    public boolean isAdminUnlocked() {
        return adminUnlocked;
    }

    @Override
    public void setUserPin(String oldPin, String newPin) throws IncorrectPINException, OperationInterruptedException {
        byte[] oldPinBytes = oldPin.getBytes(ASCII);
        byte[] newPinBytes = newPin.getBytes(ASCII);
        byte[] data = new byte[oldPinBytes.length + newPinBytes.length + 2];
        data[0] = (byte) oldPinBytes.length;
        System.arraycopy(oldPinBytes, 0, data, 1, oldPinBytes.length);
        data[oldPinBytes.length + 1] = (byte) newPinBytes.length;
        System.arraycopy(newPinBytes, 0, data, oldPinBytes.length + 2, newPinBytes.length);

        ResponseAPDU resp = send(channel, new CommandAPDU(0x00, INS_SET_PIN, 0x00, 0x00, data));
        if (resp.getSW() == 0x9000) {
            userUnlocked = true;
        } else if ((resp.getSW() & 0xfff0) == 0x63C0) {
            userUnlocked = false;
            throw new IncorrectPINException(PinMode.USER, resp.getSW2() & 0xf);
        } else {
            throw new RuntimeException(String.format("APDU error: 0x%04x", resp.getSW()));
        }
    }

    @Override
    public void setAdminPin(String oldPin, String newPin) throws IncorrectPINException, OperationInterruptedException {
        byte[] oldPinBytes = oldPin.getBytes(ASCII);
        byte[] newPinBytes = newPin.getBytes(ASCII);
        byte[] data = new byte[oldPinBytes.length + newPinBytes.length + 2];
        data[0] = (byte) oldPinBytes.length;
        System.arraycopy(oldPinBytes, 0, data, 1, oldPinBytes.length);
        data[oldPinBytes.length + 1] = (byte) newPinBytes.length;
        System.arraycopy(newPinBytes, 0, data, oldPinBytes.length + 2, newPinBytes.length);

        ResponseAPDU resp = send(channel, new CommandAPDU(0x00, INS_SET_PIN, 0x00, 0x01, data));
        if (resp.getSW() == 0x9000) {
            adminUnlocked = true;
        } else if ((resp.getSW() & 0xfff0) == 0x63C0) {
            adminUnlocked = false;
            throw new IncorrectPINException(PinMode.ADMIN, resp.getSW2() & 0xf);
        } else {
            throw new RuntimeException(String.format("APDU error: 0x%04x", resp.getSW()));
        }
    }

    @Override
    public void resetUserPin(String newPin) throws PinModeLockedException, OperationInterruptedException {
        if (!adminUnlocked) {
            throw new PinModeLockedException(PinMode.ADMIN);
        }

        sendAndCheck(channel, new CommandAPDU(0x00, INS_RESET_USER_PIN, 0x00, 0x00, newPin.getBytes(ASCII)));
    }

    @Override
    public byte[] getHeader() throws PinModeLockedException, OperationInterruptedException {
        if (!userUnlocked) {
            throw new PinModeLockedException(PinMode.USER);
        }
        return sendAndCheck(channel, new CommandAPDU(0x00, INS_GET_HEADER, 0x00, 0x00));
    }

    @Override
    public byte[] getPublicKey(int index) throws PinModeLockedException, OperationInterruptedException {
        if (!userUnlocked) {
            throw new PinModeLockedException(PinMode.USER);
        }
        return sendAndCheck(channel, new CommandAPDU(0x00, INS_GET_PUB, 0x00, 0x00, new byte[]{(byte) (index >> 24), (byte) (index >> 16), (byte) (index >> 8), (byte) index}));
    }

    @Override
    public byte[] sign(int index, byte[] hash) throws PinModeLockedException, OperationInterruptedException {
        if (!userUnlocked) {
            throw new PinModeLockedException(PinMode.USER);
        }
        byte[] data = new byte[hash.length + 4];
        System.arraycopy(new byte[]{(byte) (index >> 24), (byte) (index >> 16), (byte) (index >> 8), (byte) index}, 0, data, 0, 4);
        System.arraycopy(hash, 0, data, 4, hash.length);
        return sendAndCheck(channel, new CommandAPDU(0x00, INS_SIGN, 0x00, 0x00, data));
    }

    @Override
    public byte[] generateMasterKeyPair(boolean allowExport, boolean returnPrivateKey) throws PinModeLockedException, OperationInterruptedException {
        if (!adminUnlocked) {
            throw new PinModeLockedException(PinMode.ADMIN);
        }
        byte p2 = 0x00;
        if (allowExport) {
            p2 |= FLAG_CAN_EXPORT;
        }
        if (returnPrivateKey) {
            p2 |= FLAG_RETURN_PRIVATE;
        }
        return sendAndCheck(channel, new CommandAPDU(0x00, INS_GENERATE_KEY_PAIR, 0x00, p2));
    }

    @Override
    public void importExtendedKeyPair(byte[] extendedPrivateKey, boolean allowExport) throws PinModeLockedException, OperationInterruptedException {
        if (!adminUnlocked) {
            throw new PinModeLockedException(PinMode.ADMIN);
        }
        byte p2 = allowExport ? FLAG_CAN_EXPORT : 0x00;
        sendAndCheck(channel, new CommandAPDU(0x00, INS_IMPORT_KEY_PAIR, 0x00, p2, extendedPrivateKey));
    }
}