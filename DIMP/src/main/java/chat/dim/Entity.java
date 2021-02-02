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

import java.lang.ref.WeakReference;

import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

/**
 *  Entity (User/Group)
 *  ~~~~~~~~~~~~~~~~~~~
 *  Base class of User and Group, ...
 *
 *  properties:
 *      identifier - entity ID
 *      type       - entity type
 *      meta       - meta for generate ID
 *      document   - entity document
 */
public class Entity {

    public final ID identifier;

    private WeakReference<DataSource> dataSourceRef = null;

    public Entity(ID identifier) {
        super();
        this.identifier = identifier;
    }

    @Override
    public boolean equals(Object other) {
        if (super.equals(other)) {
            // same object
            return true;
        } else if (other instanceof Entity) {
            // check with identifier
            Entity entity = (Entity) other;
            return identifier.equals(entity.identifier);
        } else {
            // null or unknown object
            return false;
        }
    }

    @Override
    public String toString() {
        String clazzName = getClass().getSimpleName();
        return "<" + clazzName + "|" + getType() + " " + identifier + ">";
    }

    /**
     *  Get ID.type
     *
     * @return network type
     */
    public byte getType() {
        return identifier.getType();
    }

    public DataSource getDataSource() {
        if (dataSourceRef == null) {
            return null;
        }
        return dataSourceRef.get();
    }

    public void setDataSource(DataSource dataSource) {
        dataSourceRef = new WeakReference<>(dataSource);
    }

    public Meta getMeta() {
        return getDataSource().getMeta(identifier);
    }

    public Document getDocument(String type) {
        return getDataSource().getDocument(identifier, type);
    }

    /**
     *  Entity Data Source
     *  ~~~~~~~~~~~~~~~~~~
     */
    public interface DataSource {

        /**
         *  Get meta for entity ID
         *
         * @param identifier - entity ID
         * @return meta object
         */
        Meta getMeta(ID identifier);

        /**
         *  Get document for entity ID
         *
         * @param identifier - entity ID
         * @param type - document type
         * @return Document
         */
        Document getDocument(ID identifier, String type);
    }
}
