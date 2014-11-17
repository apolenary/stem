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

import org.stem.api.REST;
import org.stem.domain.Cluster;

public class StorageStatListener extends ZookeeperEventListener<REST.StorageNode> {

    @Override
    public Class<REST.StorageNode> getBaseClass() {
        return REST.StorageNode.class;
    }

    @Override
    protected void onChildAdded(REST.StorageNode stat) {
        Cluster.instance.updateStat(stat);
    }

    @Override
    protected void onChildUpdated(REST.StorageNode stat) {
        Cluster.instance.updateStat(stat);
    }

    @Override
    protected void onChildRemoved(REST.StorageNode stat) {
        // TODO:
    }

    @Override
    protected void onError(Throwable t) {
    }
}
