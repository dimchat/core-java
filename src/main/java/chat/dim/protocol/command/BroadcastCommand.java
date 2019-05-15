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
package chat.dim.protocol.command;

import chat.dim.protocol.CommandContent;

import java.util.Map;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command : "broadcast",
 *      title   : "...", // broadcast title
 *      extra   : info   // broadcast info
 *  }
 */
public class BroadcastCommand extends CommandContent {

    public static String REPORT = "report";

    public final String title;

    public BroadcastCommand(Map<String, Object> dictionary) {
        super(dictionary);
        title = (String) dictionary.get("title");
    }

    public BroadcastCommand(String message) {
        super(BROADCAST);
        title = message;
        dictionary.put("title", message);
    }
}
