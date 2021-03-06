/*
 * Copyright 2014 Alexey Plotnik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stem.coordination;

import org.stem.utils.JsonUtils;

public abstract class ZNodeAbstract implements ZNode {

    protected static final Codec JSON_CODEC = new JsonCodec();

    // TODO: Make these methods static
    @Override
    public byte[] encode() {
        return codec().encode(formingEntity());
    }

    protected Codec codec() {
        return JSON_CODEC;
    }

    protected Object formingEntity() {
        return this;
    }

    private static class JsonCodec implements ZNode.Codec {

        @Override
        public byte[] encode(Object obj) {
            return JsonUtils.encodeBytes(obj);
        }

        @Override
        public <T extends ZNode> T decode(byte[] raw, Class<T> clazz) {
            return JsonUtils.decode(raw, clazz);
        }

//        @Override
//        public ZNode decode(byte[] raw, Class<? extends ZNode> clazz) {
//            return JsonUtils.decode(raw, clazz);
//        }
    }
}
