/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim.core;

import java.util.Map;

import chat.dim.dkd.cmd.BaseGroupCommand;
import chat.dim.dkd.cmd.FactoryManager;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;

public class GroupCommandFactory extends HistoryCommandFactory {

    @Override
    public Content parseContent(Map<String, Object> content) {
        FactoryManager man = FactoryManager.getInstance();
        String cmd = man.generalFactory.getCmd(content);
        // get factory by command name
        Command.Factory factory = man.generalFactory.commandFactories.get(cmd);
        if (factory == null) {
            factory = this;
        }
        return factory.parseCommand(content);
    }

    @Override
    public Command parseCommand(Map<String, Object> command) {
        return new BaseGroupCommand(command);
    }
}
