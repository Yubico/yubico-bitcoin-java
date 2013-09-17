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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.yubico.bitcoin.api.PinModeLockedException;
import com.yubico.bitcoin.api.UnusableIndexException;
import com.yubico.bitcoin.api.YkneoBitcoin;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/16/13
 * Time: 9:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class DeterministicWallet implements BlockChainListener, Serializable {
    private static final long serialVersionUID = -2797668513289160303L;
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

        //Derive from same master.
        externalKeys = new DeterministicHierarchy(master);
        externalKeys.get(EXTERNAL_CHAIN, true, true);

        internalKeys = new DeterministicHierarchy(master);
        internalKeys.get(INTERNAL_CHAIN, true, true);

        for (int i = 0; i < LOOKAHEAD_WINDOW; i++) {
            createInternal();
            createExternal();
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
        for (int i = 0; i < highestExternal; i++) {
            path.set(INTERNAL_CHAIN.size(), new ChildNumber(i, false));

            DeterministicKey deterministicKey = internalKeys.get(path, true, false);
            if (Arrays.equals(key.getPubKey(), deterministicKey.getPubKeyBytes())) {
                return deterministicKey;
            }
        }

        throw new RuntimeException("Deterministic key not found!");
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

    @Override
    public void receiveFromBlock(Transaction tx, StoredBlock block, AbstractBlockChain.NewBlockType blockType) throws VerificationException {
        for (TransactionOutput out : tx.getOutputs()) {
            if (out.isMine(wallet)) {
                if (out.getScriptPubKey().isSentToRawPubKey()) {
                    updateInternalLookahead(wallet.findKeyFromPubKey(out.getScriptBytes()));
                } else {
                    updateExternalLookahead(wallet.findKeyFromPubHash(out.getScriptPubKey().getPubKeyHash()));
                }
            }
        }
    }

    @Override
    public boolean isTransactionRelevant(Transaction tx) throws ScriptException {
        return wallet.isTransactionRelevant(tx);
    }

    @Override
    public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void reorganize(StoredBlock splitPoint, List<StoredBlock> oldBlocks, List<StoredBlock> newBlocks) throws VerificationException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyTransactionIsInBlock(Sha256Hash txHash, StoredBlock block, AbstractBlockChain.NewBlockType blockType) throws VerificationException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Address getChangeAddress() {
        List<ChildNumber> path = Lists.newArrayList(INTERNAL_CHAIN);
        path.add(new ChildNumber(nextInternal++, false));
        return internalKeys.get(path, true, false).toECKey().toAddress(wallet.getParams());
    }

    public Transaction sendCoins(Address address, BigInteger amount, YkneoBitcoin neo) {
        Wallet.SendRequest req = Wallet.SendRequest.to(address, amount);
        req.changeAddress = getChangeAddress();
        if(wallet.completeTx(req)) {
            System.out.println("Transaction created: "+req.tx);
            //Sign
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
                } catch (PinModeLockedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (UnusableIndexException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }

        return req.tx;
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }
}
