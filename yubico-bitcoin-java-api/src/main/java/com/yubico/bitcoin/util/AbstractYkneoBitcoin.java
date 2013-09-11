package com.yubico.bitcoin.util;

import com.yubico.bitcoin.api.*;

import java.nio.charset.Charset;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/11/13
 * Time: 11:12 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractYkneoBitcoin implements YkneoBitcoin, YkneoConstants {
    public static final Charset ASCII = Charset.forName("US-ASCII");
    public static final byte[] NO_DATA = new byte[0];

    private boolean userUnlocked = false;
    private boolean adminUnlocked = false;

    protected static int apduStatus(byte[] apdu) {
        return ((apdu[apdu.length - 2] & 0xff) << 8) | (apdu[apdu.length - 1] & 0xff);
    }

    protected static void apduError(byte[] resp) {
        throw new RuntimeException(String.format("APDU error: 0x%04x", resp[resp.length-2] << 8 | resp[resp.length-1]));
    }

    protected abstract byte[] send(int cla, int ins, int p1, int p2, byte[] data) throws OperationInterruptedException;

    protected void select() throws OperationInterruptedException {
        byte[] resp = send(0x00, 0xa4, 0x04, 0x00, AID);
        int status = apduStatus(resp);
        if (status != 0x9000) {
            throw new RuntimeException(String.format("Unable to select the applet, error code: 0x%04x", status));
        }
    }

    protected byte[] sendAndCheck(int cla, int ins, int p1, int p2, byte[] data) throws OperationInterruptedException {
        byte[] resp = send(cla, ins, p1, p2, data);
        if (apduStatus(resp) != 0x9000) {
            apduError(resp);
        }
        byte[] respData = new byte[resp.length -2];
        System.arraycopy(resp, 0, respData, 0, respData.length);
        return respData;
    }

    @Override
    public byte[] exportExtendedPublicKey() throws PinModeLockedException, OperationInterruptedException {
        if (!adminUnlocked) {
            throw new PinModeLockedException(PinMode.ADMIN);
        }

        return sendAndCheck(0x00, INS_EXPORT_EXT_PUB_KEY, 0x00, 0x00, NO_DATA);
    }

    @Override
    public void unlockUser(String pin) throws IncorrectPINException, OperationInterruptedException {
        byte[] pinBytes = pin.getBytes(ASCII);
        byte[] resp = send(0x00, INS_VERIFY_PIN, 0x00, 0x00, pinBytes);
        int status = apduStatus(resp);
        if (status == 0x9000) {
            userUnlocked = true;
        } else if ((status & 0xfff0) == 0x63C0) {
            userUnlocked = false;
            throw new IncorrectPINException(PinMode.USER, status & 0x000f);
        } else {
            throw new RuntimeException(String.format("APDU error: 0x%04x", status));
        }
    }

    @Override
    public void unlockAdmin(String pin) throws IncorrectPINException, OperationInterruptedException {
        byte[] pinBytes = pin.getBytes(ASCII);
        byte[] resp = send(0x00, INS_VERIFY_PIN, 0x00, 0x01, pinBytes);
        int status = apduStatus(resp);
        if (status == 0x9000) {
            adminUnlocked = true;
        } else if ((status & 0xfff0) == 0x63C0) {
            adminUnlocked = false;
            throw new IncorrectPINException(PinMode.ADMIN, status & 0x000f);
        } else {
            throw new RuntimeException(String.format("APDU error: 0x%04x", status));
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

        byte[] resp = send(0x00, INS_SET_PIN, 0x00, 0x00, data);
        int status = apduStatus(resp);
        if (status == 0x9000) {
            userUnlocked = true;
        } else if ((status & 0xfff0) == 0x63C0) {
            userUnlocked = false;
            throw new IncorrectPINException(PinMode.USER, status & 0x000f);
        } else {
            throw new RuntimeException(String.format("APDU error: 0x%04x", status));
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

        byte[] resp = send(0x00, INS_SET_PIN, 0x00, 0x01, data);
        int status = apduStatus(resp);
        if (status == 0x9000) {
            adminUnlocked = true;
        } else if ((status & 0xfff0) == 0x63C0) {
            adminUnlocked = false;
            throw new IncorrectPINException(PinMode.ADMIN, status & 0x000f);
        } else {
            throw new RuntimeException(String.format("APDU error: 0x%04x", status));
        }
    }

    @Override
    public void resetUserPin(String newPin) throws PinModeLockedException, OperationInterruptedException {
        if (!adminUnlocked) {
            throw new PinModeLockedException(PinMode.ADMIN);
        }

        sendAndCheck(0x00, INS_RESET_USER_PIN, 0x00, 0x00, newPin.getBytes(ASCII));
    }

    @Override
    public byte[] getHeader() throws PinModeLockedException, OperationInterruptedException {
        if (!userUnlocked) {
            throw new PinModeLockedException(PinMode.USER);
        }
        return sendAndCheck(0x00, INS_GET_HEADER, 0x00, 0x00, NO_DATA);
    }

    @Override
    public byte[] getPublicKey(int index) throws PinModeLockedException, OperationInterruptedException {
        if (!userUnlocked) {
            throw new PinModeLockedException(PinMode.USER);
        }
        return sendAndCheck(0x00, INS_GET_PUB, 0x00, 0x00, new byte[]{(byte) (index >> 24), (byte) (index >> 16), (byte) (index >> 8), (byte) index});
    }

    @Override
    public byte[] sign(int index, byte[] hash) throws PinModeLockedException, OperationInterruptedException {
        if (!userUnlocked) {
            throw new PinModeLockedException(PinMode.USER);
        }
        byte[] data = new byte[hash.length + 4];
        System.arraycopy(new byte[]{(byte) (index >> 24), (byte) (index >> 16), (byte) (index >> 8), (byte) index}, 0, data, 0, 4);
        System.arraycopy(hash, 0, data, 4, hash.length);
        return sendAndCheck(0x00, INS_SIGN, 0x00, 0x00, data);
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
        return sendAndCheck(0x00, INS_GENERATE_KEY_PAIR, 0x00, p2, NO_DATA);
    }

    @Override
    public void importExtendedKeyPair(byte[] extendedPrivateKey, boolean allowExport) throws PinModeLockedException, OperationInterruptedException {
        if (!adminUnlocked) {
            throw new PinModeLockedException(PinMode.ADMIN);
        }
        byte p2 = allowExport ? FLAG_CAN_EXPORT : 0x00;
        send(0x00, INS_IMPORT_KEY_PAIR, 0x00, p2, extendedPrivateKey);
    }
}
