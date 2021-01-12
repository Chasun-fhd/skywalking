/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.skywalking.oap.server.receiver.event.listener;

import lombok.RequiredArgsConstructor;
import org.apache.skywalking.apm.network.event.v3.Event;
import org.apache.skywalking.apm.network.event.v3.Source;
import org.apache.skywalking.apm.util.StringUtil;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.analysis.DownSampling;
import org.apache.skywalking.oap.server.core.analysis.IDManager;
import org.apache.skywalking.oap.server.core.analysis.NodeType;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.apache.skywalking.oap.server.core.config.NamingControl;
import org.apache.skywalking.oap.server.core.source.EndpointMeta;
import org.apache.skywalking.oap.server.core.source.ServiceInstanceUpdate;
import org.apache.skywalking.oap.server.core.source.ServiceMeta;
import org.apache.skywalking.oap.server.core.source.SourceReceiver;
import org.apache.skywalking.oap.server.library.module.ModuleManager;

import static java.util.Objects.nonNull;

/**
 * Generate service, service instance and endpoint traffic by event data.
 */
@RequiredArgsConstructor
public class TrafficAnalysisListener implements EventAnalysisListener {
    private final SourceReceiver sourceReceiver;

    private final NamingControl namingControl;

    private ServiceMeta serviceMeta;

    private ServiceInstanceUpdate instanceMeta;

    private EndpointMeta endpointMeta;

    @Override
    public void build() {
        if (nonNull(serviceMeta)) {
            sourceReceiver.receive(serviceMeta);
        }
        if (nonNull(instanceMeta)) {
            sourceReceiver.receive(instanceMeta);
        }
        if (nonNull(endpointMeta)) {
            sourceReceiver.receive(endpointMeta);
        }
    }

    @Override
    public void parse(final Event event) {
        if (!event.hasSource() || StringUtil.isBlank(event.getSource().getService())) {
            return;
        }

        final Source source = event.getSource();

        final long timeBucket = TimeBucket.getTimeBucket(event.getStartTime(), DownSampling.Minute);

        // to service traffic
        final String serviceName = namingControl.formatServiceName(source.getService());
        final String serviceId = IDManager.ServiceID.buildId(serviceName, NodeType.Normal);
        serviceMeta = new ServiceMeta();
        serviceMeta.setName(namingControl.formatServiceName(source.getService()));
        serviceMeta.setNodeType(NodeType.Normal);
        serviceMeta.setTimeBucket(timeBucket);

        // to service instance traffic
        if (StringUtil.isNotEmpty(source.getServiceInstance())) {
            instanceMeta = new ServiceInstanceUpdate();
            instanceMeta.setServiceId(serviceId);
            instanceMeta.setName(namingControl.formatInstanceName(source.getServiceInstance()));
            instanceMeta.setTimeBucket(timeBucket);
        }

        // to endpoint traffic
        if (StringUtil.isNotEmpty(source.getEndpoint())) {
            endpointMeta = new EndpointMeta();
            endpointMeta.setServiceName(serviceName);
            endpointMeta.setServiceNodeType(NodeType.Normal);
            endpointMeta.setEndpoint(namingControl.formatEndpointName(serviceName, source.getEndpoint()));
            endpointMeta.setTimeBucket(timeBucket);
        }
    }

    public static class Factory implements EventAnalysisListener.Factory {
        private final SourceReceiver sourceReceiver;

        private final NamingControl namingControl;

        public Factory(final ModuleManager moduleManager) {
            this.sourceReceiver = moduleManager.find(CoreModule.NAME)
                                               .provider()
                                               .getService(SourceReceiver.class);
            this.namingControl = moduleManager.find(CoreModule.NAME)
                                              .provider()
                                              .getService(NamingControl.class);
        }

        @Override
        public EventAnalysisListener create(final ModuleManager moduleManager) {
            return new TrafficAnalysisListener(sourceReceiver, namingControl);
        }
    }
}
