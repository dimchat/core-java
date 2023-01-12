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

        public final Map<String, Command.Factory> commandFactories = new HashMap<>();

        public String getCmd(Map<String, Object> command) {
            return (String) command.get("cmd");
        }

        public Command parseCommand(Object command) {
            if (command == null) {
                return null;
            } else if (command instanceof Command) {
                return (Command) command;
            }
            Map<String, Object> info = Wrapper.getMap(command);
            assert info != null : "command error: " + command;
            // get factory by content type
            String name = getCmd(info);
            Command.Factory factory = commandFactories.get(name);
            if (factory == null) {
                int type = getContentType(info);
                factory = (Command.Factory) contentFactories.get(type);
                assert factory != null : "cannot parse command: " + command;
            }
            return factory.parseCommand(info);
        }
    }
}