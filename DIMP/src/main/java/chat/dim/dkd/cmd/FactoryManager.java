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
import chat.dim.type.Wrapper;

public enum FactoryManager {

    INSTANCE;

    public static FactoryManager getInstance() {
        return INSTANCE;
    }

    public GeneralFactory generalFactory = new GeneralFactory();

    public static class GeneralFactory extends chat.dim.dkd.FactoryManager.GeneralFactory {

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

        public String getCmd(Map<String, Object> content) {
            return (String) content.get("command");
        }

        public Command parseCommand(Object content) {
            if (content == null) {
                return null;
            } else if (content instanceof Command) {
                return (Command) content;
            }
            Map<String, Object> info = Wrapper.getMap(content);
            assert info != null : "command error: " + content;
            // get factory by command name
            String name = getCmd(info);
            Command.Factory factory = getCommandFactory(name);
            if (factory == null) {
                // unknown command name, get base command factory
                int type = getContentType(info);
                factory = (Command.Factory) getContentFactory(type);
                assert factory != null : "cannot parse command: " + content;
            }
            return factory.parseCommand(info);
        }
    }
}
