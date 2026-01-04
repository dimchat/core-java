/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Albert Moky
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
package chat.dim.format;

import java.util.Map;

import chat.dim.data.Converter;
import chat.dim.type.Mapper;

public abstract class BaseNetworkFormatWrapper {

    private final Map<String, Object> dictionary;

    protected BaseNetworkFormatWrapper(Map<String, Object> map) {
        super();
        if (map instanceof Mapper) {
            map = ((Mapper) map).toMap();
        }
        dictionary = map;
    }

    public Map<String, Object> toMap() {
        return dictionary;
    }

    public boolean isEmpty() {
        return dictionary.isEmpty();
    }

    public Object get(String key) {
        return dictionary.get(key);
    }

    public Object put(String key, Object value) {
        return dictionary.put(key, value);
    }

    public Object remove(String key) {
        return dictionary.remove(key);
    }

    public String getString(String key) {
        return Converter.getString(dictionary.get(key));
    }

    public void setMap(String key, Mapper mapper) {
        if (mapper == null) {
            remove(key);
        } else {
            put(key, mapper.toMap());
        }
    }

}
