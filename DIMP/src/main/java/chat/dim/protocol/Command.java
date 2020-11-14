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
    public static final String RECEIPT   = "receipt";
    public static final String HANDSHAKE = "handshake";
    public static final String LOGIN     = "login";
    //-------- command names end --------

    protected Command(Map<String, Object> dictionary) {
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
        return (String) get("command");
    }

    /**
     *  Command Parser
     *  ~~~~~~~~~~~~~~
     */
    public static class Parser {

        // override to support other command name
        protected Command parseCommand(Map<String, Object> cmd, String name) {
            if (META.equals(name)) {
                return new MetaCommand(cmd);
            }
            if (PROFILE.equals(name)) {
                return new ProfileCommand(cmd);
            }
            return null;
        }

        /**
         *  Parse map object to command
         *
         * @param cmd - command info
         * @return Command
         */
        public Command parseCommand(Map<String, Object> cmd) {

            //
            //  Group Commands
            //
            Object group = cmd.get("group");
            if (group != null) {
                return GroupCommand.parseCommand(cmd);
            }

            //
            //  Core Commands
            //
            String name = (String) cmd.get("command");
            Command core = parseCommand(cmd, name);
            if (core != null) {
                return core;
            }

            return new Command(cmd);
        }
    }

    // default parser
    public static Parser parser = new Parser();

    public static Command parseCommand(Map<String, Object> cmd) {
        return parser.parseCommand(cmd);
    }
}
