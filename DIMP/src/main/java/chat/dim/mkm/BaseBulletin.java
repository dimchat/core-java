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

    /**
     *  Group bots for split and distribute group messages
     *
     * @return bot ID list
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<ID> getAssistants() {
        if (assistants == null) {
            Object value = getProperty("assistants");
            if (value instanceof List) {
                assistants = ID.convert((List<String>) value);
            }
        }
        return assistants;
    }

    @Override
    public void setAssistants(List<ID> bots) {
        if (bots == null) {
            setProperty("assistants", null);
        } else {
            setProperty("assistants", ID.revert(bots));
        }
        assistants = bots;
    }
}
