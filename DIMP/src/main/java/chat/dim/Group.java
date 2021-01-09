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
package chat.dim;

import java.util.List;

import chat.dim.protocol.Bulletin;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;

public class Group extends Entity {

    private ID founder = null;

    public Group(ID identifier) {
        super(identifier);
    }

    @Override
    public DataSource getDataSource() {
        return (DataSource) super.getDataSource();
    }

    public Bulletin getBulletin() {
        Document doc = getDocument(Document.BULLETIN);
        if (doc instanceof Bulletin) {
            return (Bulletin) doc;
        }
        return null;
    }

    public ID getFounder() {
        if (founder == null) {
            founder = getDataSource().getFounder(identifier);
        }
        return founder;
    }

    public ID getOwner() {
        return getDataSource().getOwner(identifier);
    }

    // NOTICE: the owner must be a member
    //         (usually the first one)
    public List<ID> getMembers() {
        return getDataSource().getMembers(identifier);
    }

    public List<ID> getAssistants() {
        return getDataSource().getAssistants(identifier);
    }

    /**
     *  Group Data Source
     *  ~~~~~~~~~~~~~~~~~
     */
    public interface DataSource extends Entity.DataSource {

        /**
         *  Get group founder
         *
         * @param group - group ID
         * @return fonder ID
         */
        ID getFounder(ID group);

        /**
         *  Get group owner
         *
         * @param group - group ID
         * @return owner ID
         */
        ID getOwner(ID group);

        /**
         *  Get group members list
         *
         * @param group - group ID
         * @return members list (ID)
         */
        List<ID> getMembers(ID group);

        /**
         *  Get assistants for this group
         *
         * @param group - group ID
         * @return robot ID list
         */
        List<ID> getAssistants(ID group);
    }
}
