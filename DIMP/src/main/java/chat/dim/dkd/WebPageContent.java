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

    // web URL
    private  URI url;
    // small image
    private TransportableData icon;

    public WebPageContent(Map<String, Object> content) {
        super(content);
        // lazy load
        url = null;
        icon = null;
    }

    public WebPageContent(URI url, String title, String desc, byte[] icon) {
        super(ContentType.PAGE);
        setURL(url);
        setTitle(title);
        setDesc(desc);
        setIcon(icon);
    }

    @Override
    public void setURL(URI location) {
        assert location != null : "URL cannot be empty";
        put("URL", location.toString());
        url = location;
    }

    @Override
    public URI getURL() {
        if (url == null) {
            String string = getString("URL", null);
            if (string != null) {
                url = URI.create(string);
            }
            assert url != null : "URL cannot be empty: " + toMap();
        }
        return url;
    }

    @Override
    public void setTitle(String text) {
        assert text != null : "Web title cannot be empty";
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
        if (imageData == null || imageData.length == 0) {
            remove("icon");
            icon = null;
        } else {
            TransportableData ted = TransportableData.create(imageData);
            put("icon", ted.toObject());
            icon = ted;
        }
    }

    @Override
    public byte[] getIcon() {
        TransportableData ted = icon;
        if (ted == null) {
            Object base64 = get("icon");
            icon = ted = TransportableData.parse(base64);
        }
        return ted == null ? null : ted.getData();
    }
}
