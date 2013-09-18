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

import java.util.concurrent.ExecutionException;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 11:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class UnusableIndexException extends YkneoBitcoinException {
    private final int index;

    public UnusableIndexException(int index) {
        super(String.format("The index: %d cannot be used with this extended key pair as it results in an invalid sub key", index));
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
