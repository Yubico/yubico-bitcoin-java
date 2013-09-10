package com.yubico.bitcoin.pcsc;

import com.google.common.io.BaseEncoding;
import com.yubico.bitcoin.api.IncorrectPINException;
import com.yubico.bitcoin.api.PinMode;
import org.junit.*;
import static org.junit.Assert.*;

import javax.smartcardio.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 1:57 PM
 *
 * These tests require a YubiKey NEO with the ykneo-bitcoin applet loaded, and the default PINs set.
 */
public class YkneoBitcoinPCSCTest {
    private static final String TERMINAL_NAME = "Yubikey NEO";
    private static final String userPin = "000000";
    private static final String adminPin = "00000000";

    private static Card card;
    private YkneoBitcoinPCSC neo;

    @BeforeClass
    public static void setupClass() throws CardException {
        TerminalFactory factory = TerminalFactory.getDefault();
        CardTerminals terminals = factory.terminals();
        for(CardTerminal terminal : terminals.list()) {
            if(terminal.getName().contains(TERMINAL_NAME)) {
                card = terminal.connect("*");
            }
        }
        assertNotNull("YubiKey NEO not found!", card);
    }

    @Before
    public void setup() throws CardException {
        neo = new YkneoBitcoinPCSC(card.getBasicChannel());
    }

    @Test
    public void testSetUserPin() throws Exception {
        String newPin = "hello world";
        neo.setUserPin(userPin, newPin);

        try {
            neo.unlockUser(userPin);
            assertTrue(neo.isUserUnlocked());
        } catch (IncorrectPINException e) {
            assertEquals(2, e.getTriesRemaining());
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
            assertTrue(neo.isAdminUnlocked());
        } catch (IncorrectPINException e) {
            assertEquals(2, e.getTriesRemaining());
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
        while(!locked && tries-- > 0) {
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
        Future<byte[]> future = neo.generateMasterKeyPair(true, true);
        byte[] privateKey = future.get(1, TimeUnit.MINUTES);
        assertEquals(78, privateKey.length);
    }

    @Test
    public void testImport() throws Exception {
        neo.unlockAdmin(adminPin);
        byte[] importKey = BaseEncoding.base16().lowerCase().decode("0488ade400000000000000000060499f801b896d83179a4374aeb7822aaeaceaa0db1f85ee3e904c4defbd9689004b03d6fc340455b363f51020ad3ecca4f0850280cf436c70c727923f6db46c3e");
        neo.importExtendedKeyPair(importKey, true).get(1, TimeUnit.MINUTES);
    }

    @Test
    public void testExport() throws Exception {
        testImport();
        byte[] expectedPubKey = BaseEncoding.base16().lowerCase().decode("0488b21e00000000000000000060499f801b896d83179a4374aeb7822aaeaceaa0db1f85ee3e904c4defbd968903cbcaa9c98c877a26977d00825c956a238e8dddfbd322cce4f74b0b5bd6ace4a7");
        byte[] pubKey = neo.exportExtendedPublicKey().get(1, TimeUnit.MINUTES);
        assertArrayEquals(expectedPubKey, pubKey);
    }

    @Test
    public void testGetAndSign() throws Exception {
        testImport();
        neo.unlockUser(userPin);
        byte[] pubKey = neo.getPublicKey(0).get(1, TimeUnit.MINUTES);
        byte[] expectedPubKey = BaseEncoding.base16().lowerCase().decode("04fc9e5af0ac8d9b3cecfe2a888e2117ba3d089d8585886c9c826b6b22a98d12ea67a50538b6f7d8b5f7a1cc657efd267cde8cc1d8c0451d1340a0fb3642777544");
        assertArrayEquals(expectedPubKey, pubKey);

        byte[] hash = new byte[32];
        byte[] signature = neo.sign(0, hash).get(1, TimeUnit.MINUTES);
        //TODO: Verify signature.
    }
}
