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

import chat.dim.ext.SharedCommandExtensions;

/**
 *  Command message content interface.
 *
 *  Base interface for all command-type messages, which are used to send
 *  operational instructions with parameters between entities.
 *
 *  <blockquote><pre>
 *  JSON format: {
 *      "type"  : i2s(0x88),
 *      "sn"    : 12345,
 *
 *      "time"    : 123.45,         // message time
 *      "group" : "group@zzz",
 *
 *      "command" : "...",  // Unique command name/identifier
 *      "extra"   : info    // Optional command parameters (dynamic structure)
 *  }
 *  </pre></blockquote>
 */
public interface Command extends Content {

    /**
     *  Get command name
     *
     * @return command/method/declaration
     */
    String getCmd();

    //
    //  Factory methods
    //

    /**
     *  Parse any object to command
     *
     * @param content any object (map/string/...)
     * @return Command
     */
    static Command parse(Object content) {
        return SharedCommandExtensions.commandHelper.parseCommand(content);
    }

    /**
     *  Get command factory for name (cmd)
     *
     * @param cmd command name
     * @return CommandFactory
     */
    static Factory getFactory(String cmd) {
        return SharedCommandExtensions.commandHelper.getCommandFactory(cmd);
    }

    /**
     *  Set command factory for name (cmd)
     *
     * @param cmd command name
     * @param factory CommandFactory
     */
    static void setFactory(String cmd, Factory factory) {
        SharedCommandExtensions.commandHelper.setCommandFactory(cmd, factory);
    }

    /**
     *  Factory interface for parsing command messages from map objects.
     *
     *  Provides a standardized way to convert raw map data (from JSON) into
     *  strongly-typed {@link Command} instances.
     */
    interface Factory {

        /**
         *  Parses a map object (from JSON) into a {@link Command} instance.
         *
         * @param content is the raw map data containing command information.
         * @return a Command instance if parsing succeeds, null otherwise.
         */
        Command parseCommand(Map<String, Object> content);
    }

}
