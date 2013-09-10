package com.yubico.bitcoin.pcsc;

import com.yubico.bitcoin.api.IncorrectPINException;
import com.yubico.bitcoin.api.OperationInterruptedException;
import org.junit.*;
import static org.junit.Assert.*;

import javax.smartcardio.*;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 1:57 PM
 * To change this template use File | Settings | File Templates.
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
    public void testGenerateKey() throws Exception {
        neo.unlockAdmin(adminPin);
        Future<byte[]> future = neo.generateMasterKeyPair(true, true);
        byte[] privateKey = future.get(60, TimeUnit.SECONDS);
        assertEquals(78, privateKey.length);
    }

    @Test
    public void testImportKey() throws Exception {
        neo.unlockAdmin(adminPin);
        //byte[] importKey = ;
    }
}
