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
package chat.dim.mkm;

import java.util.List;

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
 *      document   - visa for user, or bulletin for group
 */
public interface Entity {

    /**
     *  Get entity ID
     *
     * @return ID
     */
    ID getIdentifier();

    /**
     *  Get ID.type
     *
     * @return network type
     */
    int getType();

    void setDataSource(DataSource dataSource);
    DataSource getDataSource();

    Meta getMeta();
    List<Document> getDocuments();

    /**
     *  Entity Data Source
     *  ~~~~~~~~~~~~~~~~~~
     *
     *      1. meta for user, which is generated by the user's private key
     *      2. meta for group, which is generated by the founder's private key
     *      3. meta key, which can verify message sent by this user(or group founder)
     *      4. visa key, which can encrypt message for the receiver(user)
     */
    interface DataSource {

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
         * @return Document
         */
        List<Document> getDocuments(ID identifier);
    }

    /**
     *  Entity Delegate
     *  ~~~~~~~~~~~~~~~
     */
    interface Delegate {

        /**
         *  Create user with ID
         *
         * @param identifier - user ID
         * @return user
         */
        User getUser(ID identifier);

        /**
         *  Create group with ID
         *
         * @param identifier - group ID
         * @return group
         */
        Group getGroup(ID identifier);
    }
}
