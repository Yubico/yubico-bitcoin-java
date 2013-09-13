/*
 * Copyright (C) 2013 Yubico AB.
 * All rights reserved.
 * Proprietary code owned by Yubico AB.
 * No rights to modifications or redistribution.
 */

package com.yubico.bitcoin.api;

import java.io.IOException;

/**
 * Interface to the ykneo-bitcoin applet running on a YubiKey NEO.
 * <p/>
 * Keys used are in the <a href="https://en.bitcoin.it/wiki/BIP_0032">BIP 32</a> format.
 * A single extended key pair is stored on the YubiKey NEO, which is used to derive sub keys used for signing.
 * <p/>
 * Though BIP 32 supports a tree hierarchy of keys, ykneo-bitcoin only supports a flat hierarchy (though the stored
 * extended key pair does not need to be a root node). The getPublicKey and sign methods work on the sub keys m/i, where
 * m is the stored extended key pair and i is the index used in the methods. It is thus possible to get public keys and
 * sign data using private keys from the sub keys known as m/0...n where n is a 32 bit integer. Private key derivation
 * is supported, by setting the first (sign-) bit of index, as per the BIP 32 specification.
 * <p/>
 * Example:
 * YkneoBitcoin neo = ...
 * byte[] extendedPrivateMasterKey m = ...
 * neo.importExtendedKeyPair(m, false).get(); //neo now holds the master key pair m.
 * <p/>
 * neo.getPublicKey(0); //This returns the uncompressed public key from sub key m/0
 * int index = 4711;
 * index |= 0x80000000
 * neo.sign(index, hash); //This returns the signature of hash signed by m/4711'
 */
public interface YkneoBitcoin {
    /**
     * Gets the version of the ykneo-bitcoin applet that is loaded on the YubiKey NEO as a 3 byte array.
     * The contained bytes represent the major, minor and micro versions of the applet.
     *
     * @return The ykneo-bitcoin applet version.
     */
    byte[] getAppletVersion();

    /**
     * Unlocks user mode of operation. If the incorrect PIN is given too many times, the mode will be locked.
     *
     * @param pin The PIN code to unlock user mode.
     * @throws IncorrectPINException
     * @throws IOException
     */
    void unlockUser(String pin) throws IncorrectPINException, IOException;

    /**
     * Unlocks admin mode of operation. If the incorrect PIN is given too many times, the mode will be locked.
     *
     * @param pin The PIN code to unlock admin mode.
     * @throws IncorrectPINException
     * @throws IOException
     */
    void unlockAdmin(String pin) throws IncorrectPINException, IOException;

    /**
     * Check to see if user mode is unlocked.
     *
     * @return True if user mode is unlocked, false if not.
     */
    boolean isUserUnlocked();

    /**
     * Check to see if admin mode is unlocked.
     *
     * @return True if admin mode is unlocked, false if not.
     */
    boolean isAdminUnlocked();

    /**
     * Changes the user PIN. Does not require user mode to be unlocked.
     * After successfully setting the PIN, the mode will be locked.
     *
     * @param oldPin The current user PIN.
     * @param newPin The new user PIN to set.
     * @throws IncorrectPINException
     * @throws IOException
     */
    void setUserPin(String oldPin, String newPin) throws IncorrectPINException, IOException;

    /**
     * Changes the admin PIN. Does not require admin mode to be unlocked.
     * After successfully setting the PIN, the mode will be locked.
     *
     * @param oldPin The current admin PIN,
     * @param newPin The new admin PIN to set.
     * @throws IncorrectPINException
     * @throws IOException
     */
    void setAdminPin(String oldPin, String newPin) throws IncorrectPINException, IOException;

    /**
     * Re-sets and unblocks the user PIN. Can be used if the user PIN is lost.
     * Requires admin mode to be unlocked.
     *
     * @param newPin The new user PIN to set.
     * @throws PinModeLockedException
     * @throws IOException
     */
    void resetUserPin(String newPin) throws PinModeLockedException, IOException;

    /**
     * Gets the 13 byte BIP 32 key header for the stored extended private key.
     *
     * @return version(4) | depth(4) | parents fingerprint(4) | child number(4)
     * @throws PinModeLockedException
     * @throws IOException
     * @throws NoKeyLoadedException
     */
    byte[] getHeader() throws PinModeLockedException, IOException, NoKeyLoadedException;

    /**
     * Gets the public key obtained by deriving a sub key from the master key pair using the given index.
     * Requires user mode to be unlocked.
     *
     * @param compress True to return a compressed public key, false to return the uncompressed public key.
     * @param index    The index of the derived sub key to get.
     * @return A 65 (uncompressed) or 33 (compressed) byte public key.
     * @throws PinModeLockedException
     * @throws UnusableIndexException
     * @throws IOException
     * @throws NoKeyLoadedException
     */
    byte[] getPublicKey(boolean compress, int... index) throws PinModeLockedException, UnusableIndexException, IOException, NoKeyLoadedException;

    /**
     * Signs the given hash using the private key obtained by deriving a sub key from the master key pair using the given index.
     * Requires user mode to be unlocked.
     *
     * @param hash  The 32 byte hash to sign.
     * @param index The index of the derived sub key to sign with.
     * @return A digital signature.
     * @throws PinModeLockedException
     * @throws UnusableIndexException
     * @throws IOException
     * @throws NoKeyLoadedException
     */
    byte[] sign(byte[] hash, int... index) throws PinModeLockedException, UnusableIndexException, IOException, NoKeyLoadedException;

    /**
     * Generates a new master key pair randomly, overwriting any existing key pair stored on the device.
     * The allowExport flag determines if the extended public key can later be exported or not.
     * The returnPrivateKey flag determines if the generated key should be returned from the device (for backup purposes) or not.
     * Requires admin mode to be unlocked.
     *
     * @param allowExport      Sets the allowExport flag permitting the extended public key to be exported.
     * @param returnPrivateKey When true, the generated extended private key is returned, when false, an empty byte[] is returned.
     * @param testnetKey       When true, the generated key will be for testnet use.
     * @return A BIP 32 formatted extended private key, if returnPrivateKey is set.
     * @throws PinModeLockedException
     * @throws IOException
     */
    byte[] generateMasterKeyPair(boolean allowExport, boolean returnPrivateKey, boolean testnetKey) throws PinModeLockedException, IOException;

    /**
     * Imports a new extended key pair, overwriting any existing key pair stored on the device.
     * The allowExport flag determines if the extended public key can later be exported or not.
     * Requires admin mode to be unlocked.
     *
     * @param extendedPrivateKey A BIP 32 formatted extended private key to be imported.
     * @param allowExport        Sets the allowExport flag permitting the extended public key to be exported.
     * @throws PinModeLockedException
     * @throws IOException
     */
    void importExtendedKeyPair(byte[] extendedPrivateKey, boolean allowExport) throws PinModeLockedException, IOException;

    /**
     * Exports the stored extended public key which can be used for the creation of read-only wallets.
     * Unless the allowExport flag was set when the key was generated or imported, this method will fail.
     * Requires admin mode to be unlocked.
     *
     * @return A BIP 32 formatted extended public key.
     * @throws PinModeLockedException
     * @throws IOException
     * @throws OperationNotPermittedException
     * @throws NoKeyLoadedException
     */
    byte[] exportExtendedPublicKey() throws PinModeLockedException, IOException, OperationNotPermittedException, NoKeyLoadedException;
}