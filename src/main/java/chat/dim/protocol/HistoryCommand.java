/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.protocol;

import java.util.Date;
import java.util.Map;

import chat.dim.protocol.group.GroupCommand;

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
public class HistoryCommand extends Command {

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
        Object timestamp = dictionary.get("time");
        if (timestamp == null) {
            time = null;
        } else {
            time = getDate((Number) timestamp);
        }
    }

    public HistoryCommand(String command) {
        super(ContentType.HISTORY.value, command);
        time = new Date();
        dictionary.put("time", getTimestamp(time));
    }

    private long getTimestamp(Date time) {
        return time.getTime() / 1000;
    }

    private Date getDate(Number timestamp) {
        return new Date(timestamp.longValue() * 1000);
    }

    //-------- Runtime --------

    @SuppressWarnings("unchecked")
    public static HistoryCommand getInstance(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof HistoryCommand) {
            return (HistoryCommand) object;
        }
        assert object instanceof Map;
        Map<String, Object> dictionary = (Map<String, Object>) object;
        Object group = dictionary.get("group");
        if (group != null) {
            // group history command
            return GroupCommand.getInstance(dictionary);
        }
        // unsupported history command
        return new HistoryCommand(dictionary);
    }
}
