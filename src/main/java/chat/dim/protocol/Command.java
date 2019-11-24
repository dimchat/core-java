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

import java.util.HashMap;
import java.util.Map;

import chat.dim.Content;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command : "...", // command name
 *      extra   : info   // command parameters
 *  }
 */
public class Command extends Content {

    //-------- command names begin --------
    public static final String HANDSHAKE = "handshake";
    public static final String RECEIPT   = "receipt";
    public static final String META      = "meta";
    public static final String PROFILE   = "profile";
    //-------- command names end --------

    public final String command;

    public Command(Map<String, Object> dictionary) {
        super(dictionary);
        command = (String) dictionary.get("command");
    }

    protected Command(ContentType type, String cmd) {
        super(type);
        command = cmd;
        dictionary.put("command", cmd);
    }

    public Command(String command) {
        this(ContentType.COMMAND, command);
    }

    //-------- Runtime --------

    private static Map<String, Class> commandClasses = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static void register(String command, Class clazz) {
        // check whether clazz is subclass of CommandContent
        if (clazz.equals(Command.class)) {
            throw new IllegalArgumentException("should not add Command.class itself!");
        }
        assert Command.class.isAssignableFrom(clazz); // asSubclass
        commandClasses.put(command, clazz);
    }

    static Class commandClass(Map<String, Object> dictionary) {
        // get subclass by command name
        String command = (String) dictionary.get("command");
        return commandClasses.get(command);
    }

    @SuppressWarnings("unchecked")
    public static Command getInstance(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof Command) {
            return (Command) object;
        }
        assert object instanceof Map;
        Map<String, Object> dictionary = (Map<String, Object>) object;
        Class clazz = commandClass(dictionary);
        if (clazz != null) {
            // create instance by subclass (with command name)
            return (Command) createInstance(clazz, dictionary);
        }
        // custom command
        return new Command(dictionary);
    }

    static {
        // Handshake
        register(HANDSHAKE, HandshakeCommand.class);
        // Meta
        register(META, MetaCommand.class);
        // Profile
        register(PROFILE, ProfileCommand.class);
        // ...
    }
}
