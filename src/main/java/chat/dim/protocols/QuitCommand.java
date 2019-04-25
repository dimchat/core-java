package chat.dim.protocols;

import chat.dim.mkm.entity.ID;

public class QuitCommand extends GroupCommand {

    public QuitCommand(ID group) {
        super(QUIT, group);
    }
}
