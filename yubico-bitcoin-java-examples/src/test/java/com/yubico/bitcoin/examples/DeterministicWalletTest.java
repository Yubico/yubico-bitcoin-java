/*
 * Copyright (C) 2013 Yubico AB.
 * All rights reserved.
 * Proprietary code owned by Yubico AB.
 * No rights to modifications or redistribution.
 */

package com.yubico.bitcoin.examples;

import com.google.bitcoin.core.*;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.discovery.DnsDiscovery;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.common.util.concurrent.MoreExecutors;
import com.yubico.bitcoin.api.YkneoBitcoin;
import com.yubico.bitcoin.pcsc.YkneoBitcoinPCSC;

import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;
import java.io.File;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/17/13
 * Time: 10:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class DeterministicWalletTest {
    private static final NetworkParameters PARAMS = TestNet3Params.get();
    private static final File WALLET_FILE = new File("test.wallet");

    public static void main(String[] argv) throws Exception {
        TerminalFactory factory = TerminalFactory.getDefault();
        CardTerminals terminals = factory.terminals();
        Card card = null;
        for (CardTerminal terminal : terminals.list()) {
            if (terminal.getName().contains("Yubikey NEO")) {
                card = terminal.connect("*");
            }
        }

        YkneoBitcoin neo = new YkneoBitcoinPCSC(card.getBasicChannel());
        neo.unlockAdmin("00000000");

        byte[] masterPubKey = neo.exportExtendedPublicKey();
        byte[] pubkey = new byte[33];
        byte[] chaincode = new byte[32];
        System.arraycopy(masterPubKey, 13, chaincode, 0, 32);
        System.arraycopy(masterPubKey, 45, pubkey, 0, 33);
        // Try to read the wallet from storage, create a new one if not possible.
        Wallet wallet;
        boolean freshWallet = false;
        try {
            wallet = Wallet.loadFromFile(WALLET_FILE);
        } catch (UnreadableWalletException e) {
            wallet = new Wallet(PARAMS);

            wallet.saveToFile(WALLET_FILE);
            freshWallet = true;
        }

        DeterministicWallet detWallet = new DeterministicWallet(wallet, HDKeyDerivation.createMasterPubKeyFromBytes(pubkey, chaincode));

        System.out.println(wallet);

        wallet.autosaveToFile(WALLET_FILE, 500, TimeUnit.MILLISECONDS, null);

        File blockChainFile = new File("test.blockchain");
        if (!blockChainFile.exists() && !freshWallet) {
            // No block chain, but we had a wallet. So empty out the transactions in the wallet so when we rescan
            // the blocks there are no problems (wallets don't support replays without being emptied).
            wallet.clearTransactions(0);
        }

        wallet.addEventListener(new AbstractWalletEventListener() {
            @Override
            public void onWalletChanged(Wallet wallet) {
                // MUST BE THREAD SAFE.
                System.out.println(wallet.toString());
            }
        });

        BlockChain chain = new BlockChain(PARAMS, wallet, new SPVBlockStore(PARAMS, blockChainFile));
        //chain.addListener(detWallet);

        PeerGroup peerGroup = new PeerGroup(PARAMS, chain);
        peerGroup.setUserAgent("TestWallet", "0.1");
        peerGroup.addPeerDiscovery(new DnsDiscovery(PARAMS));
        peerGroup.addWallet(wallet);

        peerGroup.start();

        while (true) {
            Thread.sleep(10000);
            System.out.println(".");
        }
    }

    private static void sendCoins(YkneoBitcoin neo, DeterministicWallet wallet, Address receiver, BigInteger amount) {
        final Wallet.SendResult res = wallet.sendCoins(receiver, amount, neo);
        res.broadcastComplete.addListener(new Runnable() {
            @Override
            public void run() {
                System.out.println("Transaction sent: " + res.tx);
            }
        }, MoreExecutors.sameThreadExecutor());

    }
}
