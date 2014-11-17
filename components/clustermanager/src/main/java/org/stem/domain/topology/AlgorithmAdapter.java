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

package org.stem.domain.topology;

import java.util.List;
import java.util.Map;

/**
 * @param <BUCKET_IN>    Bucket type used by application
 * @param <BUCKET_OUT>   Bucket type used by algorithm
 * @param <NODE_IN>      Node type used in application
 * @param <NODE_OUT>     Internal node type of algorithm
 * @param <TOPOLOGY_IN>  Topology type used in application (Topology by default)
 * @param <TOPOLOGY_OUT> Topology type used by Algorithm
 */
public interface AlgorithmAdapter<
        BUCKET_IN, BUCKET_OUT,
        NODE_IN, NODE_OUT,
        REPLICA_SET_IN, REPLICA_SET_OUT,
        TOPOLOGY_IN, TOPOLOGY_OUT> {

    BUCKET_OUT convertBucket(BUCKET_IN src);
    NODE_OUT convertNode(NODE_IN src);
    REPLICA_SET_OUT convertReplicaSet(REPLICA_SET_IN src);
    TOPOLOGY_OUT convertTopology(TOPOLOGY_IN src);
    public Map<BUCKET_OUT, Topology.ReplicaSet> computeMapping(List<BUCKET_IN> dataset, int rf, TOPOLOGY_IN topology);
}
