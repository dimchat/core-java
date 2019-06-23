/* license: https://mit-license.org
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

import chat.dim.mkm.entity.ID;
import chat.dim.protocol.HistoryCommand;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupCommand extends HistoryCommand {

    // Group ID for group message already defined in DKD.MessageContent
    @Override
    public ID getGroup() {
        Object group = super.getGroup();
        try {
            return ID.getInstance(group);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public final ID member;
    public final List<ID> members;

    @SuppressWarnings("unchecked")
    public GroupCommand(Map<String, Object> dictionary) throws ClassNotFoundException {
        super(dictionary);
        Object object = dictionary.get("member");
        if (object == null) {
            member  = null;
            List<Object> array = (List<Object>) dictionary.get("members");
            if (array == null) {
                members = null;
            } else {
                // transform String to ID
                members = new ArrayList<>(array.size());
                for (Object item: array) {
                    members.add(ID.getInstance(item));
                }
            }
        } else {
            member  = ID.getInstance(object);
            members = null;
        }
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
    public GroupCommand(String command, ID groupID) {
        super(command);
        setGroup(groupID);
        member  = null;
        members = null;
        dictionary.put("group", groupID);
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
    public GroupCommand(String command, ID groupID, ID memberID) {
        super(command);
        setGroup(groupID);
        member  = memberID;
        members = null;

        dictionary.put("group", groupID);
        dictionary.put("member", memberID);
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
        member  = null;
        members = memberList;
        dictionary.put("members", memberList);
        dictionary.put("group", groupID);
    }

    //-------- Runtime --------

    private static Map<String, Class> groupCommandClasses = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static void register(String command, Class clazz) {
        // check whether clazz is subclass of GroupCommand
        if (clazz.equals(GroupCommand.class)) {
            throw new IllegalArgumentException("should not add GroupCommand.class itself!");
        }
        clazz = clazz.asSubclass(GroupCommand.class);
        groupCommandClasses.put(command, clazz);
    }

    @SuppressWarnings("unchecked")
    private static GroupCommand createInstance(Map<String, Object> dictionary)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        String command = (String) dictionary.get("command");
        Class clazz = groupCommandClasses.get(command);
        if (clazz == null) {
            //throw new ClassNotFoundException("unknown group command: " + command);
            try {
                return new GroupCommand(dictionary);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
        // try 'getInstance()'
        try {
            Method method = clazz.getMethod("getInstance", Object.class);
            if (method.getDeclaringClass().equals(clazz)) {
                return (GroupCommand) method.invoke(null, dictionary);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        Constructor constructor = clazz.getConstructor(Map.class);
        return (GroupCommand) constructor.newInstance(dictionary);
    }

    @SuppressWarnings("unchecked")
    public static GroupCommand getInstance(Object object)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (object == null) {
            return null;
        } else if (object instanceof GroupCommand) {
            return (GroupCommand) object;
        } else if (object instanceof Map) {
            return createInstance((Map<String, Object>) object);
        } else {
            throw new IllegalArgumentException("content error: " + object);
        }
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

