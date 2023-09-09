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

import java.net.URI;
import java.util.Map;

import chat.dim.format.TransportableData;
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

    private TransportableData icon = null;

    public WebPageContent(Map<String, Object> content) {
        super(content);
    }

    public WebPageContent(URI url, String title, String desc, byte[] icon) {
        super(ContentType.PAGE);
        setURL(url);
        setTitle(title);
        setDesc(desc);
        setIcon(icon);
    }

    @Override
    public void setURL(URI url) {
        if (url == null) {
            remove("URL");
        } else {
            put("URL", url.toString());
        }
    }

    @Override
    public URI getURL() {
        String url = getString("URL", null);
        return url == null ? null : URI.create(url);
    }

    @Override
    public void setTitle(String text) {
        put("title", text);
    }

    @Override
    public String getTitle() {
        return getString("title", null);
    }

    @Override
    public void setDesc(String text) {
        put("desc", text);
    }

    @Override
    public String getDesc() {
        return getString("desc", null);
    }

    @Override
    public void setIcon(byte[] imageData) {
        if (imageData != null && imageData.length > 0) {
            TransportableData ted = TransportableData.create(imageData);
            put("icon", ted.toObject());
            icon = ted;
        } else {
            remove("icon");
            icon = null;
        }
    }

    @Override
    public byte[] getIcon() {
        TransportableData ted = icon;
        if (ted == null) {
            String base64 = getString("icon", null);
            icon = ted = TransportableData.parse(base64);
        }
        return ted == null ? null : ted.getData();
    }
}
