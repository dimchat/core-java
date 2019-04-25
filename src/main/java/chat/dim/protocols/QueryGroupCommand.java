package chat.dim.protocols;

import chat.dim.mkm.entity.ID;

public class QueryGroupCommand extends GroupCommand {

    public QueryGroupCommand(ID group) {
        super(QUERY, group);
    }
}
