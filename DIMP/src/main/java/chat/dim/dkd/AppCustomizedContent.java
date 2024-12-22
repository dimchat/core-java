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

import java.util.Map;

import chat.dim.protocol.ContentType;
import chat.dim.protocol.CustomizedContent;

/**
 *  Application Customized message: {
 *      type : 0xCC,
 *      sn   : 123,
 *
 *      app   : "{APP_ID}",  // application (e.g.: "chat.dim.sechat")
 *      mod   : "{MODULE}",  // module name (e.g.: "drift_bottle")
 *      act   : "{ACTION}",  // action name (3.g.: "throw")
 *      extra : info         // action parameters
 *  }
 */
public class AppCustomizedContent extends BaseContent implements CustomizedContent {

    public AppCustomizedContent(Map<String, Object> content) {
        super(content);
    }

    public AppCustomizedContent(int type, String app, String mod, String act) {
        super(type);
        put("app", app);
        put("mod", mod);
        put("act", act);
    }

    public AppCustomizedContent(String app, String mod, String act) {
        super(ContentType.CUSTOMIZED.value);
        put("app", app);
        put("mod", mod);
        put("act", act);
    }

    //-------- getters --------

    @Override
    public String getApplication() {
        return getString("app", "");
    }

    @Override
    public String getModule() {
        return getString("mod", "");
    }

    @Override
    public String getAction() {
        return getString("act", "");
    }
}
