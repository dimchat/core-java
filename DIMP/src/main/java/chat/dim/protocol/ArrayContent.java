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
package chat.dim.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import chat.dim.dkd.ListContent;

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
public interface ArrayContent extends Content {

    List<Content> getContents();

    //
    //  Factory
    //

    static ArrayContent create(List<Content> contents) {
        return new ListContent(contents);
    }

    static List<Content> convert(Iterable<?> contents) {
        List<Content> array = new ArrayList<>();
        Content msg;
        for (Object item : contents) {
            msg = Content.parse(item);
            if (msg != null) {
                array.add(msg);
            }
        }
        return array;
    }

    static List<Map<String, Object>> revert(Iterable<Content> contents) {
        List<Map<String, Object>> array = new ArrayList<>();
        for (Content item : contents) {
            array.add(item.toMap());
        }
        return array;
    }
}
