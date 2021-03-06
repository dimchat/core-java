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
package chat.dim.protocol;

import java.util.Map;

import chat.dim.dkd.BaseContent;

/**
 *  Money message: {
 *      type : 0x40,
 *      sn   : 123,
 *
 *      currency : "RMB", // USD, USDT, ...
 *      amount   : 100.00
 *  }
 */
public class MoneyContent extends BaseContent {

    public MoneyContent(Map<String, Object> dictionary) {
        super(dictionary);
    }

    protected MoneyContent(ContentType type, String currency, double amount) {
        this(type.value, currency, amount);
    }
    protected MoneyContent(int type, String currency, double amount) {
        super(type);
        setCurrency(currency);
        setAmount(amount);
    }

    public MoneyContent(String currency, double amount) {
        this(ContentType.MONEY, currency, amount);
    }

    //-------- setters/getters --------

    private void setCurrency(String currency) {
        put("currency", currency);
    }

    public String getCurrency() {
        return (String) get("currency");
    }

    public void setAmount(double amount) {
        put("amount", amount);
    }

    // Returns the amount of money as a double, which may involve rounding.
    public double getAmount() {
        Object amount = get("amount");
        if (amount == null) {
            throw new NullPointerException("amount of money not found: " + getMap());
        }
        return ((Number) amount).doubleValue();
    }
}
