package chat.dim.protocols;

import chat.dim.mkm.entity.ID;

import java.util.List;

public class ResetGroupCommand extends GroupCommand {

    public ResetGroupCommand(ID group, List<ID> members) {
        super(RESET, group, members);
    }
}
