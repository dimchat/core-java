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
package chat.dim.protocol;

import java.util.Date;

import chat.dim.dkd.cmd.BaseDocumentCommand;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command   : "document", // command name
 *      did       : "{ID}",     // entity ID
 *      meta      : {...},      // only for handshaking with new friend
 *      document  : {...},      // when document is empty, means query for ID
 *      last_time : 12345       // old document time for querying
 *  }
 */
public interface DocumentCommand extends MetaCommand {

    /*
     *  Entity Document
     *
     */
    Document getDocument();

    /**
     *  Last document time for querying
     *
     * @return time of last document from sender
     */
    Date getLastTime();

    //
    //  Factories
    //

    static DocumentCommand query(ID identifier) {
        return new BaseDocumentCommand(identifier);
    }
    static DocumentCommand query(ID identifier, Date last) {
        return new BaseDocumentCommand(identifier, last);
    }

    static DocumentCommand response(ID identifier, Document doc) {
        return new BaseDocumentCommand(identifier, doc);
    }
    static DocumentCommand response(ID identifier, Meta meta, Document doc) {
        return new BaseDocumentCommand(identifier, meta, doc);
    }
}
