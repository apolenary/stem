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

package org.stem;

import org.junit.Test;
import org.stem.client.v2.Connection;

import java.net.InetSocketAddress;

public class NewClientTest //extends IntegrationTestBase
{
    @Test
    public void testName() throws Exception
    {
        Connection connection = new Connection(
                new InetSocketAddress("127.0.0.1", 9998),
                new Connection.Factory());

        Thread.sleep(100000);

        connection.close();
    }
}