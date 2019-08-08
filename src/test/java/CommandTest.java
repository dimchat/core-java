
import junit.framework.TestCase;
import org.junit.Test;

import chat.dim.mkm.entity.ID;
import chat.dim.protocol.group.GroupCommand;
import chat.dim.protocol.group.JoinCommand;

public class CommandTest extends TestCase {

    @Test
    public void testGroupCommand() {
        ID groupID = ID.getInstance("Group-1280719982@7oMeWadRw4qat2sL4mTdcQSDAqZSo7LH5G");
        JoinCommand join = new JoinCommand(groupID);
        Log.info("join: " + join);
        assertEquals(GroupCommand.JOIN, join.command);
    }
}
