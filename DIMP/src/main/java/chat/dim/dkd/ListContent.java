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
 *  Content Array message: {
 *      type : 0xCA,
 *      sn   : 123,
 *
 *      contents : [...]  // content array
 *  }
 */
public class ListContent extends BaseContent implements ArrayContent {

    private List<Content> list;

    public ListContent(Map<String, Object> dictionary) {
        super(dictionary);
        // lazy load
        list = null;
    }

    public ListContent(List<Content> contents) {
        super(ContentType.ARRAY);
        list = contents;
        put("contents", revert(contents));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Content> getContents() {
        if (list == null) {
            Object info = get("contents");
            if (info != null) {
                list = convert((List<Object>) info);
            } else {
                list = new ArrayList<>();
            }
        }
        return list;
    }

    static List<Content> convert(List<Object> contents) {
        List<Content> array = new ArrayList<>();
        Content res;
        for (Object item : contents) {
            res = Content.parse(item);
            if (res != null) {
                array.add(res);
            }
        }
        return array;
    }
    static List<Object> revert(List<Content> contents) {
        List<Object> array = new ArrayList<>();
        for (Content item : contents) {
            array.add(item.toMap());
        }
        return array;
    }
}
