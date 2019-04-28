package chat.dim.protocols;

import chat.dim.dkd.content.HistoryCommand;
import chat.dim.mkm.entity.ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupCommand extends HistoryCommand {

    // Group ID for group message already defined in DKD.MessageContent
    public ID getGroup() {
        return ID.getInstance(super.getGroup());
    }

    public final ID member;
    public final List<ID> members;

    @SuppressWarnings("unchecked")
    public GroupCommand(Map<String, Object> dictionary) {
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

    /**
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

    /**
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

    /**
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
}

