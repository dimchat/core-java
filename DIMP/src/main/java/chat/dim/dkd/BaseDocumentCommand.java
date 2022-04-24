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
package chat.dim.dkd;

import java.util.Map;

import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command   : "document", // command name
 *      ID        : "{ID}",     // entity ID
 *      meta      : {...},      // only for handshaking with new friend
 *      document  : {...},      // when document is empty, means query for ID
 *      signature : "..."       // old document's signature for querying
 *  }
 */
public class BaseDocumentCommand extends BaseMetaCommand implements DocumentCommand {

    private Document doc;

    public BaseDocumentCommand(Map<String, Object> dictionary) {
        super(dictionary);
        // lazy
        doc = null;
    }

    /**
     *  Send Meta and Document to new friend
     *
     * @param identifier - entity ID
     * @param meta - entity Meta
     * @param doc - entity Document
     */
    public BaseDocumentCommand(ID identifier, Meta meta, Document doc) {
        super(DOCUMENT, identifier, meta);
        // document
        if (doc != null) {
            put("document", doc.toMap());
        }
        this.doc = doc;
    }

    /**
     *  Response Entity Document
     *
     * @param identifier - entity ID
     * @param doc - entity Document
     */
    public BaseDocumentCommand(ID identifier, Document doc) {
        this(identifier, null, doc);
    }

    /**
     *  Query Entity Document
     *
     * @param identifier - entity ID
     */
    public BaseDocumentCommand(ID identifier) {
        this(identifier, null, null);
    }

    /**
     *  Query Entity Document for updating with current signature
     *
     * @param identifier - entity ID
     * @param signature - document signature
     */
    public BaseDocumentCommand(ID identifier, String signature) {
        this(identifier, null, null);
        // signature
        if (signature != null) {
            put("signature", signature);
        }
    }

    @Override
    public Document getDocument() {
        if (doc == null) {
            Object info = get("document");
            doc = Document.parse(info);
        }
        return doc;
    }

    @Override
    public String getSignature() {
        return (String) get("signature");
    }
}
