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
 *  Command Content
 *
 *  <blockquote><pre>
 *  data format: {
 *      'type' : i2s(0x88),
 *      'sn'   : 123,
 *
 *      'command' : "...", // command name
 *      'extra'   : info   // command parameters
 *  }
 *  </pre></blockquote>
 */
public interface Command extends Content {

    //-------- command names begin --------
    String META      = "meta";
    String DOCUMENTS = "documents";
    String RECEIPT   = "receipt";
    //-------- command names end --------

    /**
     *  Get command name
     *
     * @return command/method/declaration
     */
    String getCmd();

    //
    //  Factory methods
    //
    static Command parse(Object content) {
        return SharedCommandExtensions.cmdHelper.parseCommand(content);
    }

    static Factory getFactory(String cmd) {
        return SharedCommandExtensions.cmdHelper.getCommandFactory(cmd);
    }
    static void setFactory(String cmd, Factory factory) {
        SharedCommandExtensions.cmdHelper.setCommandFactory(cmd, factory);
    }

    /**
     *  Command Factory
     */
    interface Factory {

        /**
         *  Parse map object to command
         *
         * @param content - command content
         * @return Command
         */
        Command parseCommand(Map<String, Object> content);
    }

}
