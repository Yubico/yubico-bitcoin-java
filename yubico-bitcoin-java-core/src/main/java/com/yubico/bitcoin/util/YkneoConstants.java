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

package com.yubico.bitcoin.util;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 12:51 PM
 * To change this template use File | Settings | File Templates.
 */
public interface YkneoConstants {
    public static final byte[] AID = new byte[]{(byte) 0xa0, 0x00, 0x00, 0x05, 0x27, 0x21, 0x02};

    public static final byte INS_GET_PUB = 0x01;
    public static final byte INS_SIGN = 0x02;
    public static final byte INS_GET_HEADER = 0x03;
    public static final byte INS_GENERATE_KEY_PAIR = 0x11;
    public static final byte INS_IMPORT_KEY_PAIR = 0x12;
    public static final byte INS_EXPORT_EXT_PUB_KEY = 0x13;
    public static final byte INS_RESET_USER_PIN = 0x14;
    public static final byte INS_SET_RETRY_COUNT = 0x15;
    public static final byte INS_VERIFY_PIN = 0x21;
    public static final byte INS_SET_PIN = 0x22;

    public static final byte FLAG_CAN_EXPORT = 0x01;
    public static final byte FLAG_RETURN_PRIVATE = 0x02;
    public static final byte FLAG_TESTNET = 0x04;
    public static final byte FLAG_ADMIN_PIN = 0x01;

    public static final byte PIN_MAX_LENGTH = 127;
}
