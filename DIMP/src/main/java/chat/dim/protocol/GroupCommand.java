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

import java.util.List;
import java.util.Map;

import chat.dim.ID;
import chat.dim.protocol.group.*;

public class GroupCommand extends HistoryCommand {

    //-------- group command names begin --------
    // founder/owner
    public static final String FOUND    = "found";
    public static final String ABDICATE = "abdicate";
    // member
    public static final String INVITE   = "invite";
    public static final String EXPEL    = "expel";
    public static final String JOIN     = "join";
    public static final String QUIT     = "quit";
    public static final String QUERY    = "query";
    public static final String RESET    = "reset";
    // administrator/assistant
    public static final String HIRE     = "hire";
    public static final String FIRE     = "fire";
    public static final String RESIGN   = "resign";
    //-------- group command names end --------

    @SuppressWarnings("unchecked")
    public GroupCommand(Map<String, Object> dictionary) {
        super(dictionary);
    }

    /*
     *  Group history command: {
     *      type : 0x89,
     *      sn   : 123,
     *
     *      command : "join",      // or quit
     *      group   : "{GROUP_ID}",
     *  }
     */
    public GroupCommand(String command, ID group) {
        super(command);
        setGroup(group);
    }

    /*
     *  Group history command: {
     *      type : 0x89,
     *      sn   : 123,
     *
     *      command : "invite",      // or expel
     *      group   : "{GROUP_ID}",
     *      member  : "{MEMBER_ID}",
     *  }
     */
    public GroupCommand(String command, ID group, ID member) {
        super(command);
        setGroup(group);
        setMember(member);
    }

    /*
     *  Group history command: {
     *      type : 0x89,
     *      sn   : 123,
     *
     *      command : "invite",      // or expel
     *      group   : "{GROUP_ID}",
     *      members : ["{MEMBER_ID}", ],
     *  }
     */
    public GroupCommand(String command, ID groupID, List memberList) {
        super(command);
        setGroup(groupID);
        setMembers(memberList);
    }

    /*
     *  Member ID (or String)
     *
     */
    public Object getMember() {
        return dictionary.get("member");
    }

    public void setMember(Object member) {
        if (member == null) {
            dictionary.remove("member");
        } else {
            dictionary.put("member", member);
        }
    }

    /*
     *  Member ID (or String) list
     *
     */
    public List getMembers() {
        Object members = dictionary.get("members");
        if (members == null) {
            // TODO: get from 'member'?
            return null;
        } else {
            return (List) members;
        }
    }

    public void setMembers(List members) {
        if (members == null) {
            dictionary.remove("members");
        } else {
            dictionary.put("members", members);
        }
        // TODO: remove 'member'?
    }

    //-------- Runtime --------

    @SuppressWarnings("unchecked")
    public static GroupCommand getInstance(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof GroupCommand) {
            // return GroupCommand object directly
            return (GroupCommand) object;
        }
        assert object instanceof Map : "group command error: " + object;
        Map<String, Object> dictionary = (Map<String, Object>) object;
        Class clazz = commandClass(dictionary);
        if (clazz != null) {
            // create instance by subclass (with command name)
            return (GroupCommand) createInstance(clazz, dictionary);
        }
        // custom group command
        return new GroupCommand(dictionary);
    }

    static {
        // Invite member to group
        register(INVITE, InviteCommand.class);
        // Expel member from group
        register(EXPEL, ExpelCommand.class);
        // Join group
        register(JOIN, JoinCommand.class);
        // Quit group
        register(QUIT, QuitCommand.class);
        // Reset group info
        register(RESET, ResetCommand.class);
        // Query group info
        register(QUERY, QueryCommand.class);
        // ...
    }
}
