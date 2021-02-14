package eu.su.mas.dedaleEtu.princ;

import eu.su.mas.dedale.env.EnvironmentType;

/**
 * Configuration file for a Dedale instance 
 * 
 * @author hc
 *
 */
public final class ConfigurationFile {

	/******************************
	 * 
	 * PLATEFORM configuration
	 * 
	 *****************************/

	public static boolean PLATFORMisDISTRIBUTED= false;
	public static boolean COMPUTERisMAIN= true;

	public static String PLATFORM_HOSTNAME="127.0.0.1";
	public static String PLATFORM_ID="Ithaq";
	public static Integer PLATFORM_PORT=8887;
	
	public static String LOCAL_CONTAINER_NAME=PLATFORM_ID+"_"+"container1";
	public static String LOCAL_CONTAINER2_NAME=PLATFORM_ID+"_"+"container2";
	public static String LOCAL_CONTAINER3_NAME=PLATFORM_ID+"_"+"container3";
	public static String LOCAL_CONTAINER4_NAME=PLATFORM_ID+"_"+"container4";
	
	
	/**
	 * Required by the environment class to be able to load it. Currently, only one type of environment is available
	 */
	public static EnvironmentType ENVIRONMENT_TYPE=EnvironmentType.GS;
	
	/**
	 * Name of the agent in charge of the environment E/S
	 */
	public static String DEFAULT_GATEKEEPER_NAME="GK";
	
	
	/******************************
	 * 
	 * ENVIRONMENT configuration
	 * 
	 * When the environment is loaded we need :
	 *  - a topology, 
	 *  - the configuration of the elements on the map,
	 *  - and the characteristics of the agents.
	 *
	 *  The two first parameters must be null if the environment is generated or already online 
	 * 
	 *****************************/
	
	
	/**
	 * Not null when the environment is loaded; should be null if the environment is generated or already online
	 */
	//public static String INSTANCE_TOPOLOGY=null;
	
	//public static String INSTANCE_TOPOLOGY="resources/topology/map2020-topologyExam1-graph.dgs";
	//public static String INSTANCE_TOPOLOGY="resources/topology/HouatTopology";
	public static String INSTANCE_TOPOLOGY="resources/topology/map2018-topology-ica";
	//public static String INSTANCE_TOPOLOGY="resources/topology/map2020-topologyExam1-tree.dgs";
	//public static String INSTANCE_TOPOLOGY="resources/interlocking/mapInterlocking2-topology";
	//public static String INSTANCE_TOPOLOGY="resources/map2018-topology";
	//public static String INSTANCE_TOPOLOGY="resources/map2019-topologyExam1";

	
	/**
	 * Not null when the environment is loaded; should be null if the environment is generated or already online
	 */
	// If the element is generated
	//public static String INSTANCE_CONFIGURATION_ELEMENTS=null;
	
	// If the environment is loaded but you do not want to define elements on the map
	public static String INSTANCE_CONFIGURATION_ELEMENTS="resources/distributedExploration/emptyMap";
	
	//public static String INSTANCE_CONFIGURATION_ELEMENTS="resources/treasureHunt/map2019-elementsExam1";
	//public static String INSTANCE_CONFIGURATION_ELEMENTS="resources/treasureHunt/Houat-elements";
	
	
	//public static String INSTANCE_CONFIGURATION_ELEMENTS="resources/treasureHunt/map2018-elements-ica";
	//public static String INSTANCE_CONFIGURATION_ELEMENTS="resources/interlocking/mapInterlocking2-elements";
	
	//public static String INSTANCE_CONFIGURATION_ELEMENTS="resources/map2018-elements";
	
	//public static String INSTANCE_CONFIGURATION_ELEMENTS="resources/mapInterlocking2-elements";
	
	
	/**
	 * Must'nt be null as it describes the native agents' capabilities 
	 */
	public static String INSTANCE_CONFIGURATION_ENTITIES="resources/agentExplo";
	//public static String INSTANCE_CONFIGURATION_ENTITIES="resources/agentExplo-2";
	//public static String INSTANCE_CONFIGURATION_ENTITIES="resources/hunt/map2020-entitiesTree";
	//public static String INSTANCE_CONFIGURATION_ENTITIES="resources/hunt/map2020-entitiesGraph";
	//public static String INSTANCE_CONFIGURATION_ENTITIES="resources/map2018-entities2";
	//public static String INSTANCE_CONFIGURATION_ENTITIES="resources/map2019-entitiesExam1";
	//public static String INSTANCE_CONFIGURATION_ENTITIES="resources/monoAgent-entities";
	//public static String INSTANCE_CONFIGURATION_ENTITIES="resources/mapInterlocking2-entities";
	//public static String INSTANCE_CONFIGURATION_ENTITIES=null;
	
	/************************************
	 * 
	 * 
	 * When the environment is generated 
	 * 
	 * 
	 ***********************************/
	
	/**
	 * Parameter used to generate the environment 
	 */
	public static Integer ENVIRONMENT_SIZE=15;
	
	/**
	 * Parameter used to perceive the wumpus trough its smell
	 */
	public static final Integer DEFAULT_DETECTION_RADIUS = 1;
	
	/**true if a grid environment should be generated, false otherwise (A dogoronev env is generated)**/
	public static boolean ENVIRONMENTisGRID=true;
	public static boolean ACTIVE_WELL=false;
	public static boolean ACTIVE_GOLD=false;
	public static boolean ACTIVE_DIAMOND=true;
	
}