package chat.dim.core;

import chat.dim.mkm.Account;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

public interface IBarrackDelegate {

    public boolean saveMeta(Meta meta, ID identifier);

    public Account getAccount(ID identifier);
    public User getUser(ID identifier);
    public Group getGroup(ID identifier);
}
