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
 *      title : "...",                // Web title
 *      icon  : "...",                // base64_encode(icon)
 *      desc  : "...",
 *
 *      URL   : "https://dim.chat/",  // Web URL
 *
 *      HTML      : "...",            // Web content
 *      mime_type : "text/html",      // Content-Type
 *      encoding  : "utf8",
 *      base      : "about:blank"     // Base URL
 *
 *  }
 */
public class WebPageContent extends BaseContent implements PageContent {

    // small image
    private TransportableData icon;

    // web URL
    private  URI url;

    public WebPageContent(Map<String, Object> content) {
        super(content);
        // lazy load
        icon = null;
        url = null;
    }

    public WebPageContent(String title, TransportableData icon, String desc,
                          URI url, String html) {
        super(ContentType.PAGE);

        setTitle(title);
        setIcon(icon);

        setURL(url);
        setHTML(html);

        setDesc(desc);
    }

    @Override
    public String getTitle() {
        return getString("title", "");
    }

    @Override
    public void setTitle(String text) {
        assert text != null : "Web title cannot be empty";
        put("title", text);
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

    @Override
    public void setIcon(byte[] imageData) {
        TransportableData ted;
        if (imageData == null || imageData.length == 0) {
            ted = null;
        } else {
            ted = TransportableData.create(imageData);
        }
        setIcon(ted);
    }
    private void setIcon(TransportableData ted) {
        if (ted == null) {
            remove("icon");
        } else {
            put("icon", ted.toObject());
        }
        icon = ted;
    }

    @Override
    public String getDesc() {
        return getString("desc", null);
    }

    @Override
    public void setDesc(String text) {
        put("desc", text);
    }

    @Override
    public URI getURL() {
        if (url == null) {
            String string = getString("URL", null);
            if (string != null) {
                url = createURL(string);
            }
            assert url != null : "URL cannot be empty: " + toMap();
        }
        return url;
    }
    protected URI createURL(String string) {
        return URI.create(string);
    }

    @Override
    public void setURL(URI location) {
        assert location != null : "URL cannot be empty";
        put("URL", location.toString());
        url = location;
    }

    @Override
    public String getHTML() {
        return getString("HTML", null);
    }

    @Override
    public void setHTML(String html) {
        put("HTML", html);
    }

}
