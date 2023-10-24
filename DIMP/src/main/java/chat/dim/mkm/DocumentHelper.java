/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2023 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Albert Moky
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

import java.util.Date;
import java.util.List;

import chat.dim.protocol.Bulletin;
import chat.dim.protocol.Document;
import chat.dim.protocol.Visa;

public interface DocumentHelper {

    /**
     *  Check whether new time is before old time
     */
    static boolean isBefore(Date oldTime, Date newTime) {
        if (oldTime == null || newTime == null) {
            return false;
        }
        return newTime.before(oldTime);
    }

    /**
     *  Check whether this document's time is before old document's time
     */
    static boolean isExpired(Document thisDoc, Document oldDoc) {
        return isBefore(oldDoc.getTime(), thisDoc.getTime());
    }

    /**
     *  Select last document matched the type
     */
    static Document lastDocument(List<Document> documents, String type) {
        if (documents == null || documents.isEmpty()) {
            return null;
        } else if (type == null || type.equals("*")) {
            type = "";
        }
        boolean checkType = type.length() > 0;

        Document last = null;
        String docType;
        boolean matched;
        for (Document doc : documents) {
            // 1. check type
            if (checkType) {
                docType = doc.getType();
                matched = docType == null || docType.isEmpty() || docType.equals(type);
                if (!matched) {
                    continue;
                }
            }
            // 2. check time
            if (last != null) {
                if (isExpired(doc, last)) {
                    continue;
                }
            }
            // got it
            last = doc;
        }
        return last;
    }

    /**
     *  Select last visa document
     */
    static Visa lastVisa(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return null;
        }
        Visa last = null;
        boolean matched;
        for (Document doc : documents) {
            // 1. check type
            matched = doc instanceof Visa;
            if (!matched) {
                continue;
            }
            // 2. check time
            if (last != null) {
                if (isExpired(doc, last)) {
                    continue;
                }
            }
            // got it
            last = (Visa) doc;
        }
        return last;
    }

    /**
     *  Select last bulletin document
     */
    static Bulletin lastBulletin(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return null;
        }
        Bulletin last = null;
        boolean matched;
        for (Document doc : documents) {
            // 1. check type
            matched = doc instanceof Bulletin;
            if (!matched) {
                continue;
            }
            // 2. check time
            if (last != null) {
                if (isExpired(doc, last)) {
                    continue;
                }
            }
            // got it
            last = (Bulletin) doc;
        }
        return last;
    }

}
