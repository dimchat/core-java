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

import chat.dim.dkd.Content;
import chat.dim.protocol.file.AudioContent;
import chat.dim.protocol.file.FileContent;
import chat.dim.protocol.file.ImageContent;
import chat.dim.protocol.file.VideoContent;

/*
 *  @enum DKDMessageType
 *
 *  @abstract A flag to indicate what kind of message content this is.
 *
 *  @discussion A message is something send from one place to another one,
 *      it can be an instant message, a system command, or something else.
 *
 *      DKDMessageType_Text indicates this is a normal message with plaintext.
 *
 *      DKDMessageType_File indicates this is a file, it may include filename
 *      and file data, but usually the file data will encrypted and upload to
 *      somewhere and here is just a URL to retrieve it.
 *
 *      DKDMessageType_Image indicates this is an image, it may send the image
 *      data directly(encrypt the image data with Base64), but we suggest to
 *      include a URL for this image just like the 'File' message, of course
 *      you can get a thumbnail of this image here.
 *
 *      DKDMessageType_Audio indicates this is a voice message, you can get
 *      a URL to retrieve the voice data just like the 'File' message.
 *
 *      DKDMessageType_Video indicates this is a video file.
 *
 *      DKDMessageType_Page indicates this is a web page.
 *
 *      DKDMessageType_Quote indicates this message has quoted another message
 *      and the message content should be a plaintext.
 *
 *      DKDMessageType_Command indicates this is a command message.
 *
 *      DKDMessageType_Forward indicates here contains a TOP-SECRET message
 *      which needs your help to redirect it to the true receiver.
 *
 *  Bits:
 *      0000 0001 - this message contains plaintext you can read.
 *      0000 0010 - this is a message you can see.
 *      0000 0100 - this is a message you can hear.
 *      0000 1000 - this is a message for the robot, not for human.
 *
 *      0001 0000 - this message's main part is in somewhere else.
 *      0010 0000 - this message contains the 3rd party content.
 *      0100 0000 - (RESERVED)
 *      1000 0000 - this is a message send by the system, not human.
 *
 *      (All above are just some advices to help choosing numbers :P)
 */
public enum ContentType {

    TEXT    (0x01), // 0000 0001

    FILE    (0x10), // 0001 0000
    IMAGE   (0x12), // 0001 0010
    AUDIO   (0x14), // 0001 0100
    VIDEO   (0x16), // 0001 0110

    // web page
    PAGE    (0x20), // 0010 0000

    // quote a message before and reply it with text
    QUOTE   (0x37), // 0011 0111

    COMMAND (0x88), // 1000 1000
    HISTORY (0x89), // 1000 1001 (Entity history command)

    // top-secret message forward by proxy (Service Provider)
    FORWARD (0xFF); // 1111 1111

    public final int value;

    ContentType(int value) {
        this.value = value;
    }

    static {
        // Text
        Content.register(TEXT.value, TextContent.class);

        // File
        Content.register(FILE.value, FileContent.class);
        // Image
        Content.register(IMAGE.value, ImageContent.class);
        // Audio
        Content.register(AUDIO.value, AudioContent.class);
        // Video
        Content.register(VIDEO.value, VideoContent.class);

        // Page
        Content.register(PAGE.value, PageContent.class);

        // Quote

        // Command
        Content.register(COMMAND.value, CommandContent.class);

        // History
        Content.register(HISTORY.value, HistoryCommand.class);

        // Forward
        Content.register(FORWARD.value, ForwardContent.class);

        // ...
    }
}
