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
package chat.dim.protocol.group;

import java.util.List;

import chat.dim.dkd.group.BaseGroupCommand;
import chat.dim.dkd.group.ExpelGroupCommand;
import chat.dim.dkd.group.FireGroupCommand;
import chat.dim.dkd.group.HireGroupCommand;
import chat.dim.dkd.group.InviteGroupCommand;
import chat.dim.dkd.group.JoinGroupCommand;
import chat.dim.dkd.group.QuitGroupCommand;
import chat.dim.dkd.group.ResetGroupCommand;
import chat.dim.dkd.group.ResignGroupCommand;
import chat.dim.protocol.HistoryCommand;
import chat.dim.protocol.ID;

/**
 *  Group History
 *
 *  <blockquote><pre>
 *  data format: {
 *      'type' : i2s(0x89),
 *      'sn'   : 123,
 *
 *      'command' : "reset",   // "invite", "quit", "query", ...
 *      'time'    : 123.456,   // command timestamp
 *
 *      'group'   : "{GROUP_ID}",
 *      'member'  : "{MEMBER_ID}",
 *      'members' : ["{MEMBER_ID}",]
 *  }
 *  </pre></blockquote>
 */
public interface GroupCommand extends HistoryCommand {

    //-------- group command names begin --------
    // founder/owner
    String FOUND    = "found";
    String ABDICATE = "abdicate";
    // member
    String INVITE   = "invite";
    String EXPEL    = "expel";  // Deprecated (use 'reset' instead)
    String JOIN     = "join";
    String QUIT     = "quit";
    //String QUERY  = "query";  // Deprecated
    String RESET    = "reset";
    // administrator
    String HIRE     = "hire";
    String FIRE     = "fire";
    String RESIGN   = "resign";
    //-------- group command names end --------

    /*
     *  Member ID (or String)
     *
     */
    ID getMember();
    void setMember(ID member);

    /*
     *  Member ID (or String) list
     *
     */
    List<ID> getMembers();
    void setMembers(List<ID> members);

    //
    //  Factories
    //

    static GroupCommand create(String cmd, ID group) {
        return new BaseGroupCommand(cmd, group);
    }
    static GroupCommand create(String cmd, ID group, ID member) {
        return new BaseGroupCommand(cmd, group, member);
    }
    static GroupCommand create(String cmd, ID group, List<ID> members) {
        return new BaseGroupCommand(cmd, group, members);
    }

    static InviteCommand invite(ID group, ID member) {
        return new InviteGroupCommand(group, member);
    }
    static InviteCommand invite(ID group, List<ID> members) {
        return new InviteGroupCommand(group, members);
    }

    // Deprecated (use 'reset' instead)
    static ExpelCommand expel(ID group, ID member) {
        return new ExpelGroupCommand(group, member);
    }
    static ExpelCommand expel(ID group, List<ID> members) {
        return new ExpelGroupCommand(group, members);
    }

    static JoinCommand join(ID group) {
        return new JoinGroupCommand(group);
    }

    static QuitCommand quit(ID group) {
        return new QuitGroupCommand(group);
    }

    static ResetCommand reset(ID group, List<ID> members) {
        return new ResetGroupCommand(group, members);
    }

    //  Administrators

    static HireCommand hire(ID group, List<ID> administrators) {
        return new HireGroupCommand(group, administrators);
    }

    static FireCommand fire(ID group, List<ID> administrators) {
        return new FireGroupCommand(group, administrators);
    }

    static ResignCommand resign(ID group) {
        return new ResignGroupCommand(group);
    }
}
