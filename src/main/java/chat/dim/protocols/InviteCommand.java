package chat.dim.protocols;

import chat.dim.mkm.entity.ID;

import java.util.List;

public class InviteCommand extends GroupCommand {

    public InviteCommand(ID group, ID member) {
        super(INVITE, group, member);
    }

    public InviteCommand(ID group, List<ID> members) {
        super(INVITE, group, members);
    }
}
