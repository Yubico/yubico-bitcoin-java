package com.yubico.bitcoin.examples;

import com.google.bitcoin.core.*;
import com.google.bitcoin.crypto.ChildNumber;
import com.google.bitcoin.crypto.DeterministicHierarchy;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.discovery.DnsDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.H2FullPrunedBlockStore;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.yubico.bitcoin.api.YkneoBitcoin;
import com.yubico.bitcoin.pcsc.YkneoBitcoinPCSC;
import org.spongycastle.util.encoders.Hex;

import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/13/13
 * Time: 1:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestWallet {
    private final YkneoBitcoin neo;
    private final NetworkParameters params;
    private final File walletFile;
    private final File detWalletFile;
    private final PeerGroup peerGroup;
    private final AbstractBlockChain chain;
    private Wallet wallet;
    private DeterministicWallet detWallet;

    public TestWallet(YkneoBitcoin neo) throws Exception {
        this.neo = neo;

        byte[] masterPubKey = neo.exportExtendedPublicKey();
        byte[] pubkey = new byte[33];
        byte[] chaincode = new byte[32];
        System.arraycopy(masterPubKey, 13, chaincode, 0, 32);
        System.arraycopy(masterPubKey, 45, pubkey, 0, 33);
        params = TestNet3Params.get();
        // Try to read the wallet from storage, create a new one if not possible.
        boolean freshWallet = false;
        walletFile = new File("test.wallet");
        detWalletFile = new File("test.detwallet");
        try {
            wallet = Wallet.loadFromFile(walletFile);
        } catch (UnreadableWalletException e) {
            wallet = new Wallet(params);

            wallet.saveToFile(walletFile);
            freshWallet = true;
        }

        detWallet = new DeterministicWallet(wallet, HDKeyDerivation.createMasterPubKeyFromBytes(pubkey, chaincode));

        System.out.println(wallet);

        wallet.autosaveToFile(walletFile, 500, TimeUnit.MILLISECONDS, null);

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

        chain = new BlockChain(params, wallet, new SPVBlockStore(params, blockChainFile));
        chain.addListener(detWallet);

        peerGroup = new PeerGroup(params, chain);
        peerGroup.setUserAgent("TestWallet", "0.1");
        peerGroup.addPeerDiscovery(new DnsDiscovery(params));
        peerGroup.addWallet(wallet);

        peerGroup.start();
        peerGroup.startBlockChainDownload(new AbstractPeerEventListener() {
            @Override
            public void onPeerConnected(Peer peer, int peerCount) {
                super.onPeerConnected(peer, peerCount);
                System.out.println("Peer connected: "+peer+", peer count: "+peerCount);
            }

            @Override
            public void onPeerDisconnected(Peer peer, int peerCount) {
                super.onPeerDisconnected(peer, peerCount);
                System.out.println("Peer disconnected: " + peer + ", peer count: " + peerCount);
            }

            @Override
            public void onBlocksDownloaded(Peer peer, Block block, int blocksLeft) {
                super.onBlocksDownloaded(peer, block, blocksLeft);
                System.out.println("Blocks downloaded. Peer: " + peer + ", block: " + block + ", blocks left: " + blocksLeft);
            }
        });
    }

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

        TestWallet testWallet = new TestWallet(neo);

        boolean transfered = false;

        while(true) {
            Thread.sleep(10000);
            System.out.println(".");
            if(!transfered && testWallet.wallet.getBalance().equals(BigInteger.valueOf(1990000))) {
                System.out.println("!!! We have the funds, try a transaction...");
                transfered = true;
                neo.unlockUser("000000");
                Transaction send = testWallet.detWallet.sendCoins(new Address(testWallet.wallet.getParams(), "muta47dg6WeYzN9LtFE8hfmxTQ8H4Pqzty"), BigInteger.valueOf(150000), neo);

                System.out.println("Transaction created: "+send);
                final ListenableFuture<Transaction> future = testWallet.peerGroup.broadcastTransaction(send);
                future.addListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            System.out.println("Future done! Transaction: "+future.get());
                        } catch (InterruptedException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (ExecutionException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                }, MoreExecutors.sameThreadExecutor());
            }
        }
    }
}
