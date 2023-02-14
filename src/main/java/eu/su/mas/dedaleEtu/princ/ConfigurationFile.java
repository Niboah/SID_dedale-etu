package eu.su.mas.dedaleEtu.princ;

import eu.su.mas.dedale.env.EnvironmentType;
import eu.su.mas.dedale.env.GeneratorType;

import java.util.Arrays;

/**
 * Configuration file for a Dedale instance
 * 1) Network and platform parameters
 * 2) Environment parameters
 *
 * @author hc
 */
public final class ConfigurationFile {
    // 1) Network and platform parameters

    // Distributed or not, and is the current computer in charge of the
    // main-container
    public static boolean PLATFORMisDISTRIBUTED = false;
    public static boolean COMPUTERisMAIN = true;

    // network configuration
    public static String PLATFORM_HOSTNAME = "127.0.0.1";
    public static String PLATFORM_ID = "SIDPlatform";
    public static Integer PLATFORM_PORT = 8887;

    // List of containers to be created on the current computer
    public static String[] LOCAL_CONTAINER_NAMES = new String[] {
            PLATFORM_ID + "_container1",
            PLATFORM_ID + "_container2",
            PLATFORM_ID + "_container3",
            PLATFORM_ID + "_container4"
    };

    // 2) Environment parameters

    /**
     * The environment is either a GraphStream (2D discrete) or JME (3D continuous)
     * one.
     */
    public static EnvironmentType ENVIRONMENT_TYPE = EnvironmentType.GS;

    /**
     * The environment is either manually designed, or generated with a specific
     * generator
     */
    public static GeneratorType GENERATOR_TYPE = GeneratorType.MANUAL;

    /**
     * The GateKeeper is in charge of the Platform and of the agents within, do not
     * change its name.
     */
    public static String DEFAULT_GATEKEEPER_NAME = "GK";

    // 2-a) Environment parameters when the environment is loaded. We need : - a
    // topology, - the configuration of the elements on the map,

    // These parameters must be empty if the environment is generated or already
    // online

    /**
     * Give the topology
     */
    public static String
            INSTANCE_TOPOLOGY_2020_EXAM1 =
            "resources/topology/map2020-topologyExam1-graph.dgs";
    public static String INSTANCE_TOPOLOGY_HOUAT =
            "resources/topology/HouatTopology";
    public static String INSTANCE_TOPOLOGY_2018_ICA =
            "resources/topology/map2018-topology-ica";
    public static String
            INSTANCE_TOPOLOGY_2021_EXAM1_TREE =
            "resources/topology/map2021-topologyExam1-tree.dgs";
    public static String
            INSTANCE_TOPOLOGY_INTERLOCKING =
            "resources/interlocking/mapInterlocking2-topology";
    public static String
            INSTANCE_TOPOLOGY_2021_EXAM1_GRAPH =
            "resources/topology/map2021-topologyExam1-graph.dgs";
    public static String INSTANCE_TOPOLOGY_2018 =
            "resources/map2018-topology";
    public static String INSTANCE_TOPOLOGY_2019 =
            "resources/map2019-topologyExam1";
    public static String INSTANCE_TOPOLOGY = INSTANCE_TOPOLOGY_2018;

    /**
     * Give the elements available on the map, if any
     */
    // If the environment is loaded, but you do not want to define elements
    // on the map
    public static String INSTANCE_CONFIGURATION_ELEMENTS_EMPTY =
            "resources/distributedExploration/emptyMap";
    public static String INSTANCE_CONFIGURATION_ELEMENTS_2019_EXAM1 =
            "resources/treasureHunt/map2019-elementsExam1";
    public static String INSTANCE_CONFIGURATION_ELEMENTS_HOUAT =
            "resources/treasureHunt/Houat-elements";
    public static String INSTANCE_CONFIGURATION_ELEMENTS_2018_ICA =
            "resources/treasureHunt/map2018-elements-ica";
    public static String INSTANCE_CONFIGURATION_ELEMENTS_INTERLOCKING =
            "resources/interlocking/mapInterlocking2-elements";
    public static String INSTANCE_CONFIGURATION_ELEMENTS_2018 =
            "resources/map2018-elements";
    public static String INSTANCE_CONFIGURATION_ELEMENTS =
            INSTANCE_CONFIGURATION_ELEMENTS_2018;

    // 2-b) Environment parameters when it is generated

    /**
     * Size of the generated environment, mandatory
     */
    // Parameters required for some generators (see dedale.gitlab.io)
    public static Integer ENVIRONMENT_SIZE = 10;
    // used by the BARABASI_ALBERT generator to know
    // the number of childs
    public static Integer OPTIONAL_ADDITIONAL_ENVGENERATOR_PARAM1 = 1;
    public static Integer[] GENERATOR_PARAMETERS =
            {ENVIRONMENT_SIZE, OPTIONAL_ADDITIONAL_ENVGENERATOR_PARAM1};

    /**
     * Wumpus proximity detection radius
     */
    public static final Integer DEFAULT_DETECTION_RADIUS = 1;

    /**
     * Agents communication radius
     */
    public static Integer DEFAULT_COMMUNICATION_REACH = 3;

    /**
     * Elements on the map
     */

    public static boolean ACTIVE_WELL = false;
    public static boolean ACTIVE_GOLD = true;
    public static boolean ACTIVE_DIAMOND = false;

    // 3) Agents characteristics

    /**
     * Mustn't be null as it describes the native agents' capabilities
     */
    public static String INSTANCE_CONFIGURATION_ENTITIES_EMPTY = null;
    public static String INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLO_3 =
            "resources/agentExplo-3w";
    public static String INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLO_2 =
            "resources/agentExplo-2";
    public static String INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLOCOOP_2 =
            "resources/agentExploCoop-2";
    public static String INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLOCOOP_2_JSON =
            "resources/agentExploCoop-2.json";
    public static String INSTANCE_CONFIGURATION_ENTITIES_AGENT_KEYBOARD =
            "resources/agentKeyboardControlled";
    public static String INSTANCE_CONFIGURATION_ENTITIES_2020_TREE =
            "resources/hunt/map2020-entitiesTree";
    public static String INSTANCE_CONFIGURATION_ENTITIES_2021_TREE =
            "resources/hunt/map2021-entitiesTree";
    public static String INSTANCE_CONFIGURATION_ENTITIES_2020_GRAPH =
            "resources/hunt/map2020-entitiesGraph";
    public static String INSTANCE_CONFIGURATION_ENTITIES_2021_GRAPH =
            "resources/hunt/map2021-entitiesGraph";
    public static String INSTANCE_CONFIGURATION_ENTITIES_2018_2 =
            "resources/map2018-entities2";
    public static String INSTANCE_CONFIGURATION_ENTITIES_2019 =
            "resources/map2019-entitiesExam1";
    public static String INSTANCE_CONFIGURATION_ENTITIES_MONO =
            "resources/monoAgent-entities";
    public static String INSTANCE_CONFIGURATION_ENTITIES_INTERLOCKING =
            "resources/mapInterlocking2-entities";
    public static String INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLO_2018 =
            "src/test/java/resources/map2018-agentExplo";
    public static String INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLO_SOLO =
            "src/test/java/resources/agentExploSolo";
    public static String INSTANCE_CONFIGURATION_ENTITIES_2018_TANKER =
            "src/test/java/resources/map2018-agentTanker";
    public static String INSTANCE_CONFIGURATION_ENTITIES_2018_COLLECT =
            "src/test/java/resources/map2018-agentCollect";
    public static String INSTANCE_CONFIGURATION_ENTITIES_2018 =
            "src/test/java/resources/map2018-entities";
    public static String INSTANCE_CONFIGURATION_ENTITIES_2018_GOLEM =
            "src/test/java/resources/map2018-agentGolem";
    public static String INSTANCE_CONFIGURATION_ENTITIES_2018_TANKER_COLLECT =
            "src/test/java/resources/map2018-agentTankerCollect";
    public static String INSTANCE_CONFIGURATION_ENTITIES_2019_EXAM1 =
            "src/test/java/resources/map2019-entitiesExam1";
    public static String INSTANCE_CONFIGURATION_ENTITIES =
            INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLOCOOP_2_JSON;
}
