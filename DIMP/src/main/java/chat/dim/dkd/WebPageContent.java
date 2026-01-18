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

import chat.dim.protocol.ContentType;
import chat.dim.protocol.PageContent;
import chat.dim.protocol.TransportableFile;

/**
 *  Web Page Content
 *
 *  <blockquote><pre>
 *  data format: {
 *      "type" : i2s(0x20),
 *      "sn"   : 123,
 *
 *      "title" : "...",                // Web title
 *      "desc"  : "...",
 *      "icon"  : "data:image/x-icon;base64,...",
 *
 *      "URL"   : "https://github.com/moky/dimp",
 *
 *      "HTML"      : "...",            // Web content
 *      "mime_type" : "text/html",      // Content-Type
 *      "encoding"  : "utf8",
 *      "base"      : "about:blank"     // Base URL
 *  }
 *  </pre></blockquote>
 */
public class WebPageContent extends BaseContent implements PageContent {

    // small image
    private TransportableFile icon;

    // web URL
    private  URI url;

    public WebPageContent(Map<String, Object> content) {
        super(content);
        // lazy load
        icon = null;
        url = null;
    }

    public WebPageContent(String title, TransportableFile icon, String desc,
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
    public TransportableFile getIcon() {
        TransportableFile img = icon;
        if (img == null) {
            Object uri = get("icon");
            img = TransportableFile.parse(uri);
            icon = img;
        }
        return img;
    }

    @Override
    public void setIcon(TransportableFile img) {
        if (img == null || img.isEmpty()) {
            remove("icon");
        } else {
            put("icon", img.serialize());
        }
        icon = img;
    }

    @Override
    public String getDesc() {
        return getString("desc");
    }

    @Override
    public void setDesc(String text) {
        put("desc", text);
    }

    @Override
    public URI getURL() {
        if (url == null) {
            String string = getString("URL");
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
        //assert location != null : "URL cannot be empty";
        put("URL", location == null ? null : location.toString());
        url = location;
    }

    @Override
    public String getHTML() {
        return getString("HTML");
    }

    @Override
    public void setHTML(String html) {
        put("HTML", html);
    }

}
