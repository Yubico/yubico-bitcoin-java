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

package com.yubico.bitcoin.util;

import com.yubico.bitcoin.api.*;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Abstract class for implementing YkneoBitcoin.
 * Provides full implementation for YkneoBitcoin, the extending class need only implement the send method.
 * Subclasses should call select() before trying to invoke any other methods, but after send has been initialized.
 */
public abstract class AbstractYkneoBitcoin implements YkneoBitcoin, YkneoConstants {
    public static final Charset ASCII = Charset.forName("US-ASCII");
    public static final byte[] NO_DATA = new byte[0];

    protected static int apduStatus(byte[] apdu) {
        return ((apdu[apdu.length - 2] & 0xff) << 8) | (apdu[apdu.length - 1] & 0xff);
    }

    private static byte[] fromInts(int[] ints) {
        byte[] bytes = new byte[ints.length * 4];
        int offset = 0;
        for (int val : ints) {
            bytes[offset++] = (byte) (val >> 24);
            bytes[offset++] = (byte) (val >> 16);
            bytes[offset++] = (byte) (val >> 8);
            bytes[offset++] = (byte) val;
        }

        return bytes;
    }

    private final Requirements req = new Requirements();

    private final byte[] version = new byte[3];
    private boolean keyLoaded = false;
    private boolean userUnlocked = false;
    private boolean adminUnlocked = false;

    protected abstract byte[] send(int cla, int ins, int p1, int p2, byte[] data) throws IOException;

    protected void select() throws IOException {
        byte[] resp = send(0x00, 0xa4, 0x04, 0x00, AID);
        int status = apduStatus(resp);
        if (status != 0x9000) {
            throw new IOException(String.format("Unable to select the applet, error code: 0x%04x", status));
        }
        System.arraycopy(resp, 0, version, 0, 3);
        if(resp[3] == 1) {
            keyLoaded = true;
        }
        require().versionLessThan(1, 0, 0);
    }

    protected byte[] sendAndCheck(int cla, int ins, int p1, int p2, byte[] data) throws IOException {
        byte[] resp = send(cla, ins, p1, p2, data);
        if (apduStatus(resp) != 0x9000) {
            if (apduStatus(resp) == 0x6a82) {
                throw new NoKeyLoadedException();
            } else if (apduStatus(resp) == 0x6986) {
                throw new OperationNotPermittedException();
            }
            throw new RuntimeException(String.format("APDU error: 0x%02x%02x", resp[resp.length - 2], resp[resp.length - 1]));
        }
        byte[] respData = new byte[resp.length - 2];
        System.arraycopy(resp, 0, respData, 0, respData.length);
        return respData;
    }

    protected Requirements require() {
        return req;
    }

    @Override
    public String getAppletVersion() {
        return version[0] + "." + version[1] + "." + version[2];
    }

    @Override
    public boolean isKeyLoaded() {
        return keyLoaded;
    }

    @Override
    public byte[] exportExtendedPublicKey() throws PinModeLockedException, IOException {
        require().adminMode();

        return sendAndCheck(0x00, INS_EXPORT_EXT_PUB_KEY, 0x00, 0x00, NO_DATA);
    }

    @Override
    public void unlockUser(String pin) throws IncorrectPINException, IOException {
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
    public void unlockAdmin(String pin) throws IncorrectPINException, IOException {
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
    public void setUserPin(String oldPin, String newPin) throws IncorrectPINException, IOException {
        int status = sendSetPin(oldPin, newPin, 0x00);
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
    public void setAdminPin(String oldPin, String newPin) throws IncorrectPINException, IOException {
        int status = sendSetPin(oldPin, newPin, FLAG_ADMIN_PIN);
        if (status == 0x9000) {
            adminUnlocked = true;
        } else if ((status & 0xfff0) == 0x63C0) {
            adminUnlocked = false;
            throw new IncorrectPINException(PinMode.ADMIN, status & 0x000f);
        } else {
            throw new RuntimeException(String.format("APDU error: 0x%04x", status));
        }
    }

    private int sendSetPin(String oldPin, String newPin, int p2) throws IncorrectPINException, IOException {
        byte[] oldPinBytes = oldPin.getBytes(ASCII);
        byte[] newPinBytes = newPin.getBytes(ASCII);
        if (newPinBytes.length < 1 || newPinBytes.length > PIN_MAX_LENGTH) {
            throw new IllegalArgumentException(String.format("PIN length must be between 1 and %d", PIN_MAX_LENGTH));
        }
        byte[] data = new byte[oldPinBytes.length + newPinBytes.length + 2];
        data[0] = (byte) oldPinBytes.length;
        System.arraycopy(oldPinBytes, 0, data, 1, oldPinBytes.length);
        data[oldPinBytes.length + 1] = (byte) newPinBytes.length;
        System.arraycopy(newPinBytes, 0, data, oldPinBytes.length + 2, newPinBytes.length);

        return apduStatus(send(0x00, INS_SET_PIN, 0x00, p2, data));
    }

    @Override
    public void resetUserPin(String newPin) throws PinModeLockedException, IOException {
        require().adminMode();

        sendAndCheck(0x00, INS_RESET_USER_PIN, 0x00, 0x00, newPin.getBytes(ASCII));
    }

    @Override
    public void setAdminRetryCount(int attempts) throws PinModeLockedException, IOException {
        require().versionAtLeast(0, 1, 0).adminMode();

        if (attempts < 1 || attempts > 15) {
            throw new IllegalArgumentException(String.format("PIN retry count must be between 1 and 15, was: %d", attempts));
        }

        sendAndCheck(0x00, INS_SET_RETRY_COUNT, 0x00, 0x01, new byte[]{(byte) attempts});
    }

    @Override
    public void setUserRetryCount(int attempts) throws PinModeLockedException, IOException {
        require().versionAtLeast(0, 1, 0).adminMode();

        if (attempts < 1 || attempts > 15) {
            throw new IllegalArgumentException(String.format("PIN retry count must be between 1 and 15, was: %d", attempts));
        }

        sendAndCheck(0x00, INS_SET_RETRY_COUNT, 0x00, 0x00, new byte[]{(byte) attempts});
    }

    @Override
    public byte[] getHeader() throws PinModeLockedException, IOException {
        require().userMode();

        return sendAndCheck(0x00, INS_GET_HEADER, 0x00, 0x00, NO_DATA);
    }

    @Override
    public byte[] getPublicKey(boolean compress, int... index) throws PinModeLockedException, IOException {
        require().userMode();

        byte[] pub = sendAndCheck(0x00, INS_GET_PUB, 0x00, 0x00, fromInts(index));
        if (compress) {
            byte[] compressed = new byte[33];
            pub[0] = (byte) ((pub[pub.length - 1] & 1) == 0 ? 0x02 : 0x03);
            System.arraycopy(pub, 0, compressed, 0, compressed.length);
            return compressed;
        }
        return pub;
    }

    @Override
    public byte[] sign(byte[] hash, int... index) throws PinModeLockedException, IOException {
        require().userMode();

        if (hash.length != 32) {
            throw new IllegalArgumentException("Hash must be 32 bytes!");
        }
        byte[] indexBytes = fromInts(index);
        byte[] data = new byte[indexBytes.length + hash.length];
        System.arraycopy(indexBytes, 0, data, 0, indexBytes.length);
        System.arraycopy(hash, 0, data, indexBytes.length, hash.length);
        return sendAndCheck(0x00, INS_SIGN, 0x00, 0x00, data);
    }

    @Override
    public byte[] generateMasterKeyPair(boolean allowExport, boolean returnPrivateKey, boolean testnetKey) throws PinModeLockedException, IOException {
        require().adminMode();

        byte p2 = 0x00;
        if (allowExport) {
            p2 |= FLAG_CAN_EXPORT;
        }
        if (returnPrivateKey) {
            p2 |= FLAG_RETURN_PRIVATE;
        }
        if (testnetKey) {
            p2 |= FLAG_TESTNET;
        }
        byte[] resp = sendAndCheck(0x00, INS_GENERATE_KEY_PAIR, 0x00, p2, NO_DATA);
        keyLoaded = true;

        return resp;
    }

    @Override
    public void importExtendedKeyPair(byte[] extendedPrivateKey, boolean allowExport) throws PinModeLockedException, IOException {
        require().adminMode();

        byte p2 = allowExport ? FLAG_CAN_EXPORT : 0x00;
        sendAndCheck(0x00, INS_IMPORT_KEY_PAIR, 0x00, p2, extendedPrivateKey);
        keyLoaded = true;
    }

    /**
     * Utility for checking requirements.
     */
    public class Requirements {
        private Requirements() {
        }

        public Requirements versionAtLeast(int major, int minor, int micro) throws OperationNotSupportedException {
            if (version[0] == major) {
                if (version[1] == minor) {
                    if (version[2] >= micro) {
                        return this;
                    }
                } else if (version[1] > minor) {
                    return this;
                }
            } else if (version[0] > major) {
                return this;
            }

            throw new OperationNotSupportedException(getAppletVersion(), major + "." + minor + "." + micro);
        }

        public Requirements versionLessThan(int major, int minor, int micro) throws OperationNotSupportedException {
            if (version[0] == major) {
                if (version[1] == minor) {
                    if (version[2] < micro) {
                        return this;
                    }
                } else if (version[1] < minor) {
                    return this;
                }
            } else if (version[0] < major) {
                return this;
            }

            throw new OperationNotSupportedException(getAppletVersion(), "<" + major + "." + minor + "." + micro);
        }

        public Requirements userMode() throws PinModeLockedException {
            if (!userUnlocked) {
                throw new PinModeLockedException(PinMode.USER);
            }

            return this;
        }

        public Requirements adminMode() throws PinModeLockedException {
            if (!adminUnlocked) {
                throw new PinModeLockedException(PinMode.ADMIN);
            }

            return this;
        }
    }
}
