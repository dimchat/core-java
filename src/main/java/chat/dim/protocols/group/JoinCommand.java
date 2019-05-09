package chat.dim.protocols.group;

import chat.dim.mkm.entity.ID;

public class JoinCommand extends GroupCommand {

    public JoinCommand(ID group) {
        super(JOIN, group);
    }
}
