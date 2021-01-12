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

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.apache.skywalking.apm.network.event.v3.Source;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.analysis.worker.MetricsStreamProcessor;
import org.apache.skywalking.oap.server.core.config.NamingControl;
import org.apache.skywalking.oap.server.core.event.Event;
import org.apache.skywalking.oap.server.library.module.ModuleManager;

/**
 * RecordAnalysisListener forwards the event data to the persistence layer with the query required conditions.
 */
@RequiredArgsConstructor
public class RecordAnalysisListener implements EventAnalysisListener {
    private static final Gson GSON = new Gson();

    private final NamingControl namingControl;

    private final Event event = new Event();

    @Override
    public void build() {
        MetricsStreamProcessor.getInstance().in(event);
    }

    @Override
    public void parse(final org.apache.skywalking.apm.network.event.v3.Event e) {
        event.setUuid(e.getUuid());

        if (e.hasSource()) {
            final Source source = e.getSource();
            event.setService(namingControl.formatServiceName(source.getService()));
            event.setServiceInstance(namingControl.formatInstanceName(source.getServiceInstance()));
            event.setEndpoint(namingControl.formatEndpointName(source.getService(), source.getEndpoint()));
        }

        event.setName(e.getName());
        event.setType(e.getType().name());
        event.setMessage(e.getMessage());
        event.setParameters(GSON.toJson(e.getParametersMap()));
        event.setStartTime(e.getStartTime());
        event.setEndTime(e.getEndTime());
    }

    public static class Factory implements EventAnalysisListener.Factory {
        private final NamingControl namingControl;

        public Factory(final ModuleManager moduleManager) {
            this.namingControl = moduleManager.find(CoreModule.NAME)
                                              .provider()
                                              .getService(NamingControl.class);
        }

        @Override
        public EventAnalysisListener create(final ModuleManager moduleManager) {
            return new RecordAnalysisListener(namingControl);
        }
    }
}
