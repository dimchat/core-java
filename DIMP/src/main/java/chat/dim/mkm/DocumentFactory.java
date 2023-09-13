/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
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

import java.util.Map;

import chat.dim.protocol.Document;
import chat.dim.protocol.ID;

/**
 *  General Document Factory
 *  ~~~~~~~~~~~~~~~~~~~~~~~~
 */
public final class DocumentFactory implements Document.Factory {

    private final String docType;

    public DocumentFactory(String type) {
        super();
        docType = type;
    }

    private static String getType(String type, ID identifier) {
        if (type.equals("*")) {
            if (identifier.isGroup()) {
                return Document.BULLETIN;
            } else if (identifier.isUser()) {
                return Document.VISA;
            } else {
                return Document.PROFILE;
            }
        } else {
            return type;
        }
    }

    @Override
    public Document createDocument(ID identifier, String data, String signature) {
        String type = getType(docType, identifier);
        if (data == null || signature == null || data.isEmpty() || signature.isEmpty()) {
            // create empty document
            if (Document.VISA.equals(type)) {
                return new BaseVisa(identifier);
            } else if (Document.BULLETIN.equals(type)) {
                return new BaseBulletin(identifier);
            } else {
                return new BaseDocument(identifier, type);
            }
        } else {
            // create document with data & signature from local storage
            if (Document.VISA.equals(type)) {
                return new BaseVisa(identifier, data, signature);
            } else if (Document.BULLETIN.equals(type)) {
                return new BaseBulletin(identifier, data, signature);
            } else {
                return new BaseDocument(identifier, data, signature);
            }
        }
    }

    @Override
    public Document createDocument(ID identifier) {
        String type = getType(docType, identifier);
        if (Document.VISA.equals(type)) {
            return new BaseVisa(identifier);
        } else if (Document.BULLETIN.equals(type)) {
            return new BaseBulletin(identifier);
        } else {
            return new BaseDocument(identifier, type);
        }
    }

    @Override
    public Document parseDocument(Map<String, Object> doc) {
        ID identifier = ID.parse(doc.get("ID"));
        if (identifier == null) {
            // assert false : "document ID not found : " + doc;
            return null;
        }
        FactoryManager man = FactoryManager.getInstance();
        String type = man.generalFactory.getDocumentType(doc, null);
        if (type == null) {
            type = getType("*", identifier);
        }
        if (Document.VISA.equals(type)) {
            return new BaseVisa(doc);
        } else if (Document.BULLETIN.equals(type)) {
            return new BaseBulletin(doc);
        } else {
            return new BaseDocument(doc);
        }
    }
}
