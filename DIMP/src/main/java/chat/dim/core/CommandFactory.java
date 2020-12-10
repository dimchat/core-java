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

import chat.dim.dkd.ContentFactory;
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

public final class CommandFactory {

    private static final Map<String, Content.Parser<Command>> commandParsers = new HashMap<>();

    public static void register(String command, Content.Parser<Command> parser) {
        commandParsers.put(command, parser);
    }

    public static class CommandParser implements Content.Parser<Command> {

        protected Content.Parser<Command> getParser(String name) {
            return commandParsers.get(name);
        }
        protected Content.Parser<Command> getParser(Map<String, Object> cmd) {
            String command = (String) cmd.get("command");
            return getParser(command);
        }

        @Override
        public Command parse(Map<String, Object> cmd) {
            Content.Parser<Command> parser = getParser(cmd);
            if (parser == null) {
                // Check for group commands
                Object group = cmd.get("group");
                if (group != null) {
                    parser = getParser("group");
                }
            }
            if (parser != null) {
                return parser.parse(cmd);
            }
            return new Command(cmd);
        }
    }

    public static class HistoryParser extends CommandParser {

        @Override
        public Command parse(Map<String, Object> cmd) {
            Content.Parser<Command> parser = getParser(cmd);
            if (parser == null) {
                // Check for group commands
                Object group = cmd.get("group");
                if (group != null) {
                    parser = getParser("group");
                }
            }
            if (parser != null) {
                return parser.parse(cmd);
            }
            return new HistoryCommand(cmd);
        }
    }

    public static class GroupCommandParser extends HistoryParser {

        @Override
        public Command parse(Map<String, Object> cmd) {
            Content.Parser<Command> parser = getParser(cmd);
            if (parser != null) {
                return parser.parse(cmd);
            }
            return new GroupCommand(cmd);
        }
    }

    public static void registerCoreParsers() {
        //
        //  Register content parsers
        //
        ContentFactory.register(ContentType.FORWARD, ForwardContent::new);

        ContentFactory.register(ContentType.TEXT, TextContent::new);

        ContentFactory.register(ContentType.FILE, FileContent::new);
        ContentFactory.register(ContentType.IMAGE, ImageContent::new);
        ContentFactory.register(ContentType.AUDIO, AudioContent::new);
        ContentFactory.register(ContentType.VIDEO, VideoContent::new);

        ContentFactory.register(ContentType.PAGE, PageContent::new);

        ContentFactory.register(ContentType.COMMAND, new CommandParser());
        ContentFactory.register(ContentType.HISTORY, new HistoryParser());

        //
        //  Register command parsers
        //
        register(Command.META, MetaCommand::new);
        register(Command.PROFILE, DocumentCommand::new);
        register(Command.DOCUMENT, DocumentCommand::new);

        register("group", new GroupCommandParser());
        register(GroupCommand.INVITE, InviteCommand::new);
        register(GroupCommand.EXPEL, ExpelCommand::new);
        register(GroupCommand.JOIN, JoinCommand::new);
        register(GroupCommand.QUIT, QuitCommand::new);
        register(GroupCommand.QUERY, QueryCommand::new);
        register(GroupCommand.RESET, ResetCommand::new);
    }
}
