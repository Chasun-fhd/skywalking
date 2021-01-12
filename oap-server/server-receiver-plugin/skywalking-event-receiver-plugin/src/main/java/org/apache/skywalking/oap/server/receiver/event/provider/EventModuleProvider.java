/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.receiver.event.provider;

import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.server.GRPCHandlerRegister;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleDefine;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;
import org.apache.skywalking.oap.server.library.module.ServiceNotProvidedException;
import org.apache.skywalking.oap.server.receiver.event.EventAnalyzerModuleConfig;
import org.apache.skywalking.oap.server.receiver.event.handler.grpc.EventServiceHandler;
import org.apache.skywalking.oap.server.receiver.event.listener.RecordAnalysisListener;
import org.apache.skywalking.oap.server.receiver.event.listener.TrafficAnalysisListener;
import org.apache.skywalking.oap.server.receiver.event.module.EventModule;
import org.apache.skywalking.oap.server.receiver.event.service.EventAnalysisService;
import org.apache.skywalking.oap.server.receiver.event.service.EventAnalysisServiceImpl;
import org.apache.skywalking.oap.server.receiver.sharing.server.SharingServerModule;

public class EventModuleProvider extends ModuleProvider {

    private EventAnalysisServiceImpl eventAnalysisService;

    @Override
    public String name() {
        return "default";
    }

    @Override
    public Class<? extends ModuleDefine> module() {
        return EventModule.class;
    }

    @Override
    public ModuleConfig createConfigBeanIfAbsent() {
        return new EventAnalyzerModuleConfig();
    }

    @Override
    public void prepare() throws ServiceNotProvidedException {
        eventAnalysisService = new EventAnalysisServiceImpl(getManager());
        this.registerServiceImplementation(EventAnalysisService.class, eventAnalysisService);
    }

    @Override
    public void start() {
        eventAnalysisService.add(new RecordAnalysisListener.Factory(getManager()));
        eventAnalysisService.add(new TrafficAnalysisListener.Factory(getManager()));

        final GRPCHandlerRegister grpcHandlerRegister = getManager().find(SharingServerModule.NAME)
                                                                    .provider()
                                                                    .getService(GRPCHandlerRegister.class);
        final EventServiceHandler eventServiceHandler = new EventServiceHandler(getManager());
        grpcHandlerRegister.addHandler(eventServiceHandler);
    }

    @Override
    public void notifyAfterCompleted() {

    }

    @Override
    public String[] requiredModules() {
        return new String[] {
            CoreModule.NAME,
            SharingServerModule.NAME
        };
    }

}
