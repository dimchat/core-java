/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim.mkm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import chat.dim.protocol.Bulletin;
import chat.dim.protocol.ID;

public class BaseBulletin extends BaseDocument implements Bulletin {

    private List<ID> assistants = null;

    public BaseBulletin(Map<String, Object> dictionary) {
        super(dictionary);
    }

    public BaseBulletin(ID identifier, String data, String signature) {
        super(identifier, data, signature);
    }

    public BaseBulletin(ID identifier) {
        super(identifier, BULLETIN);
    }

    @Override
    public ID getFounder() {
        return ID.parse(getProperty("founder"));
    }

    /**
     *  Group bots for split and distribute group messages
     *
     * @return bot ID list
     */
    @Override
    public List<ID> getAssistants() {
        List<ID> bots = assistants;
        if (bots == null) {
            Object value = getProperty("assistants");
            if (value instanceof List) {
                assistants = bots = ID.convert((List<?>) value);
            } else {
                // get from 'assistant'
                bots = new ArrayList<>();
                ID single = ID.parse(get("assistant"));
                if (single != null) {
                    bots.add(single);
                }
                assistants = bots;
            }
        }
        return bots;
    }

    @Override
    public void setAssistants(List<ID> bots) {
        setProperty("assistants", bots == null ? null : ID.revert(bots));
        setProperty("assistant", null);
        assistants = bots;
    }
}
