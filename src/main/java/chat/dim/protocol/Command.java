/* license: https://mit-license.org
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import chat.dim.dkd.Content;

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

    protected Command(int type, String cmd) {
        super(type);
        command = cmd;
        dictionary.put("command", cmd);
    }

    public Command(String command) {
        this(ContentType.COMMAND.value, command);
    }

    //-------- Runtime --------

    private static Map<String, Class> commandClasses = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static void register(String command, Class clazz) {
        // check whether clazz is subclass of CommandContent
        if (clazz.equals(Command.class)) {
            throw new IllegalArgumentException("should not add CommandContent.class itself!");
        }
        clazz = clazz.asSubclass(Command.class);
        commandClasses.put(command, clazz);
    }

    @SuppressWarnings("unchecked")
    private static Command createInstance(Map<String, Object> dictionary) {
        // get subclass by command name
        String command = (String) dictionary.get("command");
        Class clazz = commandClasses.get(command);
        if (clazz == null) {
            //throw new ClassNotFoundException("unknown command: " + command);
            return new Command(dictionary);
        }
        // try 'getInstance()' of subclass
        try {
            Method method = clazz.getMethod("getInstance", Object.class);
            if (method.getDeclaringClass().equals(clazz)) {
                return (Command) method.invoke(null, dictionary);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        // try 'new MyCommand(dict)'
        try {
            Constructor constructor = clazz.getConstructor(Map.class);
            return (Command) constructor.newInstance(dictionary);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Command getInstance(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof Command) {
            return (Command) object;
        } else if (object instanceof Map) {
            return createInstance((Map<String, Object>) object);
        } else {
            throw new IllegalArgumentException("ommand error: " + object);
        }
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
