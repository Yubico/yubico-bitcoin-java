package com.yubico.bitcoin.api;

import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 10:43 AM
 * To change this template use File | Settings | File Templates.
 */
public interface YkneoBitcoin {
    /**
     * Unlocks user mode of operation. If the incorrect PIN is given too many times, the mode will be locked.
     *
     * @param pin The PIN code to unlock user mode.
     * @throws IncorrectPINException
     */
    void unlockUser(String pin) throws IncorrectPINException;

    /**
     * Unlocks admin mode of operation. If the incorrect PIN is given too many times, the mode will be locked.
     *
     * @param pin The PIN code to unlock admin mode.
     * @throws IncorrectPINException
     */
    void unlockAdmin(String pin) throws IncorrectPINException;

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
     */
    void setUserPin(String oldPin, String newPin) throws IncorrectPINException;

    /**
     * Changes the admin PIN. Does not require admin mode to be unlocked.
     * After successfully setting the PIN, the mode will be locked.
     * @param oldPin The current admin PIN,
     * @param newPin The new admin PIN to set.
     * @throws IncorrectPINException
     */
    void setAdminPin(String oldPin, String newPin) throws IncorrectPINException;

    /**
     * Re-sets and unblocks the user PIN. Can be used if the user PIN is lost.
     * Requires admin mode to be unlocked.
     * @param newPin The new user PIN to set.
     * @throws PinModeLockedException
     */
    void resetUserPin(String newPin) throws PinModeLockedException;

    /**
     * Gets the public key obtained by deriving a sub key from the master key pair using the given index.
     * This method is asynchronous, yielding the result from the Future.
     * Requires user mode to be unlocked.
     *
     * The resulting Future may throw the following exceptions:
     *   UnusableIndexException
     *   OperationInterruptedException
     *
     * @param index The index of the derived sub key to get.
     * @return A 65 byte public key.
     * @throws PinModeLockedException
     */
    Future<byte[]> getPublicKey(int index) throws PinModeLockedException;

    /**
     * Signs the given hash using the private key obtained by deriving a sub key from the master key pair using the given index.
     * This method is asynchronous, yielding the result from the Future.
     * Requires user mode to be unlocked.
     *
     * The resulting Future may throw the following exceptions:
     *   UnusableIndexException
     *   OperationInterruptedException
     *
     * @param index The index of the derived sub key to sign with.
     * @param hash The 32 byte hash to sign.
     * @return A digital signature.
     * @throws PinModeLockedException
     */
    Future<byte[]> sign(int index, byte[] hash) throws PinModeLockedException;

    /**
     * Generates a new master key pair randomly, overwriting any existing key pair stored on the device.
     * The allowExport flag determines if the extended public key can later be exported or not.
     * The returnPrivateKey flag determines if the generated key should be returned from the device (for backup purposes) or not.
     * Requires admin mode to be unlocked.
     *
     * The resulting Future may throw the following exceptions:
     *   OperationInterruptedException
     *
     * @param allowExport Sets the allowExport flag permitting the extended public key to be exported.
     * @param returnPrivateKey When true, the generated extended private key is returned, when false, an empty byte[] is returned.
     * @return A BIP 32 formatted extended private key, if returnPrivateKey is set.
     * @throws PinModeLockedException
     */
    Future<byte[]> generateMasterKeyPair(boolean allowExport, boolean returnPrivateKey) throws PinModeLockedException;

    /**
     * Imports a new extended key pair, overwriting any existing key pair stored on the device.
     * The allowExport flag determines if the extended public key can later be exported or not.
     * Requires admin mode to be unlocked.
     *
     * The resulting Future may throw the following exceptions:
     *   OperationInterruptedException
     *
     * @param extendedPrivateKey A BIP 32 formatted extended private key to be imported.
     * @param allowExport Sets the allowExport flag permitting the extended public key to be exported.
     * @return Nothing is returned, but an exception may be thrown on failue.
     * @throws PinModeLockedException
     */
    Future<Void> importExtendedKeyPair(byte[] extendedPrivateKey, boolean allowExport) throws PinModeLockedException;

    /**
     * Exports the stored extended public key which can be used for the creation of read-only wallets.
     * Unless the allowExport flag was set when the key was generated or imported, this method will fail.
     * Requires admin mode to be unlocked.
     *
     * The resulting Future may throw the following exceptions:
     *   OperationInterruptedException
     *
     * @return A BIP 32 formatted extended public key.
     * @throws PinModeLockedException
     */
    Future<byte[]> exportExtendedPublicKey() throws PinModeLockedException;
}