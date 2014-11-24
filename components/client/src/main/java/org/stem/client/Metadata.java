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

package org.stem.client;

import org.stem.api.REST;
import org.stem.api.response.ClusterResponse;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Metadata {

    private StemCluster.Manager cluster;
    private REST.Cluster descriptor;
    private REST.Topology topology;

    public REST.Topology getTopology() {
        return topology;
    }

    private final ConcurrentMap<InetSocketAddress, Host> hosts = new ConcurrentHashMap<InetSocketAddress, Host>();

    public Metadata(StemCluster.Manager cluster) {
        this.cluster = cluster;
    }

    Collection<Host> allHosts() {
        return hosts.values();
    }

    public Host getHost(InetSocketAddress addr) {
        return hosts.get(addr);
    }

    public Host add(InetSocketAddress addr) {
        Host newHost = new Host(addr);
        Host previous = hosts.putIfAbsent(addr, newHost);
        return null == previous ? newHost : null;
    }

    public boolean remove(Host host) {
        return false;
    }

    public void setDescriptor(REST.Cluster descriptor) {
        this.descriptor = descriptor;
    }
}
