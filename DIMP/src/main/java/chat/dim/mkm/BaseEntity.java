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
package chat.dim.mkm;

import java.lang.ref.WeakReference;

import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public class BaseEntity implements Entity {

    protected final ID identifier;

    private WeakReference<DataSource> barrackRef = null;

    public BaseEntity(ID id) {
        super();
        identifier = id;
    }

    @Override
    public boolean equals(Object other) {
        if (super.equals(other)) {
            // same object
            return true;
        } else if (other instanceof Entity) {
            // check with ID
            Entity entity = (Entity) other;
            other = entity.getIdentifier();
        }
        return identifier.equals(other);
    }

    @Override
    public String toString() {
        String clazz = getClass().getSimpleName();
        int network = identifier.getAddress().getType();
        return "<" + clazz + " id=\"" + identifier + "\" network=" + network + " />";
    }

    @Override
    public ID getIdentifier() {
        return identifier;
    }

    @Override
    public int getType() {
        return identifier.getType();
    }

    @Override
    public DataSource getDataSource() {
        return barrackRef == null ? null : barrackRef.get();
    }

    @Override
    public void setDataSource(DataSource facebook) {
        barrackRef = facebook == null ? null : new WeakReference<>(facebook);
    }

    @Override
    public Meta getMeta() {
        DataSource barrack = getDataSource();
        assert barrack != null : "entity delegate not set yet";
        return barrack.getMeta(identifier);
    }

    @Override
    public Document getDocument(String type) {
        DataSource barrack = getDataSource();
        assert barrack != null : "entity delegate not set yet";
        return barrack.getDocument(identifier, type);
    }
}
