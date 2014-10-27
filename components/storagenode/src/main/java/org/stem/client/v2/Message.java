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

package org.stem.client.v2;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class Message {

    protected static final Logger logger = LoggerFactory.getLogger(Message.class);
    private volatile int streamId;

    protected Message() {
    }

    public Message setStreamId(int streamId) {
        this.streamId = streamId;
        return this;
    }

    public int getStreamId() {
        return streamId;
    }

    public interface Coder<R extends Request> {

        public ByteBuf encode(R request);

        public R decode(ByteBuf buf);
    }

    public interface Decoder<R extends Response> {

    }

    public static abstract class Request extends Message {

    }

    public static abstract class Response extends Message {

    }
}
