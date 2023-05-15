package eu.su.mas.dedaleEtu.princ;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agents.GateKeeperAgent;
import eu.su.mas.dedale.mas.agents.dedaleDummyAgents.DummyWumpusShift;
import eu.su.mas.dedaleEtu.mas.agents.dummies.*;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.sid.bdi.BDIAgent;
import eu.su.mas.dedaleEtu.mas.agents.my.Agent_BDI;
import eu.su.mas.dedaleEtu.mas.agents.my.Agent_P3;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is used to start the platform and the agents.
 * To launch your agents in the environment you desire you will have to :
 * <ul>
 * <li> set the ConfigurationFile parameters (and maybe update the files in resources/) {@link ConfigurationFile}</li>
 * <li> create your agents classes and call them in the createAgents method below </li>
 * </ul>
 *
 * @author hc
 */
public class Principal {
    // container's name - container's ref
    private final HashMap<String, ContainerController> containerList;

    public Principal() {
        containerList = new HashMap<>();
    }

    public static void main(String[] args) {
        Principal principal = new Principal();
        if(ConfigurationFile.COMPUTERisMAIN) {
            //We should create the Platform and the GateKeeper, whether the platform is distributed or not
            //1), create the platform (Main container (DF+AMS) + containers + monitoring agents : RMA and SNIFFER)
            principal.emptyPlatform();
        } else {
            //We only have to create the local container and our agents
            //1') If a distant platform already exists, and you want to create and connect your container to it
            principal.createAndConnectContainer(ConfigurationFile.LOCAL_CONTAINER_NAME_MGMT,
                    ConfigurationFile.PLATFORM_HOSTNAME, ConfigurationFile.PLATFORM_ID,
                    ConfigurationFile.PLATFORM_PORT);
        }
        //2) create the gatekeeper (in charge of the environment), the agents, and add them to the platform.
        List<AgentController> agentList = principal.createAgents();
        //3) launch agents (until this point, no behaviour for these agents is started yet)
        principal.startAgents(agentList);
    }

    /**
     * Create an empty platform composed of 1 main container and 3 containers.
     */
    private void emptyPlatform() {
        Runtime rt = Runtime.instance();

        // 1) create a platform (main container+DF+AMS)
        Profile pMain = new ProfileImpl(ConfigurationFile.PLATFORM_HOSTNAME,
                ConfigurationFile.PLATFORM_PORT, ConfigurationFile.PLATFORM_ID);
        System.out.println("Launching a main-container..." + pMain);
        AgentContainer mainContainerRef = rt.createMainContainer(pMain); //DF and AMS are include

        // 2) create the containers
        createContainers(rt);

        // 3) create monitoring agents:
        //    rma agent, used to debug and monitor the platform;
        //    sniffer agent, to monitor communications;
        createMonitoringAgents(mainContainerRef);

        System.out.println("Platform ok");
    }

    /**
     * Create the containers used to hold the agents
     *
     * @param rt The reference to the main container
     * <br/>
     * note: there is a smarter way to find a container with its name, but we go fast to the goal here. Cf jade's doc.
     */
    private void createContainers(Runtime rt) {
        System.out.println("Launching containers ...");

        addOneContainer(rt, ConfigurationFile.LOCAL_CONTAINER_NAME_MGMT);
        addOneContainer(rt, ConfigurationFile.LOCAL_CONTAINER_NAME_AGENTS);

        System.out.println("Launching containers done");
    }

    private void addOneContainer(Runtime rt, String containerName) {
        ProfileImpl pContainer = new ProfileImpl(ConfigurationFile.PLATFORM_HOSTNAME, ConfigurationFile.PLATFORM_PORT, ConfigurationFile.PLATFORM_ID);
        pContainer.setParameter(Profile.CONTAINER_NAME, containerName);
        System.out.println("Launching container " + pContainer);
        containerList.put(containerName, rt.createAgentContainer(pContainer));
    }

    /**
     * @param containerName name of the container as it will appear in the JADE platform
     * @param host          is the IP of the host where the main-container should be listened to. A null value means use the default (i.e. localhost)
     * @param platformID    is the symbolic name of the platform, if different from default. A null value means use the default (i.e. localhost)
     * @param port          (if null, 8888 by default)
     */
    private void createAndConnectContainer(String containerName, String host, String platformID, Integer port) {
        Runtime rti = Runtime.instance();

        if (port == null) {
            port = ConfigurationFile.PLATFORM_PORT;
        }

        System.out.println("Create and Connect container " + containerName + " to the host : " + host + ", platformID: " + platformID + " on port " + port);

        ProfileImpl pContainer = new ProfileImpl(host, port, platformID);
        pContainer.setParameter(Profile.CONTAINER_NAME, containerName);
        AgentContainer containerRef = rti.createAgentContainer(pContainer);

        containerList.put(containerName, containerRef);
    }

    /**
     * create the monitoring agents (rma+sniffer) on the main-container given in parameter and launch them.
     * - RMA agent's is used to debug and monitor the platform;
     * - Sniffer agent is used to monitor communications
     *
     * @param mc the main-container's reference
     */
    private void createMonitoringAgents(ContainerController mc) {
        Assert.assertNotNull(mc);
        System.out.println("Launching the rma agent on the main container ...");

        try {
            AgentController rma = mc.createNewAgent("rma", "jade.tools.rma.rma", new Object[0]);
            rma.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
            System.out.println("Launching of rma agent failed");
        }

        System.out.println("Launching  Sniffer agent on the main container...");

        try {
            AgentController snif = mc.createNewAgent("sniffeur", "jade.tools.sniffer.Sniffer", new Object[0]);
            snif.start();

        } catch (StaleProxyException e) {
            e.printStackTrace();
            System.out.println("launching of sniffer agent failed");
        }
    }

    // Methods used to create the agents and to start them

    /**
     * Creates the agents and add them to the agentList.
     * Agents are NOT started yet at this point.
     *
     * @return the agentList
     */
    private List<AgentController> createAgents() {
        System.out.println("Launching agents...");
        ContainerController c;
        String agentName;
        List<AgentController> agentList = new ArrayList<>();

        if (ConfigurationFile.COMPUTERisMAIN) {
            /*
             * The main is on this computer, we deploy the GateKeeper
             */
            c = containerList.get(ConfigurationFile.LOCAL_CONTAINER_NAME_MGMT);
            Assert.assertNotNull("This container does not exist", c);
            agentName = ConfigurationFile.DEFAULT_GATEKEEPER_NAME;
            try {
                Object[] objtab = new Object[]{ConfigurationFile.ENVIRONMENT_TYPE, ConfigurationFile.GENERATOR_TYPE, ConfigurationFile.INSTANCE_TOPOLOGY, ConfigurationFile.INSTANCE_CONFIGURATION_ELEMENTS, ConfigurationFile.ACTIVE_DIAMOND, ConfigurationFile.ACTIVE_GOLD, ConfigurationFile.ACTIVE_WELL, ConfigurationFile.GENERATOR_PARAMETERS};//used to give informations to the agent
                //Object[] objtab=new Object[]{null,null,ConfigurationFile.ENVIRONMENT_TYPE};//used to give informations to the agent
                System.out.println("GateKeeperAgent.class.getName(): " + GateKeeperAgent.class.getName());
                AgentController ag = c.createNewAgent(agentName, GateKeeperAgent.class.getName(), objtab);

                agentList.add(ag);
                System.out.println(agentName + " launched");
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }

        // The main container (now) exist, we deploy the agent(s) on their local containers
        // They will have to find the gatekeeper's container to deploy themselves in the environment.
        // This is automatically performed by every agent that extends AbstractDedaleAgent

        // ADD YOUR AGENTS HERE - As many as you want.
        // Any agent added here should have its associated configuration available in the entities file
        // or otherwise its behaviour won't be started.
        // The platform will not work unless all agents defined in the entities file are bound by name
        // to agents in this list.
        AgentController[] agentsToAdd = new AgentController[]{
                newAgent("Agent_P3", new String[] {}, Agent_P3.class),
                /*
                newDummyMovingAgent("ImHere"),
                newGolem("Golem1"),
                newGolem("Golem2"),
                newExploreCoopAgent("1stAgent", new String[]{"2ndAgent"}),
                newExploreCoopAgent("2ndAgent", new String[]{"1stAgent"}),
                newDummyMovingAgent("Explo1"),
                newExploreSoloAgent("Explo2"),
                newDummyMovingAgent("Explo3"),
                newCollectorAgent("Collect1"),
                newTankerAgent("Tanker1"),
                */
        };

        for(AgentController ac: agentsToAdd) {
            if(ac != null) {
                // We don't start the agents that are NOT in the entities file
                agentList.add(ac);
            }
        }

        try {
            AgentController nonDedaleAgent =
                    containerList.get(ConfigurationFile.LOCAL_CONTAINER_NAME_AGENTS).createNewAgent(
                            "BDI1", Agent_BDI.class.getName(), new Object[] {});
            agentList.add(nonDedaleAgent);
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

        // All agents created
        System.out.println("Agents created...");
        return agentList;
    }

    private AgentController newAgent(String agentName, Object[] entityParameters,
                                     Class<? extends AbstractDedaleAgent> agentClass,
                                     String containerName) {
        try {
            //1) Get the container where the agent will appear
            ContainerController c = containerList.get(containerName);
            Assert.assertNotNull("This container does not exist",c);
            //2) Give the name of your agent, MUST be the same as the one given in the entities file.
            //3) If you want to give specific parameters to your agent, add them here
            //4) Give the class name of your agent to let the system instantiate it
            return createNewDedaleAgent(c, agentName, agentClass.getName(), entityParameters);
        } catch(RuntimeException re) {
            System.out.println("Loading " + agentName + " of class " + agentClass.getName() + " has failed.");
            System.out.println("Quite likely the agent was not included in the configuration file. Error:");
            System.out.println(re.getMessage());
            re.printStackTrace();
            return null;
        }
    }

    private AgentController newAgent(String agentName, Object[] entityParameters,
                                     Class<? extends AbstractDedaleAgent> agentClass) {
        return newAgent(agentName, entityParameters, agentClass, ConfigurationFile.LOCAL_CONTAINER_NAME_AGENTS);
    }

    private AgentController newTankerAgent(String agentName) {
        return newAgent(agentName, new Object[] {"My parameters"}, DummyTankerAgent.class);
    }

    private AgentController newCollectorAgent(String agentName) {
        return newAgent(agentName, new Object[] {"My parameters"}, DummyCollectorAgent.class);
    }

    private AgentController newExploreSoloAgent(String agentName) {
        return newAgent(agentName, new Object[] {"My parameters"}, ExploreSoloAgent.class);
    }

    private AgentController newDummyMovingAgent(String agentName) {
        return newAgent(agentName, new Object[] {"My parameters"}, DummyMovingAgent.class);
    }

    private AgentController newExploreCoopAgent(String agentName, String[] agentNamesToShare) {
        // agentNamesToShare is a list of agent names
        // this agent will share its internal exploration map with
        return newAgent(agentName, agentNamesToShare, ExploreCoopAgent.class);
    }

    private AgentController newGolem(String agentName) {
        return newAgent(agentName, new Object[] {"My parameters"}, DummyWumpusShift.class);
    }

    /**
     * Start the agents
     *
     * @param agentList List of agents started in the platform
     */
    private void startAgents(List<AgentController> agentList) {
        System.out.println("Starting agents...");

        for (final AgentController ac : agentList) {
            try {
                ac.start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Agents started...");
    }

    /**
     * @param initialContainer      container where to deploy the agent
     * @param agentName             name of the agent
     * @param className             class of the agent
     * @param additionalParameters  arguments that the agent will receive on creation
     */
    private AgentController createNewDedaleAgent(ContainerController initialContainer, String agentName,
                                                 String className, Object[] additionalParameters) {
        Object[] objtab = AbstractDedaleAgent.loadEntityCaracteristics(agentName,
                ConfigurationFile.INSTANCE_CONFIGURATION_ENTITIES);
        Object[] res2 = merge(objtab, additionalParameters);

        AgentController ag = null;
        try {
            ag = initialContainer.createNewAgent(agentName, className, res2);
        } catch (StaleProxyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Assert.assertNotNull(ag);
        System.out.println(agentName + " launched");
        return ag;
    }

    /**
     * tab2 is added at the end of tab1
     *
     * @param tab1 Head part of the concatenation
     * @param tab2 Tail part of the concatenation
     */
    private static Object[] merge(Object[] tab1, Object[] tab2) {
        Assert.assertNotNull(tab1);
        Object[] res;
        if (tab2 != null) {
            res = new Object[tab1.length + tab2.length];
            int i = 0;
            for (; i < tab1.length; i++) {
                res[i] = tab1[i];
            }
            for (Object o : tab2) {
                res[i] = o;
                i++;
            }
        } else {
            res = tab1;
        }
        return res;
    }
}
