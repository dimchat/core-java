package chat.dim.protocols.group;

import chat.dim.mkm.entity.ID;

public class QueryCommand extends GroupCommand {

    public QueryCommand(ID group) {
        super(QUERY, group);
    }
}
