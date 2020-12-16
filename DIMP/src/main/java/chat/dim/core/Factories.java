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

import java.util.HashMap;
import java.util.Map;

import chat.dim.dkd.BaseContent;
import chat.dim.protocol.AudioContent;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ForwardContent;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.HistoryCommand;
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.PageContent;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.VideoContent;
import chat.dim.protocol.group.ExpelCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.JoinCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.QuitCommand;
import chat.dim.protocol.group.ResetCommand;

public class Factories {

    public static class ContentFactory implements Content.Factory {

        @Override
        public Content parseContent(Map<String, Object> content) {
            return new BaseContent(content);
        }
    }

    public static class CommandFactory implements Content.Factory {

        @Override
        public Content parseContent(Map<String, Object> content) {
            String command = Command.getCommand(content);
            // get factory by command name
            Command.Factory factory = Command.getFactory(command);
            if (factory == null) {
                // check for group command
                if (Content.getGroup(content) != null) {
                    factory = Command.getFactory("group");
                }
                if (factory == null) {
                    // unknown command
                    factory = Command.getFactory("*");
                    if (factory == null) {
                        return new Command(content);
                    }
                }
            }
            return factory.parseCommand(content);
        }
    }

    public static class HistoryCommandFactory implements Content.Factory {

        @Override
        public Content parseContent(Map<String, Object> content) {
            String command = Command.getCommand(content);
            // get factory by command name
            Command.Factory factory = Command.getFactory(command);
            if (factory == null) {
                // check for group command
                if (Content.getGroup(content) != null) {
                    factory = Command.getFactory("group");
                }
                if (factory == null) {
                    // unknown command
                    factory = Command.getFactory("*");
                    if (factory == null) {
                        return new HistoryCommand(content);
                    }
                }
            }
            return factory.parseCommand(content);
        }
    }

    public static class GroupCommandFactory implements Command.Factory {

        @Override
        public Command parseCommand(Map<String, Object> cmd) {
            String command = Command.getCommand(cmd);
            // get factory by command name
            Command.Factory factory = Command.getFactory(command);
            if (factory == null || factory == this) {
                return new GroupCommand(cmd);
            }
            return factory.parseCommand(cmd);
        }
    }

    public static final Map<String, Command.Factory> commandFactories = new HashMap<>();

    static {
        //
        //  Register content factories
        //
        Content.register(ContentType.FORWARD, ForwardContent::new);

        Content.register(ContentType.TEXT, TextContent::new);

        Content.register(ContentType.FILE, FileContent::new);
        Content.register(ContentType.IMAGE, ImageContent::new);
        Content.register(ContentType.AUDIO, AudioContent::new);
        Content.register(ContentType.VIDEO, VideoContent::new);

        Content.register(ContentType.PAGE, PageContent::new);

        Content.register(ContentType.COMMAND, new CommandFactory());
        Content.register(ContentType.HISTORY, new HistoryCommandFactory());

        Content.register(0, new ContentFactory());  // unknown

        //
        //  Register command factories
        //
        Command.register(Command.META, MetaCommand::new);
        Command.register(Command.PROFILE, DocumentCommand::new);
        Command.register(Command.DOCUMENT, DocumentCommand::new);

        Command.register("group", new GroupCommandFactory());
        Command.register(GroupCommand.INVITE, InviteCommand::new);
        Command.register(GroupCommand.EXPEL, ExpelCommand::new);
        Command.register(GroupCommand.JOIN, JoinCommand::new);
        Command.register(GroupCommand.QUIT, QuitCommand::new);
        Command.register(GroupCommand.QUERY, QueryCommand::new);
        Command.register(GroupCommand.RESET, ResetCommand::new);
    }
}
