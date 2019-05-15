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

import chat.dim.dkd.Content;

import java.util.Map;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command : "...", // command name
 *      extra   : info   // command parameters
 *  }
 */
public class CommandContent extends Content {

    //-------- command names begin --------
    public static final String HANDSHAKE = "handshake";
    public static final String BROADCAST = "broadcast";
    public static final String RECEIPT   = "receipt";
    public static final String META      = "meta";
    public static final String PROFILE   = "profile";
    //-------- command names end --------

    public final String command;

    public CommandContent(Map<String, Object> dictionary) {
        super(dictionary);
        command = (String) dictionary.get("command");
    }

    protected CommandContent(int type, String cmd) {
        super(type);
        command = cmd;
        dictionary.put("command", cmd);
    }

    public CommandContent(String command) {
        this(ContentType.COMMAND.value, command);
    }
}
