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
package chat.dim.protocol;

import java.util.HashMap;
import java.util.Map;

import chat.dim.core.GeneralCommandFactory;
import chat.dim.core.GroupCommandFactory;
import chat.dim.core.HistoryCommandFactory;
import chat.dim.dkd.AudioFileContent;
import chat.dim.dkd.BaseContent;
import chat.dim.dkd.BaseDocumentCommand;
import chat.dim.dkd.BaseFileContent;
import chat.dim.dkd.BaseMetaCommand;
import chat.dim.dkd.BaseMoneyContent;
import chat.dim.dkd.BaseTextContent;
import chat.dim.dkd.ImageFileContent;
import chat.dim.dkd.SecretContent;
import chat.dim.dkd.TransferMoneyContent;
import chat.dim.dkd.VideoFileContent;
import chat.dim.dkd.WebPageContent;
import chat.dim.dkd.group.ExpelGroupCommand;
import chat.dim.dkd.group.InviteGroupCommand;
import chat.dim.dkd.group.JoinGroupCommand;
import chat.dim.dkd.group.QueryGroupCommand;
import chat.dim.dkd.group.QuitGroupCommand;
import chat.dim.dkd.group.ResetGroupCommand;

final class CommandFactories {

    static final Map<String, Command.Factory> commandFactories = new HashMap<>();

    static {
        registerContentFactories();
        registerCommandFactories();
    }

    /**
     *  Register core content factories
     */
    private static void registerContentFactories() {

        // Top-Secret
        Content.setFactory(ContentType.FORWARD, SecretContent::new);
        // Text
        Content.setFactory(ContentType.TEXT, BaseTextContent::new);

        // File
        Content.setFactory(ContentType.FILE, BaseFileContent::new);
        // Image
        Content.setFactory(ContentType.IMAGE, ImageFileContent::new);
        // Audio
        Content.setFactory(ContentType.AUDIO, AudioFileContent::new);
        // Video
        Content.setFactory(ContentType.VIDEO, VideoFileContent::new);

        // Web Page
        Content.setFactory(ContentType.PAGE, WebPageContent::new);

        // Money
        Content.setFactory(ContentType.MONEY, BaseMoneyContent::new);
        Content.setFactory(ContentType.TRANSFER, TransferMoneyContent::new);
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
    private static void registerCommandFactories() {

        // Meta Command
        Command.setFactory(Command.META, BaseMetaCommand::new);

        // Document Command
        Command.setFactory(Command.DOCUMENT, BaseDocumentCommand::new);

        // Group Commands
        Command.setFactory("group", new GroupCommandFactory());
        Command.setFactory(GroupCommand.INVITE, InviteGroupCommand::new);
        Command.setFactory(GroupCommand.EXPEL, ExpelGroupCommand::new);
        Command.setFactory(GroupCommand.JOIN, JoinGroupCommand::new);
        Command.setFactory(GroupCommand.QUIT, QuitGroupCommand::new);
        Command.setFactory(GroupCommand.QUERY, QueryGroupCommand::new);
        Command.setFactory(GroupCommand.RESET, ResetGroupCommand::new);
    }
}
