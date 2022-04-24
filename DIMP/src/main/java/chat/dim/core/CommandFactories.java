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
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.MoneyContent;
import chat.dim.protocol.PageContent;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.TransferContent;
import chat.dim.protocol.VideoContent;
import chat.dim.protocol.group.ExpelCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.JoinCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.QuitCommand;
import chat.dim.protocol.group.ResetCommand;

public class CommandFactories {

    public static final Map<String, Command.Factory> commandFactories = new HashMap<>();

    /**
     *  Register core content factories
     */
    public static void registerContentFactories() {

        // Top-Secret
        Content.setFactory(ContentType.FORWARD, ForwardContent::new);
        // Text
        Content.setFactory(ContentType.TEXT, TextContent::new);

        // File
        Content.setFactory(ContentType.FILE, FileContent::new);
        // Image
        Content.setFactory(ContentType.IMAGE, ImageContent::new);
        // Audio
        Content.setFactory(ContentType.AUDIO, AudioContent::new);
        // Video
        Content.setFactory(ContentType.VIDEO, VideoContent::new);

        // Web Page
        Content.setFactory(ContentType.PAGE, PageContent::new);

        // Money
        Content.setFactory(ContentType.MONEY, MoneyContent::new);
        Content.setFactory(ContentType.TRANSFER, TransferContent::new);
        // ...

        // Command
        Content.setFactory(ContentType.COMMAND, new GeneralCommandFactory());

        // History Command
        Content.setFactory(ContentType.HISTORY, new HistoryCommandFactory());

        // unknown content type
        Content.setFactory(0, BaseContent::new);
    }

    /**
     *  Register core command factories
     */
    public static void registerCommandFactories() {

        // Meta Command
        Command.setFactory(Command.META, MetaCommand::new);

        // Document Command
        Command.setFactory(Command.DOCUMENT, DocumentCommand::new);

        // Group Commands
        Command.setFactory("group", new GroupCommandFactory());
        Command.setFactory(GroupCommand.INVITE, InviteCommand::new);
        Command.setFactory(GroupCommand.EXPEL, ExpelCommand::new);
        Command.setFactory(GroupCommand.JOIN, JoinCommand::new);
        Command.setFactory(GroupCommand.QUIT, QuitCommand::new);
        Command.setFactory(GroupCommand.QUERY, QueryCommand::new);
        Command.setFactory(GroupCommand.RESET, ResetCommand::new);
    }
}
