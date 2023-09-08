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

import chat.dim.protocol.Bulletin;
import chat.dim.protocol.ID;

/**
 *  Group for organizing users
 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *  roles:
 *      founder
 *      owner
 *      members
 *      administrators - Optional
 *      assistants     - group bots
 */
public interface Group extends Entity {

    // group document
    Bulletin getBulletin();

    ID getFounder();

    ID getOwner();

    // NOTICE: the owner must be a member
    //         (usually the first one)
    List<ID> getMembers();

    List<ID> getAssistants();

    /**
     *  Group Data Source
     *  ~~~~~~~~~~~~~~~~~
     *
     *      1. founder has the same public key with the group's meta.key
     *      2. owner and members should be set complying with the consensus algorithm
     */
    interface DataSource extends Entity.DataSource {

        /**
         *  Get founder of the group
         *
         * @param group - group ID
         * @return fonder ID
         */
        ID getFounder(ID group);

        /**
         *  Get current owner of the group
         *
         * @param group - group ID
         * @return owner ID
         */
        ID getOwner(ID group);

        /**
         *  Get all members in the group
         *
         * @param group - group ID
         * @return members list (ID)
         */
        List<ID> getMembers(ID group);

        /**
         *  Get assistants for this group
         *
         * @param group - group ID
         * @return bot ID list
         */
        List<ID> getAssistants(ID group);
    }
}
