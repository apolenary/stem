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

import java.util.concurrent.ExecutionException;

public class StemClientInternalError extends StemClientException {
    public StemClientInternalError(String message) {
        super(message);
    }

    public StemClientInternalError(Throwable cause) {
        super(cause);
    }

    public StemClientInternalError(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public StemClientException copy() {
        return new StemClientInternalError(getMessage(), this);
    }
}