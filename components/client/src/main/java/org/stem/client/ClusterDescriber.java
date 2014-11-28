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

import com.google.common.base.Throwables;
import org.stem.api.REST;
import org.stem.coordination.ZNode;
import org.stem.coordination.ZookeeperClient;
import org.stem.coordination.ZookeeperEventListener;
import org.stem.coordination.ZookeeperPaths;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class ClusterDescriber {

    final StemCluster.Manager cluster;
    final Notifier notifier;


    private volatile boolean isShutdown;

    private class Notifier {

        final ZookeeperEventListener<REST.TopologySnapshot> stateWatcher;

        public Notifier() {

            stateWatcher = new ZookeeperEventListener<REST.TopologySnapshot>() {
                @Override
                public Class<? extends REST.TopologySnapshot> getBaseClass() {
                    return REST.TopologySnapshot.class;
                }

                @Override
                protected ZNode.Codec codec() {
                    return REST.TopologySnapshot.CODEC;
                }

                @Override
                protected void onNodeUpdated(REST.TopologySnapshot object) {
                    onTopologyChanged(object);
                }
            };
        }

        void start() throws Exception {
            cluster.coordinationClient.listenForZNode(ZookeeperPaths.topologySnapshotPath(), stateWatcher);
        }

        // TODO: stop() {}
    }

    public ClusterDescriber(StemCluster.Manager cluster) {
        this.cluster = cluster;
        this.notifier = new Notifier();
    }

    private void onTopologyChanged(REST.TopologySnapshot state) {
        refreshNodeList(cluster, state.getTopology(), false);
        updateMapping(state.getMapping());
        // TODO: !!!!!!!!!!!
    }

    private void updateMapping(REST.Mapping mapping) {
        cluster.metadata.setMapping(mapping);
    }

    void start() {
        if (isShutdown)
            return;

        try {
            REST.Topology topology = tryReadTopology();
            refreshNodeList(cluster, topology, true);
            // TODO: refreshBucketMap(cluster, true);

            notifier.start();
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    private void refreshNodeList(StemCluster.Manager cluster, REST.Topology topology, boolean isInitialAttempt) {
        List<InetSocketAddress> foundHosts = new ArrayList<InetSocketAddress>();
        List<String> dcs = new ArrayList<String>(); // TODO: implement extraction of dc name
        List<String> racks = new ArrayList<String>(); // TODO: implement extraction of rack name

        for (REST.StorageNode nodeInfo : topology.nodes()) {
            InetSocketAddress addr = nodeInfo.getSocketAddress();
            foundHosts.add(addr);
        }

        for (InetSocketAddress addr : foundHosts) {
            Host host = cluster.metadata.getHost(addr);
            boolean isNew = false;
            if (null == host) {
                host = cluster.metadata.add(addr);
                isNew = true;
            }

            if (isNew && !isInitialAttempt)
                cluster.triggerOnAdd(host);

            // TODO: remove hosts
        }

        // TODO: wait until all pools will be online
        cluster.metadata.setTopology(topology);
    }

    private void refreshNodeList() {
        refreshNodeList(cluster, cluster.metadata.getTopology(), false);
    }

    private REST.Topology tryReadTopology() {
        return readTopology();
    }

    private REST.Topology readTopology() {
        try {
            return zookeeperClient().readZNodeData(ZookeeperPaths.topologyPath(), REST.Topology.class);

        } catch (Exception e) {
            Throwables.propagate(e);
        }
        return null;
    }

    private ZookeeperClient zookeeperClient() {
        return cluster.coordinationClient;
    }

    public CloseFuture closeAsync() {
        isShutdown = true;

        return CloseFuture.immediateFuture();
    }

    public void onUp(Host host) {
    }

    public void onDown(Host host) {

    }

    public void onRemove(Host host) {
        refreshNodeList();
    }

    public void onSuspected(Host host) {

    }

    public void refreshNodeInfo(Host host) {
        // TODO: should or not we to implement it?
    }
}
