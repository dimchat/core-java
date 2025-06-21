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
package chat.dim.dkd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import chat.dim.protocol.ArrayContent;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;

/**
 *  Array Content
 *
 *  <blockquote><pre>
 *  data format: {
 *      'type' : i2s(0xCA),
 *      'sn'   : 123,
 *
 *      'contents' : [...]  // content array
 *  }
 *  </pre></blockquote>
 */
public class ListContent extends BaseContent implements ArrayContent {

    private List<Content> list;

    public ListContent(Map<String, Object> content) {
        super(content);
        // lazy load
        list = null;
    }

    public ListContent(List<Content> contents) {
        super(ContentType.ARRAY);
        // set contents
        put("contents", ArrayContent.revert(contents));
        list = contents;
    }

    @Override
    public List<Content> getContents() {
        if (list == null) {
            Object info = get("contents");
            if (info instanceof List) {
                list = ArrayContent.convert((List<?>) info);
            } else {
                list = new ArrayList<>();
            }
        }
        return list;
    }

}
