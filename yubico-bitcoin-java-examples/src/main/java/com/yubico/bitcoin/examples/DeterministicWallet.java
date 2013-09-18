/*
 * Copyright (C) 2013 Yubico AB.
 * All rights reserved.
 * Proprietary code owned by Yubico AB.
 * No rights to modifications or redistribution.
 */

package com.yubico.bitcoin.examples;

import com.google.bitcoin.core.*;
import com.google.bitcoin.crypto.*;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.yubico.bitcoin.api.PinModeLockedException;
import com.yubico.bitcoin.api.UnusableIndexException;
import com.yubico.bitcoin.api.YkneoBitcoin;

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * This is an example using a bitcoinj Wallet together with a YubiKey NEO with a BIP 32 master key.
 * External (public) addresses are derived from the master key using the m/0 chain.
 * Internal (change) addresses are derived from the master key using the m/1 chain.
 *
 * Note that m doesn't need to be the root key, it can be any extended key, as long as it is loaded onto the NEO.
 */
public class DeterministicWallet {
    private static final int LOOKAHEAD_WINDOW = 10;

    private static final ImmutableList<ChildNumber> EXTERNAL_CHAIN = ImmutableList.of(new ChildNumber(0, false));
    private static final ImmutableList<ChildNumber> INTERNAL_CHAIN = ImmutableList.of(new ChildNumber(1, false));

    private transient final Wallet wallet;
    private final DeterministicHierarchy externalKeys;
    private final DeterministicHierarchy internalKeys;

    private int highestExternal = 0;
    private int highestInternal = 0;
    private int nextInternal = 0;

    public DeterministicWallet(Wallet wallet, DeterministicKey master) {
        this.wallet = wallet;

        wallet.addEventListener(new WalletListener());

        //Derive from same master.
        externalKeys = new DeterministicHierarchy(master);
        externalKeys.get(EXTERNAL_CHAIN, true, true);

        internalKeys = new DeterministicHierarchy(master);
        internalKeys.get(INTERNAL_CHAIN, true, true);

        for (int i = 0; i < LOOKAHEAD_WINDOW; i++) {
            createInternal();
            createExternal();
        }

        for(Transaction tx : wallet.getTransactions(true)) {
            seeTransaction(tx);
        }
    }

    private ECKey createInternal() {
        DeterministicKey key = internalKeys.deriveNextChild(INTERNAL_CHAIN, true, false, false);
        highestInternal = Math.max(highestInternal, key.getChildNumber().getChildNumber());
        ECKey ecKey = key.toECKey();
        wallet.addKey(ecKey);
        return ecKey;
    }

    private ECKey createExternal() {
        DeterministicKey key = externalKeys.deriveNextChild(EXTERNAL_CHAIN, true, false, false);
        highestExternal = Math.max(highestExternal, key.getChildNumber().getChildNumber());
        ECKey ecKey = key.toECKey();
        wallet.addKey(ecKey);
        return ecKey;
    }

    private DeterministicKey lookup(ECKey key) {
        List<ChildNumber> path = Lists.newArrayList(EXTERNAL_CHAIN);
        path.add(null);
        for (int i = 0; i < highestExternal; i++) {
            path.set(EXTERNAL_CHAIN.size(), new ChildNumber(i, false));

            DeterministicKey deterministicKey = externalKeys.get(path, true, false);
            if (Arrays.equals(key.getPubKey(), deterministicKey.getPubKeyBytes())) {
                return deterministicKey;
            }
        }

        path = Lists.newArrayList(INTERNAL_CHAIN);
        path.add(null);
        for (int i = 0; i < highestExternal; i++) {
            path.set(INTERNAL_CHAIN.size(), new ChildNumber(i, false));

            DeterministicKey deterministicKey = internalKeys.get(path, true, false);
            if (Arrays.equals(key.getPubKey(), deterministicKey.getPubKeyBytes())) {
                return deterministicKey;
            }
        }

        throw new RuntimeException("Deterministic key not found!");
    }

    private void seeTransaction(Transaction tx) {
        for(TransactionOutput output : tx.getOutputs()) {
            try {
                if(output.isMine(wallet)) {
                    if (output.getScriptPubKey().isSentToRawPubKey()) {
                        updateInternalLookahead(wallet.findKeyFromPubKey(output.getScriptBytes()));
                    } else {
                        updateExternalLookahead(wallet.findKeyFromPubHash(output.getScriptPubKey().getPubKeyHash()));
                    }
                }
            } catch (ScriptException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    private void updateExternalLookahead(ECKey key) {
        List<ChildNumber> path = Lists.newArrayList(EXTERNAL_CHAIN);
        path.add(null);
        for (int i = 0; i < highestExternal; i++) {
            path.set(EXTERNAL_CHAIN.size(), new ChildNumber(i, false));

            if (Arrays.equals(key.getPubKey(), externalKeys.get(path, true, false).getPubKeyBytes())) {
                int newHighest = i + LOOKAHEAD_WINDOW;
                while (highestExternal < newHighest) {
                    createExternal();
                }
                return;
            }
        }
    }

    private void updateInternalLookahead(ECKey key) {
        List<ChildNumber> path = Lists.newArrayList(INTERNAL_CHAIN);
        path.add(null);
        for (int i = 0; i < highestInternal; i++) {
            path.set(INTERNAL_CHAIN.size(), new ChildNumber(i, false));

            if (Arrays.equals(key.getPubKey(), internalKeys.get(path, true, false).getPubKeyBytes())) {
                nextInternal = Math.max(nextInternal, i+1);
                int newHighest = i + LOOKAHEAD_WINDOW;
                while (highestInternal < newHighest) {
                    createInternal();
                }
                return;
            }
        }
    }

    /**
     * Gets the next change address from the internal chain.
     * @return
     */
    public Address getChangeAddress() {
        List<ChildNumber> path = Lists.newArrayList(INTERNAL_CHAIN);
        path.add(new ChildNumber(nextInternal++, false));
        return internalKeys.get(path, true, false).toECKey().toAddress(wallet.getParams());
    }

    public Wallet.SendResult send(Wallet.SendRequest request) {
        return wallet.sendCoins(request);
    }

    public Wallet.SendRequest prepareSendRequest(Address address, BigInteger amount, YkneoBitcoin neo) {
        Wallet.SendRequest req = Wallet.SendRequest.to(address, amount);
        req.changeAddress = getChangeAddress();
        if(wallet.completeTx(req)) {
            int index = 0;
            for(TransactionInput input : req.tx.getInputs()) {
                try {
                    ECKey key = input.getOutpoint().getConnectedKey(wallet);
                    Script scriptPubKey = input.getOutpoint().getConnectedOutput().getScriptPubKey();

                    Sha256Hash hash = req.tx.hashForSignature(index, scriptPubKey, Transaction.SigHash.ALL, false);

                    int[] path = Ints.toArray(Lists.newArrayList(Iterables.transform(lookup(key).getChildNumberPath(), new Function<ChildNumber, Integer>() {
                        @Override
                        public Integer apply( ChildNumber input) {
                            return input.getChildNumber();
                        }
                    })));
                    TransactionSignature sig = new TransactionSignature(ECKey.ECDSASignature.decodeFromDER(neo.sign(hash.getBytes(), path)), Transaction.SigHash.ALL, false);

                    if (scriptPubKey.isSentToAddress()) {
                        input.setScriptSig(ScriptBuilder.createInputScript(sig, key));
                    } else if (scriptPubKey.isSentToRawPubKey()) {
                        input.setScriptSig(ScriptBuilder.createInputScript(sig));
                    } else {
                        // Should be unreachable - if we don't recognize the type of script we're trying to sign for, we should
                        // have failed above when fetching the key to sign with.
                        throw new RuntimeException("Do not understand script type: " + scriptPubKey);
                    }
                } catch (ScriptException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    throw new RuntimeException("Unable to complete transaction!");
                } catch (PinModeLockedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    throw new RuntimeException("Unable to complete transaction!");
                } catch (UnusableIndexException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    throw new RuntimeException("Unable to complete transaction!");
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    throw new RuntimeException("Unable to complete transaction!");
                }
            }
        }

        return req;
    }

    private class WalletListener extends AbstractWalletEventListener {
        @Override
        public void onCoinsReceived(Wallet wallet, Transaction tx, BigInteger prevBalance, BigInteger newBalance) {
            seeTransaction(tx);
        }
    }
}