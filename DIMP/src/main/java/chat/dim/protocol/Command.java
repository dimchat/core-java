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

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      cmd     : "...", // command name
 *      extra   : info   // command parameters
 *  }
 */
public interface Command extends Content {

    //-------- command names begin --------
    String META      = "meta";
    String DOCUMENT  = "document";

    String HANDSHAKE = "handshake";
    //-------- command names end --------

    /**
     *  Get command name
     *
     * @return command name string
     */
    String getCmd();

    static String getCmd(Map<String, Object> command) {
        // TODO: modify after all server/clients support 'cmd'
        Object cmd = command.get("cmd");
        if (cmd == null) {
            cmd = command.get("command");
        }
        return (String) cmd;
    }

    //
    //  Factory method
    //
    static Factory getFactory(String cmd) {
        return CommandFactories.commandFactories.get(cmd);
    }
    static void setFactory(String cmd, Factory factory) {
        CommandFactories.commandFactories.put(cmd, factory);
    }

    /**
     *  Command Factory
     *  ~~~~~~~~~~~~~~~
     */
    interface Factory {

        /**
         *  Parse map object to command
         *
         * @param command - command info
         * @return Command
         */
        Command parseCommand(Map<String, Object> command);
    }
}
