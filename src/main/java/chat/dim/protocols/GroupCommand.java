package chat.dim.protocols;

import chat.dim.dkd.content.HistoryCommand;
import chat.dim.mkm.entity.ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupCommand extends HistoryCommand {

    // Group ID for group message already defined in DKD.MessageContent
    public ID getGroup() {
        if (this.group == null) {
            return ID.getInstance(dictionary.get("group"));
        } else {
            return ID.getInstance(this.group);
        }
    }

    public final ID member;
    public final List<ID> members;

    public GroupCommand(GroupCommand content) {
        super(content);
        this.member  = content.member;
        this.members = content.members;
    }

    @SuppressWarnings("unchecked")
    public GroupCommand(Map<String, Object> dictionary) {
        super(dictionary);
        Object member = dictionary.get("member");
        if (member == null) {
            this.member  = null;
            List<String> members = (List<String>) dictionary.get("members");
            if (members == null) {
                this.members = null;
            } else {
                List<ID> array = new ArrayList<>(members.size());
                for (String string: members) {
                    array.add(ID.getInstance(string));
                }
                this.members = array;
            }
        } else {
            this.member  = ID.getInstance(member);
            this.members = null;
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
    public GroupCommand(String command, ID group) {
        super(command);
        this.group   = group.toString();
        this.member  = null;
        this.members = null;

        this.dictionary.put("group", group.toString());
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
    public GroupCommand(String command, ID group, ID member) {
        super(command);
        this.group   = group.toString();
        this.member  = member;
        this.members = null;

        this.dictionary.put("group", group.toString());
        this.dictionary.put("member", member.toString());
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
    public GroupCommand(String command, ID group, List<ID> members) {
        super(command);
        this.group   = group.toString();
        this.member  = null;
        this.members = members;

        List<String> array = new ArrayList<>(members.size());
        for (ID member : members) {
            array.add(member.toString());
        }
        this.dictionary.put("members", array);
        this.dictionary.put("group", group.toString());
    }
}

