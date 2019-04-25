package chat.dim.protocols;

import chat.dim.mkm.entity.ID;

import java.util.List;

public class ExpelCommand extends GroupCommand {

    public ExpelCommand(ID group, ID member) {
        super(EXPEL, group, member);
    }

    public ExpelCommand(ID group, List<ID> members) {
        super(EXPEL, group, members);
    }
}
