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

import java.util.Map;

import chat.dim.dkd.BaseContent;
import chat.dim.protocol.AudioContent;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ForwardContent;
import chat.dim.protocol.HistoryCommand;
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.PageContent;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.VideoContent;

public class ContentParser implements Content.Factory {

    // override to support other content type
    protected Content parseContent(Map<String, Object> content, int type) {

        if (type == ContentType.FORWARD.value) {
            return new ForwardContent(content);
        }

        if (type == ContentType.TEXT.value) {
            return new TextContent(content);
        }

        if (type == ContentType.FILE.value) {
            return new FileContent(content);
        }
        if (type == ContentType.IMAGE.value) {
            return new ImageContent(content);
        }
        if (type == ContentType.AUDIO.value) {
            return new AudioContent(content);
        }
        if (type == ContentType.VIDEO.value) {
            return new VideoContent(content);
        }

        if (type == ContentType.PAGE.value) {
            return new PageContent(content);
        }

        return null;
    }

    @Override
    public Content parseContent(Map<String, Object> content) {

        int type = (int) content.get("type");

        //
        //  Core Contents
        //
        Content core = parseContent(content, type);
        if (core != null) {
            return core;
        }

        //
        //  Commands
        //
        if (type == ContentType.COMMAND.value) {
            if (content instanceof Command) {
                return (Command) content;
            }
            return Command.parse(content);
        }

        //
        //  History Commands
        //
        if (type == ContentType.HISTORY.value) {
            if (content instanceof HistoryCommand) {
                return (HistoryCommand) content;
            }
            return HistoryCommand.parse(content);
        }

        return new BaseContent(content);
    }
}
