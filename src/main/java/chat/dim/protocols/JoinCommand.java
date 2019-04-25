package chat.dim.protocols;

import chat.dim.mkm.entity.ID;

public class JoinCommand extends GroupCommand {

    public JoinCommand(ID group) {
        super(JOIN, group);
    }
}
