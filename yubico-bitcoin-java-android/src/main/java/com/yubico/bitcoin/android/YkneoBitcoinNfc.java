package com.yubico.bitcoin.android;

import android.nfc.tech.IsoDep;
import com.yubico.bitcoin.api.*;
import com.yubico.bitcoin.util.YkneoConstants;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 4:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class YkneoBitcoinNfc implements YkneoBitcoin, YkneoConstants {
    private static final byte[] SELECT_CMD = new byte[]{0x00, (byte) 0xa4, 0x04, 0x00};
    private static final byte[] APDU_OK = new byte[]{(byte) 0x90, 00};

    private static byte[] sendCommand(IsoDep nfc, int cla, int ins, int p1, int p2, byte[] data) throws OperationInterruptedException {
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

    private static boolean compareStatus(byte[] apdu, byte[] status) {
        return apdu[apdu.length - 2] == status[0] && apdu[apdu.length - 1] == status[1];
    }

    private static void apduError(byte[] resp) {
        throw new RuntimeException(String.format("APDU error: 0x%04x", resp[resp.length-2] << 8 | resp[resp.length-1]));
    }

    private final IsoDep nfc;
    private boolean userUnlocked = false;
    private boolean adminUnlocked = false;

    public YkneoBitcoinNfc(IsoDep nfc) throws IOException {
        this.nfc = nfc;

        nfc.connect();

        byte[] selectCmd = new byte[SELECT_CMD.length + AID.length + 1];
        selectCmd[4] = (byte)AID.length;
        System.arraycopy(SELECT_CMD, 0, selectCmd, 0, SELECT_CMD.length);
        System.arraycopy(AID, 0, selectCmd, 5, AID.length);

        byte[] resp = nfc.transceive(selectCmd);
        if(!compareStatus(resp, APDU_OK)) {
            apduError(resp);
        }
    }

    @Override
    public void unlockUser(String pin) throws IncorrectPINException, OperationInterruptedException {
        byte[] resp = sendCommand(nfc, 0x00, INS_VERIFY_PIN, 0x00, 0x00, pin.getBytes());
        if (compareStatus(resp, APDU_OK)) {
            userUnlocked = true;
        } else if(resp[resp.length-2] == 0x63 && (resp[resp.length-1] & 0xf0) == 0xc0) {
            userUnlocked = false;
            throw new IncorrectPINException(PinMode.USER, resp[resp.length-1] & 0xf);
        } else {
            apduError(resp);
        }
    }

    @Override
    public void unlockAdmin(String pin) throws IncorrectPINException, OperationInterruptedException {
        byte[] resp = sendCommand(nfc, 0x00, INS_VERIFY_PIN, 0x00, 0x01, pin.getBytes());
        if (compareStatus(resp, APDU_OK)) {
            adminUnlocked = true;
        } else if(resp[resp.length-2] == 0x63 && (resp[resp.length-1] & 0xf0) == 0xc0) {
            adminUnlocked = false;
            throw new IncorrectPINException(PinMode.ADMIN, resp[resp.length-1] & 0xf);
        } else {
            apduError(resp);
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
        byte[] oldPinBytes = oldPin.getBytes();
        byte[] newPinBytes = newPin.getBytes();
        byte[] data = new byte[oldPinBytes.length + newPinBytes.length + 2];
        data[0] = (byte) oldPinBytes.length;
        System.arraycopy(oldPinBytes, 0, data, 1, oldPinBytes.length);
        data[oldPinBytes.length + 1] = (byte) newPinBytes.length;
        System.arraycopy(newPinBytes, 0, data, oldPinBytes.length + 2, newPinBytes.length);

        byte[] resp = sendCommand(nfc, 0x00, INS_SET_PIN, 0x00, 0x00, data);
        if (compareStatus(resp, APDU_OK)) {
            userUnlocked = true;
        } else if (resp[resp.length-2] == 0x63 && (resp[resp.length-1] & 0xf0) == 0xc0) {
            userUnlocked = false;
            throw new IncorrectPINException(PinMode.USER, resp[resp.length-1] & 0xf);
        } else {
            apduError(resp);
        }
    }

    @Override
    public void setAdminPin(String oldPin, String newPin) throws IncorrectPINException, OperationInterruptedException {
        byte[] oldPinBytes = oldPin.getBytes();
        byte[] newPinBytes = newPin.getBytes();
        byte[] data = new byte[oldPinBytes.length + newPinBytes.length + 2];
        data[0] = (byte) oldPinBytes.length;
        System.arraycopy(oldPinBytes, 0, data, 1, oldPinBytes.length);
        data[oldPinBytes.length + 1] = (byte) newPinBytes.length;
        System.arraycopy(newPinBytes, 0, data, oldPinBytes.length + 2, newPinBytes.length);

        byte[] resp = sendCommand(nfc, 0x00, INS_SET_PIN, 0x00, 0x01, data);
        if (compareStatus(resp, APDU_OK)) {
            adminUnlocked = true;
        } else if (resp[resp.length-2] == 0x63 && (resp[resp.length-1] & 0xf0) == 0xc0) {
            adminUnlocked = false;
            throw new IncorrectPINException(PinMode.ADMIN, resp[resp.length-1] & 0xf);
        } else {
            apduError(resp);
        }
    }

    @Override
    public void resetUserPin(String newPin) throws PinModeLockedException, OperationInterruptedException {
        if (!adminUnlocked) {
            throw new PinModeLockedException(PinMode.ADMIN);
        }

        byte[] resp = sendCommand(nfc, 0x00, INS_RESET_USER_PIN, 0x00, 0x00, newPin.getBytes());
        if (!compareStatus(resp, APDU_OK)) {
            apduError(resp);
        }
    }

    @Override
    public byte[] getHeader() throws PinModeLockedException, OperationInterruptedException {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public byte[] getPublicKey(int index) throws PinModeLockedException, UnusableIndexException, OperationInterruptedException {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public byte[] sign(int index, byte[] hash) throws PinModeLockedException, UnusableIndexException, OperationInterruptedException {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public byte[] generateMasterKeyPair(boolean allowExport, boolean returnPrivateKey) throws PinModeLockedException, OperationInterruptedException {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void importExtendedKeyPair(byte[] extendedPrivateKey, boolean allowExport) throws PinModeLockedException, OperationInterruptedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public byte[] exportExtendedPublicKey() throws PinModeLockedException, OperationInterruptedException {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
