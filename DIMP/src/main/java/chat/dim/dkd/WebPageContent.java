/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
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
package chat.dim.dkd;

import java.util.Map;

import chat.dim.format.Base64;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.PageContent;

/**
 *  Web Page message: {
 *      type : 0x20,
 *      sn   : 123,
 *
 *      URL   : "https://github.com/moky/dimp", // Page URL
 *      icon  : "...",                          // base64_encode(icon)
 *      title : "...",
 *      desc  : "..."
 *  }
 */
public class WebPageContent extends BaseContent implements PageContent {

    private byte[] icon = null;

    public WebPageContent(Map<String, Object> dictionary) {
        super(dictionary);
    }

    public WebPageContent(String url, String title, String desc, byte[] icon) {
        super(ContentType.PAGE);
        setURL(url);
        setTitle(title);
        setDesc(desc);
        setIcon(icon);
    }

    @Override
    public void setURL(String urlString) {
        put("URL", urlString);
    }

    @Override
    public String getURL() {
        return (String) get("URL");
    }

    @Override
    public void setTitle(String text) {
        put("title", text);
    }

    @Override
    public String getTitle() {
        return (String) get("title");
    }

    @Override
    public void setDesc(String text) {
        put("desc", text);
    }

    @Override
    public String getDesc() {
        return (String) get("desc");
    }

    @Override
    public void setIcon(byte[] imageData) {
        icon = imageData;
        if (imageData == null) {
            remove("icon");
        } else {
            put("icon", Base64.encode(imageData));
        }
    }

    @Override
    public byte[] getIcon() {
        if (icon == null) {
            String base64 = (String) get("icon");
            if (base64 != null) {
                icon = Base64.decode(base64);
            }
        }
        return icon;
    }
}
