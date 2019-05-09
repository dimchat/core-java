import chat.dim.mkm.entity.ID;
import chat.dim.protocols.group.GroupCommand;
import chat.dim.protocols.group.JoinCommand;
import junit.framework.TestCase;
import org.junit.Test;

public class CommandTest extends TestCase {

    private void log(String msg) {
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        String method = traces[2].getMethodName();
        int line = traces[2].getLineNumber();
        System.out.println("[" + method + ":" + line + "] " + msg);
    }

    @Test
    public void testGroupCommand() {
        ID groupID = ID.getInstance("Group-1280719982@7oMeWadRw4qat2sL4mTdcQSDAqZSo7LH5G");
        JoinCommand join = new JoinCommand(groupID);
        log("join:" + join);
        assertEquals(GroupCommand.JOIN, join.command);
    }
}
