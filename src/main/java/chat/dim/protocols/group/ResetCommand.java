package chat.dim.protocols.group;

import chat.dim.mkm.entity.ID;

import java.util.List;

public class ResetCommand extends GroupCommand {

    public ResetCommand(ID group, List<ID> members) {
        super(RESET, group, members);
    }
}
