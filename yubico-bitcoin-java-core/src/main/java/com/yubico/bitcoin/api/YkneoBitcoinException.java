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

package com.yubico.bitcoin.api;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/13/13
 * Time: 11:54 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class YkneoBitcoinException extends Exception {
    public YkneoBitcoinException(String message) {
        super(message);
    }

    public YkneoBitcoinException(String message, Throwable cause) {
        super(message, cause);
    }
}
