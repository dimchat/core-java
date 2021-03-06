/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
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

import java.util.Map;

import chat.dim.core.Factories;
import chat.dim.dkd.BaseContent;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command : "...", // command name
 *      extra   : info   // command parameters
 *  }
 */
public class Command extends BaseContent {

    //-------- command names begin --------
    public static final String META      = "meta";
    public static final String PROFILE   = "profile";
    public static final String DOCUMENT  = "document";
    public static final String RECEIPT   = "receipt";
    public static final String HANDSHAKE = "handshake";
    public static final String LOGIN     = "login";
    //-------- command names end --------

    public Command(Map<String, Object> dictionary) {
        super(dictionary);
    }

    Command(ContentType type, String command) {
        super(type);
        put("command", command);
    }

    public Command(String command) {
        this(ContentType.COMMAND, command);
    }

    /**
     *  Get command name
     *
     * @return command name string
     */
    public String getCommand() {
        return getCommand(getMap());
    }

    public static String getCommand(Map<String, Object> cmd) {
        return (String) cmd.get("command");
    }

    //
    //  Factory method
    //
    public static Factory getFactory(String command) {
        return Factories.commandFactories.get(command);
    }
    public static void register(String command, Factory factory) {
        Factories.commandFactories.put(command, factory);
    }

    /**
     *  Command Factory
     *  ~~~~~~~~~~~~~~~
     */
    public interface Factory {

        /**
         *  Parse map object to command
         *
         * @param cmd - command info
         * @return Command
         */
        Command parseCommand(Map<String, Object> cmd);
    }
}
