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

import chat.dim.ID;

public class Content extends chat.dim.Content<ID> {

    protected Content(Map<String, Object> dictionary) {
        super(dictionary);
    }

    protected Content(ContentType msgType) {
        super(msgType);
    }

    protected Content(int msgType) {
        super(msgType);
    }

    //-------- Runtime --------

    private static Map<Integer, Class> contentClasses = new HashMap<>();

    public static void register(ContentType type, Class clazz) {
        register(type.value, clazz);
    }

    public static void register(int type, Class clazz) {
        if (clazz == null) {
            contentClasses.remove(type);
        } else if (clazz.equals(Content.class)) {
            throw new IllegalArgumentException("should not add Content itself!");
        } else {
            assert Content.class.isAssignableFrom(clazz) : "error: " + clazz;
            contentClasses.put(type, clazz);
        }
    }

    public static Content getInstance(Object object) {
        if (object == null) {
            return null;
        }
        //noinspection unchecked
        Map<String, Object> dictionary = (Map<String, Object>) object;
        // create instance by subclass (with content type)
        int type = (int) dictionary.get("type");
        Class clazz = contentClasses.get(type);
        //noinspection unchecked
        if (clazz != null && !clazz.isAssignableFrom(object.getClass())) {
            return (Content) createInstance(clazz, dictionary);
        }
        if (object instanceof Content) {
            // return Content object directly
            return (Content) object;
        }
        return new Content(dictionary);
    }

    static {
        // Forward content for Top-Secret message
        register(ContentType.FORWARD, ForwardContent.class);

        // Text
        register(ContentType.TEXT, TextContent.class);

        // File
        register(ContentType.FILE, FileContent.class);
        // - Image
        register(ContentType.IMAGE, ImageContent.class);
        // - Audio
        register(ContentType.AUDIO, AudioContent.class);
        // - Video
        register(ContentType.VIDEO, VideoContent.class);

        // Page
        register(ContentType.PAGE, PageContent.class);

        // Quote

        // Command
        register(ContentType.COMMAND, Command.class);
        // - MetaCommand
        //   - ProfileCommand

        // History
        register(ContentType.HISTORY, HistoryCommand.class);
        // - GroupCommand
        //   - InviteCommand
        //   - ExpelCommand
        //   - JoinCommand
        //   - QuitCommand
        //   - QueryCommand
        //   - ResetCommand

        // ...
    }
}
