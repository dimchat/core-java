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
package chat.dim.protocol;

import java.net.URI;

import chat.dim.dkd.WebPageContent;

/**
 *  Web Page
 *
 *  <blockquote><pre>
 *  data format: {
 *      'type' : i2s(0x20),
 *      'sn'   : 123,
 *
 *      'title' : "...",                // Web title
 *      'desc'  : "...",
 *      'icon'  : "data:image/x-icon;base64,...",
 *
 *      'URL'   : "https://github.com/moky/dimp",
 *
 *      'HTML'      : "...",            // Web content
 *      'mime_type' : "text/html",      // Content-Type
 *      'encoding'  : "utf8",
 *      'base'      : "about:blank"     // Base URL
 *  }
 *  </pre></blockquote>
 */
public interface PageContent extends Content {

    void setTitle(String text);
    String getTitle();

    void setIcon(PortableNetworkFile img);
    PortableNetworkFile getIcon();

    void setDesc(String text);
    String getDesc();

    void setURL(URI url);
    URI getURL();

    void setHTML(String html);
    String getHTML();

    //
    //  Factory
    //

    static PageContent create(String title, PortableNetworkFile icon, String desc,
                              URI url, String html) {
        return new WebPageContent(title, icon, desc, url, html);
    }

}
