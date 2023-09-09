/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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
package chat.dim.dkd.cmd;

import java.util.HashMap;
import java.util.Map;

import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.type.Converter;
import chat.dim.type.Wrapper;

/**
 *  Command GeneralFactory
 *  ~~~~~~~~~~~~~~~~~~~~~~
 */
public class GeneralFactory {

    private final Map<String, Command.Factory> commandFactories = new HashMap<>();

    //
    //  Command
    //

    public void setCommandFactory(String cmd, Command.Factory factory) {
        commandFactories.put(cmd, factory);
    }

    public Command.Factory getCommandFactory(String cmd) {
        return commandFactories.get(cmd);
    }

    public String getCmd(Map<?, ?> content, String defaultValue) {
        return Converter.getString(content.get("command"), defaultValue);
    }

    public Command parseCommand(Object content) {
        if (content == null) {
            return null;
        } else if (content instanceof Command) {
            return (Command) content;
        }
        Map<String, Object> info = Wrapper.getMap(content);
        if (info == null) {
            assert false : "command error: " + content;
            return null;
        }
        // get factory by command name
        String name = getCmd(info, "*");
        Command.Factory factory = getCommandFactory(name);
        if (factory == null) {
            // unknown command name, get base command factory
            factory = getDefaultFactory(info);
            if (factory == null) {
                assert false : "cannot parse command: " + content;
                return null;
            }
        }
        return factory.parseCommand(info);
    }

    private static Command.Factory getDefaultFactory(Map<?, ?> info) {
        chat.dim.msg.FactoryManager man = chat.dim.msg.FactoryManager.getInstance();
        int type = man.generalFactory.getContentType(info, 0);
        Content.Factory factory = man.generalFactory.getContentFactory(type);
        if (factory instanceof Command.Factory) {
            return (Command.Factory) factory;
        } else {
            return null;
        }
    }
}
