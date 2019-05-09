package chat.dim.core;

import chat.dim.mkm.Account;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

public interface BarrackDelegate {

    boolean saveMeta(Meta meta, ID identifier);

    Account getAccount(ID identifier);
    User getUser(ID identifier);
    Group getGroup(ID identifier);
}
