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

import com.google.common.collect.Lists;

import java.util.List;

public class ZookeeperClientFactory {
    private static List<ZookeeperClient> registry = Lists.newArrayList();

    public static ZookeeperClient create() {
        ZookeeperClient client = new ZookeeperClient();
        saveToRegistry(client);
        return client;
    }

    public static ZookeeperClient create(String host, int port) {
        ZookeeperClient client = new ZookeeperClient(host, port);
        saveToRegistry(client);
        return client;
    }

    public static ZookeeperClient create(String endpoint) {
        ZookeeperClient client = new ZookeeperClient(endpoint);
        saveToRegistry(client);
        return client;
    }

    public static void closeAll() {
        for (ZookeeperClient client : registry) {
            client.close();
        }

        // TODO: clear the list?
    }

    private static void saveToRegistry(ZookeeperClient client) {
        registry.add(client);
    }
}
