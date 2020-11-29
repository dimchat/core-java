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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import chat.dim.protocol.group.ExpelCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.JoinCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.QuitCommand;
import chat.dim.protocol.group.ResetCommand;

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

    public static List<ID> convertList(List<String> members) {
        List<ID> array = new ArrayList<>();
        ID id;
        for (String item : members) {
            id = ID.parse(item);
            if (id == null) {
                continue;
            }
            array.add(id);
        }
        return array;
    }

    public static List<String> revertList(List<ID> members) {
        List<String> array = new ArrayList<>();
        for (ID item : members) {
            array.add(item.toString());
        }
        return array;
    }

    private ID member = null;
    private List<ID> members = null;

    protected GroupCommand(Map<String, Object> dictionary) {
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
    public GroupCommand(String command, ID groupID, List<ID> memberList) {
        super(command);
        setGroup(groupID);
        setMembers(memberList);
    }

    /*
     *  Member ID (or String)
     *
     */
    public ID getMember() {
        if (member == null) {
            member = ID.parse(get("member"));
        }
        return member;
    }

    public void setMember(ID member) {
        if (member == null) {
            remove("member");
        } else {
            put("member", member.toString());
        }
        this.member = member;
    }

    /*
     *  Member ID (or String) list
     *
     */
    @SuppressWarnings("unchecked")
    public List<ID> getMembers() {
        if (members == null) {
            Object array = get("members");
            if (array == null) {
                // TODO: get from 'member'?
                return null;
            } else {
                members = convertList((List<String>) array);
            }
        }
        return members;
    }

    public void setMembers(List<ID> members) {
        if (members == null) {
            remove("members");
        } else {
            put("members", revertList(members));
        }
        // TODO: remove 'member'?
        this.members = members;
    }

    /**
     *  Command Parser
     *  ~~~~~~~~~~~~~~
     */
    public static class Parser {

        /**
         *  Parse map object to command
         *
         * @param cmd - command info
         * @return Command
         */
        public GroupCommand parseCommand(Map<String, Object> cmd) {
            String command = (String) cmd.get("command");

            if (command.equals(INVITE)) {
                return new InviteCommand(cmd);
            }
            if (command.equals(EXPEL)) {
                return new ExpelCommand(cmd);
            }

            if (command.equals(JOIN)) {
                return new JoinCommand(cmd);
            }
            if (command.equals(QUIT)) {
                return new QuitCommand(cmd);
            }

            if (command.equals(RESET)) {
                return new ResetCommand(cmd);
            }
            if (command.equals(QUERY)) {
                return new QueryCommand(cmd);
            }

            return new GroupCommand(cmd);
        }
    }

    // default parser
    public static Parser parser = new Parser();

    public static GroupCommand parseCommand(Map<String, Object> cmd) {
        return parser.parseCommand(cmd);
    }
}
