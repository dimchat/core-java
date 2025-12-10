/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
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
package chat.dim.dkd.cmd;

import java.util.Date;
import java.util.List;
import java.util.Map;

import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

/**
 *  Document Command Content
 *
 *  <blockquote><pre>
 *  data format: {
 *      'type' : i2s(0x88),
 *      'sn'   : 123,
 *
 *      'command'   : "documents", // command name
 *      'did'       : "{ID}",      // entity ID
 *      'meta'      : {...},       // only for handshaking with new friend
 *      'documents' : [...],       // when this is null, means to query
 *      'last_time' : 12345        // old document time for querying
 *  }
 *  </pre></blockquote>
 */
public class BaseDocumentCommand extends BaseMetaCommand implements DocumentCommand {

    private List<Document> docs;

    public BaseDocumentCommand(Map<String, Object> content) {
        super(content);
        // lazy
        docs = null;
    }

    /**
     *  Send Meta and Documents to new friend
     *
     * @param did       - entity ID
     * @param meta      - entity Meta
     * @param documents - entity Documents
     */
    public BaseDocumentCommand(ID did, Meta meta, List<Document> documents) {
        super(DOCUMENTS, did, meta);
        // documents
        if (documents != null) {
            put("documents", Document.revert(documents));
        }
        docs = documents;
    }

    /**
     *  Query Entity Document for updating with current signature
     *
     * @param did  - entity ID
     * @param last - last document time
     */
    public BaseDocumentCommand(ID did, Date last) {
        super(DOCUMENTS, did, null);
        // documents
        docs = null;
        // signature
        if (last != null) {
            setDateTime("last_time", last);
        }
    }

    @Override
    public List<Document> getDocuments() {
        if (docs == null) {
            Object documents = get("documents");
            if (documents instanceof List) {
                docs = Document.convert((Iterable<?>) documents);
            } else {
                assert documents == null : "documents error: " + documents;
            }
        }
        return docs;
    }

    @Override
    public Date getLastTime() {
        return getDateTime("last_time");
    }
}
