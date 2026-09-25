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
package chat.dim.ext;

import java.util.Map;

import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;

/**
 *  A helper interface for extracting command names from structured command content.
 *
 *  This interface provides a standardized way to retrieve command identifiers
 *  from command payloads (typically Map-based), with support for default values.
 */
public interface CommandHandler /*extends Command.Helper */{

    //
    //  CMD
    //

    /**
     *  Retrieves the command name from a structured command content Map.
     *
     *  Looks up the command name key (e.g., "command") in the content Map
     *  and returns its value. If the key is not found or the value is null,
     *  returns the defaultValue (if provided).
     *
     * @param content the structured command payload (Map) to extract the command name from
     * @param defaultValue the optional fallback value if the command name is not found
     * @return the extracted command name, or the defaultValue, or null if neither exists
     */
    String getCmd(Map<?, ?> content, String defaultValue);

    //
    //  Receipt
    //

    /**
     *  Create ReceiptCommand with original envelope info.
     *
     *  Extracts and cleans up metadata from the original message envelope/content
     *  to form the "origin" field in receipt commands (removes sensitive/redundant fields).
     *
     * @param text the message
     * @param head the original message envelope
     * @param body the original instant message content (optional)
     * @return a Command receipt
     */
    Command createReceipt(String text, Envelope head, Content body);

}
