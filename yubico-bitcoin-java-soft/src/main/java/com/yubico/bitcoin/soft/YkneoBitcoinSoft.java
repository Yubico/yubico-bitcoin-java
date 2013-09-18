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

package com.yubico.bitcoin.soft;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.crypto.ChildNumber;
import com.google.bitcoin.crypto.DeterministicHierarchy;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.yubico.bitcoin.api.*;
import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.math.ec.ECPoint;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a software implementation of YkneoBitcoin which is meant to be used for testing purposed only!
 *
 * It will allow you to test interop with YkneoBitcoin without requiring a physical YubiKey NEO with the applet.
 */
public class YkneoBitcoinSoft implements YkneoBitcoin {
    private static final byte[] VERSION = new byte[]{0, 0, 1};
    private static final int PIN_TRIES = 3;

    private final SecureRandom random = new SecureRandom();

    private DeterministicHierarchy master;

    private String userPin = "000000";
    private String adminPin = "00000000";
    private boolean allowExport = false;

    private int userTries = PIN_TRIES;
    private int adminTries = PIN_TRIES;
    private boolean userLocked = true;
    private boolean adminLocked = true;

    public YkneoBitcoinSoft() {
    }

    @Override
    public byte[] exportExtendedPublicKey() throws PinModeLockedException, IOException, OperationNotPermittedException, NoKeyLoadedException {
        ensurePin(PinMode.ADMIN);
        ensureKey();
        if(!allowExport) {
            throw new OperationNotPermittedException();
        }
        return master.getRootKey().serializePublic();
    }

    @Override
    public byte[] getAppletVersion() {
        return VERSION;
    }

    @Override
    public void unlockUser(String pin) throws IncorrectPINException, IOException {
        if (userPin.equals(pin)) {
            userLocked = false;
            userTries = PIN_TRIES;
            return;
        } else if (userTries > 0) {
            userTries--;
        }
        userLocked = true;
        throw new IncorrectPINException(PinMode.USER, userTries);
    }

    @Override
    public void unlockAdmin(String pin) throws IncorrectPINException, IOException {
        if (adminPin.equals(pin)) {
            adminLocked = false;
            adminTries = PIN_TRIES;
            return;
        } else if (adminTries > 0) {
            adminTries--;
        }
        adminLocked = true;
        throw new IncorrectPINException(PinMode.ADMIN, adminTries);
    }

    @Override
    public boolean isUserUnlocked() {
        return !userLocked;
    }

    @Override
    public boolean isAdminUnlocked() {
        return !adminLocked;
    }

    @Override
    public void setUserPin(String oldPin, String newPin) throws IncorrectPINException, IOException {
        unlockUser(oldPin);
        userPin = newPin;
    }

    @Override
    public void setAdminPin(String oldPin, String newPin) throws IncorrectPINException, IOException {
        unlockAdmin(oldPin);
        adminPin = newPin;
    }

    private void ensurePin(PinMode mode) throws PinModeLockedException {
        switch (mode) {
            case USER:
                if (isUserUnlocked()) {
                    return;
                }
                break;
            case ADMIN:
                if (isAdminUnlocked()) {
                    return;
                }
                break;
        }
        throw new PinModeLockedException(mode);
    }

    @Override
    public void resetUserPin(String newPin) throws PinModeLockedException, IOException {
        ensurePin(PinMode.ADMIN);
        userPin = newPin;
        userTries = PIN_TRIES;
    }

    private void ensureKey() throws NoKeyLoadedException {
        if (master == null) {
            throw new NoKeyLoadedException();
        }
    }

    @Override
    public byte[] getHeader() throws PinModeLockedException, IOException, NoKeyLoadedException {
        ensurePin(PinMode.USER);
        ensureKey();
        byte[] header = new byte[13];
        System.arraycopy(master.getRootKey().serializePrivate(), 0, header, 0, 13);
        return header;
    }

    private DeterministicKey getKey(int... index) throws PinModeLockedException, NoKeyLoadedException {
        ensurePin(PinMode.USER);
        ensureKey();
        List<ChildNumber> path = new ArrayList<ChildNumber>();
        for(int i : index) {
            if((i & 0x80000000) != 0) {
                path.add(new ChildNumber(i & 0x7fffffff, true));
            } else {
                path.add(new ChildNumber(i, false));
            }
        }

        return master.get(path, true, true);
    }

    @Override
    public byte[] getPublicKey(boolean compress, int... index) throws PinModeLockedException, UnusableIndexException, IOException, NoKeyLoadedException {
        DeterministicKey key = getKey(index);
        X9ECParameters params = SECNamedCurves.getByName("secp256k1");
        ECPoint point = params.getCurve().decodePoint(key.toECKey().getPubKey());
        point = new ECPoint.Fp(point.getCurve(), point.getX(), point.getY(), compress);
        return point.getEncoded();
    }

    @Override
    public byte[] sign(byte[] hash, int... index) throws PinModeLockedException, UnusableIndexException, IOException, NoKeyLoadedException {
        DeterministicKey key = getKey(index);
        ECKey.ECDSASignature signature = key.toECKey().sign(new Sha256Hash(hash));
        return signature.encodeToDER();
    }

    @Override
    public byte[] generateMasterKeyPair(boolean allowExport, boolean returnPrivateKey, boolean testnetKey) throws PinModeLockedException, IOException {
        ensurePin(PinMode.ADMIN);
        master = new DeterministicHierarchy(HDKeyDerivation.createMasterPrivateKey(random.generateSeed(32)));
        this.allowExport = allowExport;
        return returnPrivateKey ? master.getRootKey().serializePrivate() : new byte[0];
    }

    @Override
    public void importExtendedKeyPair(byte[] extendedPrivateKey, boolean allowExport) throws PinModeLockedException, IOException {
        ensurePin(PinMode.ADMIN);
        byte[] privkey = new byte[33];
        byte[] chaincode = new byte[32];
        System.arraycopy(extendedPrivateKey, 13, chaincode, 0, 32);
        System.arraycopy(extendedPrivateKey, 45, privkey, 0, 33);

        master = new DeterministicHierarchy(HDKeyDerivation.createMasterPrivKeyFromBytes(privkey, chaincode));
        this.allowExport = allowExport;
    }
}
