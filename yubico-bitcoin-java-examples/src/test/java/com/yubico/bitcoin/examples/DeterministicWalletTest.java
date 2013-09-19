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

package com.yubico.bitcoin.examples;

import com.google.bitcoin.core.*;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.discovery.DnsDiscovery;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.yubico.bitcoin.api.YkneoBitcoin;
import com.yubico.bitcoin.pcsc.YkneoBitcoinPCSC;

import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;
import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.List;
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
    private static final File KIT_DIR = new File(".");

    public static void main(String[] argv) throws Exception {
        TerminalFactory factory = TerminalFactory.getDefault();
        CardTerminals terminals = factory.terminals();
        Card card = null;
        for (CardTerminal terminal : terminals.list()) {
            if (terminal.getName().contains("Yubikey NEO")) {
                card = terminal.connect("*");
            }
        }

        final YkneoBitcoin neo = new YkneoBitcoinPCSC(card.getBasicChannel());
        neo.unlockAdmin("00000000");

        byte[] masterPubKey = neo.exportExtendedPublicKey();
        byte[] pubkey = new byte[33];
        byte[] chaincode = new byte[32];
        System.arraycopy(masterPubKey, 13, chaincode, 0, 32);
        System.arraycopy(masterPubKey, 45, pubkey, 0, 33);


        WalletAppKit kit = new WalletAppKit(PARAMS, KIT_DIR, "testkit");

        kit.setAutoSave(true).setBlockingStartup(false).setPeerNodes(new PeerAddress(InetAddress.getByName("seed.bitcoin.sipa.be"), 18333),
                new PeerAddress(InetAddress.getByName("testnet-seed.bitcoin.petertodd.org"), 18333),
                new PeerAddress(InetAddress.getByName("node3.mycelium.com"), 18333),
                new PeerAddress(InetAddress.getLocalHost(), 18333)).startAndWait();

        final DeterministicWallet detWallet = new DeterministicWallet(kit.wallet(), HDKeyDerivation.createMasterPubKeyFromBytes(pubkey, chaincode));
        kit.wallet().addEventListener(new AbstractWalletEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, BigInteger prevBalance, BigInteger newBalance) {
                System.out.println(detWallet);
            }

            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, BigInteger prevBalance, BigInteger newBalance) {
                System.out.println(detWallet);
            }

            @Override
            public void onKeysAdded(Wallet wallet, List<ECKey> keys) {
                System.out.println(detWallet);
            }
        });


        System.out.println(detWallet);

        final Address address = new Address(PARAMS, "moHuXamnKzBLn45tFcZtg9xPvRBq2fgbfk");

        Thread.sleep(5000);

        neo.unlockUser("000000");
        //final Wallet.SendRequest req = detWallet.prepareSendRequest(address, BigInteger.valueOf(1500000), neo);
        //wallet.commitTx(req.tx);
        //peerGroup.broadcastTransaction(req.tx).addListener(new Runnable() {
        //    @Override
        //    public void run() {
        //        System.out.println("Transaction sent: " + req.tx);
        //    }
        //}, MoreExecutors.sameThreadExecutor());
        //sendCoins(neo, detWallet, address, BigInteger.valueOf(1500000), peerGroup);

        while (true) {
            Thread.sleep(10000);
            System.out.println(".");
        }
    }

    private static void sendCoins(YkneoBitcoin neo, DeterministicWallet wallet, Address receiver, BigInteger amount, TransactionBroadcaster broadcaster) {
        final Wallet.SendRequest req = wallet.prepareSendRequest(receiver, amount, neo);

        System.out.println("Preparing to broadcast: " + req.tx);

        broadcaster.broadcastTransaction(req.tx).addListener(new Runnable() {
            @Override
            public void run() {
                System.out.println("Transaction sent: " + req.tx);
            }
        }, MoreExecutors.sameThreadExecutor());

    }
}
