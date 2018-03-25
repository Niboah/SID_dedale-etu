package eu.su.mas.dedaleEtu.princ;

import eu.su.mas.dedale.env.EnvironmentType;

/**
 * 
 * @author hc
 *
 */
public final class ConfigurationFile {


	public static boolean PLATFORMisDISTRIBUTED= false;
	public static boolean COMPUTERisMAIN= true;

	public static String PLATFORM_HOSTNAME="127.0.0.1";
	public static String PLATFORM_ID="Ithaq";
	public static Integer PLATFORM_PORT=8888;
	
	public static String LOCAL_CONTAINER_NAME=PLATFORM_ID+"_"+"container1";
	public static String LOCAL_CONTAINER2_NAME=PLATFORM_ID+"_"+"container2";
	public static String LOCAL_CONTAINER3_NAME=PLATFORM_ID+"_"+"container3";
	public static String LOCAL_CONTAINER4_NAME=PLATFORM_ID+"_"+"container4";
	
	/**
	 * Required by the environment class to be able to load it. Currently, only one type of environment is available
	 */
	public static EnvironmentType ENVIRONMENT_TYPE=EnvironmentType.GS;
	
	
	public static String DEFAULT_GATEKEEPER_NAME="GK";
	
	/**
	 * When the environment is loaded; should be null if the environment is generated or already online
	 */
	
	//public static String INSTANCE_TOPOLOGY=null;
	public static String INSTANCE_TOPOLOGY="resources/map2018-topology";
	
	/**
	 * When the environment is loaded; should be null if the environment is generated or already online
	 */
	//public static String INSTANCE_CONFIGURATION_ELEMENTS=null;
	public static String INSTANCE_CONFIGURATION_ELEMENTS="resources/map2018-elements";

	//public static String INSTANCE_CONFIGURATION_ENTITIES=null;
	public static String INSTANCE_CONFIGURATION_ENTITIES="resources/map2018-entities";
	
	/************************************
	 * 
	 * 
	 * When the environment is generated (Instance_topology and instance configuration elements are null) 
	 * 
	 * 
	 ***********************************/
	
	/**
	 * Parameter used to generate the environment 
	 */
	public static Integer ENVIRONMENT_SIZE=7;
	
	/**
	 * Parameter used to perceive the wumpus
	 */
	public static final Integer DEFAULT_DETECTION_RADIUS = 1;
	
	/**true if a grid environment should be generated, false otherwise (A dogoronev env is generated)**/
	public static boolean ENVIRONMENTisGRID=true;
	public static boolean ACTIVE_WELL=true;
	public static boolean ACTIVE_GOLD=true;
	public static boolean ACTIVE_DIAMOND=true;
	
}
