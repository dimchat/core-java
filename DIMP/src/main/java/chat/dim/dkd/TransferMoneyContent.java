/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.dkd;

import java.util.Map;

import chat.dim.protocol.ContentType;
import chat.dim.protocol.ID;
import chat.dim.protocol.TransferContent;

/**
 *  Transfer money message: {
 *      type : 0x41,
 *      sn   : 123,
 *
 *      currency : "RMB",    // USD, USDT, ...
 *      amount   : 100.00,
 *      remitter : "{FROM}", // sender ID
 *      remittee : "{TO}"    // receiver ID
 *  }
 */
public class TransferMoneyContent extends BaseMoneyContent implements TransferContent {

    public TransferMoneyContent(Map<String, Object> content) {
        super(content);
    }

    public TransferMoneyContent(String currency, Number amount) {
        super(ContentType.TRANSFER.value, currency, amount);
    }

    @Override
    public void setRemitter(ID sender) {
        setString("remitter", sender);
    }

    @Override
    public ID getRemitter() {
        return ID.parse(get("remitter"));
    }

    @Override
    public void setRemittee(ID receiver) {
        setString("remittee", receiver);
    }

    @Override
    public ID getRemittee() {
        return ID.parse(get("remittee"));
    }
}
