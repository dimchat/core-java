/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2023 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Albert Moky
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

import java.util.List;
import java.util.Map;

import chat.dim.dkd.cmd.BaseGroupCommand;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.group.FireCommand;

public class FireGroupCommand extends BaseGroupCommand implements FireCommand {

    public FireGroupCommand(Map<String, Object> content) {
        super(content);
    }

    public FireGroupCommand(ID group, List<ID> administrators, List<ID> assistants) {
        super(GroupCommand.FIRE, group);
        if (administrators != null) {
            put("administrators", ID.revert(administrators));
        }
        if (assistants != null) {
            put("assistants", ID.revert(assistants));
        }
    }

    @Override
    public List<ID> getAdministrators() {
        Object members = get("administrators");
        if (members instanceof List) {
            return ID.convert((List<?>) members);
        } else {
            return null;
        }
    }

    @Override
    public void setAdministrators(List<ID> members) {
        if (members == null) {
            remove("administrators");
        } else {
            put("administrators", ID.revert(members));
        }
    }

    @Override
    public List<ID> getAssistants() {
        Object bots = get("assistants");
        if (bots instanceof List) {
            return ID.convert((List<?>) bots);
        } else {
            return null;
        }
    }

    @Override
    public void setAssistants(List<ID> bots) {
        if (bots == null) {
            remove("assistants");
        } else {
            put("assistants", ID.revert(bots));
        }
    }
}
