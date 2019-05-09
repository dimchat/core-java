package chat.dim.protocols;

import java.util.Date;
import java.util.Map;

/**
 *  History command: {
 *      type : 0x89,
 *      sn   : 123,
 *
 *      command : "...", // command name
 *      time    : 0,     // command timestamp
 *      extra   : info   // command parameters
 *  }
 */
public class HistoryCommand extends CommandContent {

    //-------- command names begin --------
    // account
    public static final String REGISTER = "register";
    public static final String SUICIDE  = "suicide";
    // group: founder/owner
    public static final String FOUND    = "found";
    public static final String ABDICATE = "abdicate";
    // group: member
    public static final String INVITE   = "invite";
    public static final String EXPEL    = "expel";
    public static final String JOIN     = "join";
    public static final String QUIT     = "quit";
    public static final String QUERY    = "query";
    public static final String RESET    = "reset";
    // group: administrator/assistant
    public static final String HIRE     = "hire";
    public static final String FIRE     = "fire";
    public static final String RESIGN   = "resign";
    //-------- command names end --------

    public final Date time;

    public HistoryCommand(Map<String, Object> dictionary) {
        super(dictionary);
        time = getDate((long) dictionary.get("time"));
    }

    public HistoryCommand(String command) {
        super(ContentType.HISTORY.value, command);
        time = new Date();
        dictionary.put("time", getTimestamp(time));
    }

    private long getTimestamp(Date time) {
        return time.getTime() / 1000;
    }

    private Date getDate(long timestamp) {
        return new Date(timestamp * 1000);
    }
}
