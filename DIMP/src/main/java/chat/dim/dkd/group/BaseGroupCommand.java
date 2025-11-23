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
package chat.dim.dkd.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import chat.dim.dkd.cmd.BaseHistoryCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.group.GroupCommand;

/**
 *  Group History
 *
 *  <blockquote><pre>
 *  data format: {
 *      "type" : i2s(0x89),
 *      "sn"   : 123,
 *
 *      "command" : "reset",   // "invite", "quit", "query", ...
 *      "time"    : 123.456,   // command timestamp
 *
 *      "group"   : "{GROUP_ID}",
 *      "members" : ["{MEMBER_ID}",]
 *  }
 *  </pre></blockquote>
 */
public class BaseGroupCommand extends BaseHistoryCommand implements GroupCommand {

    public BaseGroupCommand(Map<String, Object> content) {
        super(content);
    }

    /**
     *  Group history command: {
     *      "type" : i2s(0x89),
     *      "sn"   : 123,
     *
     *      "command" : "join",      // or quit
     *      "group"   : "{GROUP_ID}",
     *  }
     */
    public BaseGroupCommand(String cmd, ID group) {
        super(cmd);
        setGroup(group);
    }

    /**
     *  Group history command: {
     *      "type" : i2s(0x89),
     *      "sn"   : 123,
     *
     *      "command" : "invite",      // or expel
     *      "group"   : "{GROUP_ID}",
     *      "members" : ["{MEMBER_ID}", ],
     *  }
     */
    public BaseGroupCommand(String cmd, ID group, List<ID> members) {
        super(cmd);
        setGroup(group);
        setMembers(members);
    }

    @Override
    public List<ID> getMembers() {
        Object members = get("members");
        if (members instanceof List) {
            return ID.convert((List<?>) members);
        }
        // get from 'member'
        ID single = ID.parse(get("member"));
        if (single != null) {
            List<ID> array = new ArrayList<>();
            array.add(single);
            return array;
        }
        assert false : "failed to get group members";
        return null;
    }

    @Override
    public void setMembers(List<ID> members) {
        if (members != null) {
            put("members", ID.revert(members));
        } else {
            remove("members");
        }
        remove("member");
    }
}
