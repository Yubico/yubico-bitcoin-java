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

import com.google.common.io.BaseEncoding;
import com.yubico.bitcoin.api.*;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 1:57 PM
 * <p/>
 * These tests require a YubiKey NEO with the ykneo-bitcoin applet loaded, and the default PINs set.
 */
public class YkneoBitcoinSoftTest {
    private static final String TERMINAL_NAME = "Yubikey NEO";
    private static final String userPin = "000000";
    private static final String adminPin = "00000000";

    private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();

    private YkneoBitcoin neo;

    @Before
    public void setup() throws Exception {
        neo = new YkneoBitcoinSoft();
    }

    @Test
    public void testGetVersion() throws Exception {
        assertEquals("0.1.0", neo.getAppletVersion());
    }

    @Test
    public void testSetUserPin() throws Exception {
        String newPin = "hello world";
        neo.setUserPin(userPin, newPin);

        try {
            neo.unlockUser(userPin);
            fail("Unlocked with wrong PIN!");
        } catch (IncorrectPINException e) {
            assertEquals(5, e.getTriesRemaining());
            assertEquals(PinMode.USER, e.getPinMode());
            assertFalse(neo.isUserUnlocked());
        }

        neo.unlockUser(newPin);
        neo.setUserPin(newPin, userPin);
    }

    @Test
    public void testSetAdminPin() throws Exception {
        String newPin = "hello world";
        neo.setAdminPin(adminPin, newPin);

        try {
            neo.unlockAdmin(adminPin);
            fail("Unlocked with wrong PIN!");
        } catch (IncorrectPINException e) {
            assertEquals(5, e.getTriesRemaining());
            assertEquals(PinMode.ADMIN, e.getPinMode());
            assertFalse(neo.isAdminUnlocked());
        }

        neo.unlockAdmin(newPin);
        neo.setAdminPin(newPin, adminPin);
    }

    @Test
    public void testResetUserPin() throws Exception {
        neo.unlockAdmin(adminPin);
        boolean locked = false;
        int tries = 10;
        while (!locked && tries-- > 0) {
            try {
                neo.unlockUser("foobar");
            } catch (IncorrectPINException e) {
                locked = e.getTriesRemaining() == 0;
            }
        }
        assertTrue(locked);

        neo.resetUserPin(userPin);
        neo.unlockUser(userPin);
    }

    @Test
    public void testGenerateKey() throws Exception {
        neo.unlockAdmin(adminPin);
        byte[] privateKey = neo.generateMasterKeyPair(true, true, false);
        assertEquals(78, privateKey.length);
    }

    @Test
    public void testImportExtended() throws Exception {
        neo.unlockAdmin(adminPin);
        byte[] importKey = HEX.decode("0488ade4000000000000000000873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d50800e8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35");
        neo.importExtendedKeyPair(importKey, true);
    }

    @Test
    public void testExportExtended() throws Exception {
        testImportExtended();
        String expectedPubKey = "0488b21e000000000000000000873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d5080339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2";
        String pubKey = HEX.encode(neo.exportExtendedPublicKey());
        assertEquals(expectedPubKey, pubKey);
    }

    @Test
    public void testGetHeader() throws Exception {
        testImportExtended();
        neo.unlockUser(userPin);
        String expectedHeader = "0488ade4000000000000000000";
        String header = HEX.encode(neo.getHeader());
        assertEquals(expectedHeader, header);
    }

    @Test
    public void testGetChild() throws Exception {
        testImportExtended();
        neo.unlockUser(userPin);
        String pubKey = HEX.encode(neo.getPublicKey(false, 0x80000000)); // m/0'
        String expectedPubKey = "045a784662a4a20a65bf6aab9ae98a6c068a81c52e4b032c0fb5400c706cfccc567f717885be239daadce76b568958305183ad616ff74ed4dc219a74c26d35f839";
        assertEquals(expectedPubKey, pubKey);
    }

    @Test
    public void testGetDescendant() throws Exception {
        testImportExtended();
        neo.unlockUser(userPin);
        String pubKey = HEX.encode(neo.getPublicKey(true, 0x80000000, 1, 0x80000002)); // m/0'/1/2'/2/1000000000
        String expectedPubKey = "0357bfe1e341d01c69fe5654309956cbea516822fba8a601743a012a7896ee8dc2";
        assertEquals(expectedPubKey, pubKey);
    }

    @Test
    public void testSign() throws Exception {
        testImportExtended();
        neo.unlockUser(userPin);
        byte[] hash = new byte[32];
        byte[] signature = neo.sign(hash, 0);
        //TODO: Verify signature.
        assertThat(signature.length, Matchers.lessThanOrEqualTo(72));
    }
}
