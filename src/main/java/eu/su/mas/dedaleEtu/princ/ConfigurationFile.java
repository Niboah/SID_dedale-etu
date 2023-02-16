package eu.su.mas.dedaleEtu.princ;

import eu.su.mas.dedale.env.EnvironmentType;
import eu.su.mas.dedale.env.GeneratorType;

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
    public static final boolean PLATFORMisDISTRIBUTED = false;
    public static final boolean COMPUTERisMAIN = true;

    // network configuration
    public static final String PLATFORM_HOSTNAME = "127.0.0.1";
    public static final String PLATFORM_ID = "SIDPlatform";
    public static final Integer PLATFORM_PORT = 8887;

    // List of containers to be created on the current computer
    public static final String LOCAL_CONTAINER_NAME_MGMT = PLATFORM_ID + "_" + "container1";
    public static final String LOCAL_CONTAINER_NAME_AGENTS = PLATFORM_ID + "_" + "container2";

    // 2) Environment parameters

    /**
     * The environment is either a GraphStream (2D discrete) or JME (3D continuous)
     * one.
     */
    public static final EnvironmentType ENVIRONMENT_TYPE = EnvironmentType.GS;

    /**
     * The environment is either manually designed, or generated with a specific
     * generator
     */
    public static final GeneratorType GENERATOR_TYPE = GeneratorType.MANUAL;

    /**
     * The GateKeeper is in charge of the Platform and of the agents within, do not
     * change its name.
     */
    public static final String DEFAULT_GATEKEEPER_NAME = "GK";

    // 2-a) Environment parameters when the environment is loaded. We need : - a
    // topology, - the configuration of the elements on the map,

    // These parameters must be empty if the environment is generated or already
    // online

    /**
     * Set the topology (e.g. the graph defining the environment map).
     * Some hardcoded examples for convenience, as private fields.
     * The field that is used by the platform is INSTANCE_TOPOLOGY,
     * at the end of this section.
     */
    private static final String INSTANCE_TOPOLOGY_2020_EXAM1 =
            "resources/topology/map2020-topologyExam1-graph.dgs";
    private static final String INSTANCE_TOPOLOGY_HOUAT =
            "resources/topology/HouatTopology";
    private static final String INSTANCE_TOPOLOGY_2018_ICA =
            "resources/topology/map2018-topology-ica";
    private static final String INSTANCE_TOPOLOGY_2021_EXAM1_TREE =
            "resources/topology/map2021-topologyExam1-tree.dgs";
    private static final String INSTANCE_TOPOLOGY_INTERLOCKING =
            "resources/interlocking/mapInterlocking2-topology";
    private static final String INSTANCE_TOPOLOGY_2021_EXAM1_GRAPH =
            "resources/topology/map2021-topologyExam1-graph.dgs";
    private static final String INSTANCE_TOPOLOGY_2018 =
            "resources/topology/map2018-topology";
    private static final String INSTANCE_TOPOLOGY_2019 =
            "resources/map2019-topologyExam1";
    public static final String INSTANCE_TOPOLOGY = INSTANCE_TOPOLOGY_2018;

    /**
     * Give the elements available on the map, if any
     */
    // If the environment is loaded, but you do not want to define elements
    // on the map
    private static final String INSTANCE_CONFIGURATION_ELEMENTS_EMPTY =
            "resources/distributedExploration/emptyMap";
    private static final String INSTANCE_CONFIGURATION_ELEMENTS_2019_EXAM1 =
            "resources/treasureHunt/map2019-elementsExam1";
    private static final String INSTANCE_CONFIGURATION_ELEMENTS_HOUAT =
            "resources/treasureHunt/Houat-elements";
    private static final String INSTANCE_CONFIGURATION_ELEMENTS_2018_ICA =
            "resources/treasureHunt/map2018-elements-ica";
    private static final String INSTANCE_CONFIGURATION_ELEMENTS_INTERLOCKING =
            "resources/interlocking/mapInterlocking2-elements";
    private static final String INSTANCE_CONFIGURATION_ELEMENTS_2018 =
            "resources/treasureHunt/map2018-elements";
    public static final String INSTANCE_CONFIGURATION_ELEMENTS =
            INSTANCE_CONFIGURATION_ELEMENTS_2018;

    // 2-b) Environment parameters when it is generated

    /**
     * Size of the generated environment, mandatory
     */
    // Parameters required for some generators (see dedale.gitlab.io)
    public static final Integer ENVIRONMENT_SIZE = 10;
    // used by the BARABASI_ALBERT generator to know
    // the number of childs
    public static final Integer OPTIONAL_ADDITIONAL_ENVGENERATOR_PARAM1 = 1;
    public static final Integer[] GENERATOR_PARAMETERS =
            {ENVIRONMENT_SIZE, OPTIONAL_ADDITIONAL_ENVGENERATOR_PARAM1};

    /**
     * Wumpus proximity detection radius
     */
    public static final Integer DEFAULT_DETECTION_RADIUS = 1;

    /**
     * Agents communication radius
     */
    public static final Integer DEFAULT_COMMUNICATION_REACH = 3;

    /**
     * Elements on the map
     */

    public static final boolean ACTIVE_WELL = false;
    public static final boolean ACTIVE_GOLD = true;
    public static final boolean ACTIVE_DIAMOND = false;

    // 3) Agents characteristics

    /**
     * Entities definition maps.
     * They are files that contain a description of elements present in the environment.
     * Some examples hardcoded as private fields for convenience.
     * The field that will be used by the platform is INSTANCE_CONFIGURATION_ENTITIES,
     * at the end of this section.
     * Mustn't be null as it describes the native agents' capabilities
     */
    private static final String INSTANCE_CONFIGURATION_ENTITIES_EMPTY = null;
    private static final String INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLO_3 =
            "resources/agentExplo-3w";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLO_2 =
            "resources/agentExplo-2";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLOCOOP_2 =
            "resources/agentExploCoop-2";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLOCOOP_2_JSON =
            "resources/agentExploCoop-2.json";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_AGENT_KEYBOARD =
            "resources/agentKeyboardControlled";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_2020_TREE =
            "resources/hunt/map2020-entitiesTree";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_2021_TREE =
            "resources/hunt/map2021-entitiesTree";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_2020_GRAPH =
            "resources/hunt/map2020-entitiesGraph";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_2021_GRAPH =
            "resources/hunt/map2021-entitiesGraph";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_2018_2 =
            "resources/map2018-entities2";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_2019 =
            "resources/map2019-entitiesExam1";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_MONO =
            "resources/monoAgent-entities";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_INTERLOCKING =
            "resources/mapInterlocking2-entities";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLO_2018 =
            "src/test/java/resources/map2018-agentExplo";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLO_SOLO =
            "src/test/java/resources/agentExploSolo";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_2018_TANKER =
            "src/test/java/resources/map2018-agentTanker";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_2018_COLLECT =
            "src/test/java/resources/map2018-agentCollect";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_2018 =
            "src/test/java/resources/map2018-entities";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_2018_GOLEM =
            "src/test/java/resources/map2018-agentGolem";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_2018_TANKER_COLLECT =
            "src/test/java/resources/map2018-agentTankerCollect";
    private static final String INSTANCE_CONFIGURATION_ENTITIES_2019_EXAM1 =
            "src/test/java/resources/map2019-entitiesExam1";
    public static final String INSTANCE_CONFIGURATION_ENTITIES =
            INSTANCE_CONFIGURATION_ENTITIES_AGENTEXPLOCOOP_2_JSON;
}
