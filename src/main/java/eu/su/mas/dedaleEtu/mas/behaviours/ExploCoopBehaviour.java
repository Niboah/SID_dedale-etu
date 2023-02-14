package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.List;

/**
 * <pre>
 * This behaviour allows an agent to explore the environment and learn the associated topological map.
 * The algorithm is a pseudo - DFS computationally consuming because its not optimised at all.
 *
 * When all the nodes around him are visited, the agent randomly select an open node and go there to restart its dfs.
 * This (non optimal) behaviour is done until all nodes are explored.
 *
 * Warning, this behaviour does not save the content of visited nodes, only the topology.
 * Warning, the sub-behaviour ShareMap periodically share the whole map
 * </pre>
 *
 * @author hc
 */
public class ExploCoopBehaviour extends SimpleBehaviour {
    private static final long serialVersionUID = 8567689731496787661L;
    private boolean finished = false;

    /**
     * Current knowledge of the agent regarding the environment
     */
    private MapRepresentation myMap;

    private final List<String> list_agentNames;

    /**
     * @param myAgent    the agent using this behaviour
     * @param myMap      known map of the world the agent is living in
     * @param agentNames name of the agents to share the map with
     */
    public ExploCoopBehaviour(final AbstractDedaleAgent myAgent, MapRepresentation myMap, List<String> agentNames) {
        super(myAgent);
        this.myMap = myMap;
        this.list_agentNames = agentNames;
    }

    @Override
    public void action() {
        if (this.myMap == null) {
            this.myMap = new MapRepresentation();
            this.myAgent.addBehaviour(new ShareMapBehaviour(this.myAgent, 500, this.myMap, list_agentNames));
        }

        //0) Retrieve the current position
        String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition().getLocationId();

        if (myPosition != null) {
            //List of observable from the agent's current position
            List<Couple<Location, List<Couple<Observation, Integer>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();//myPosition

            // Just added here to let you see what the agent is doing, otherwise it will be too quick
            try {
                this.myAgent.doWait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //1) remove the current node from openlist and add it to closedNodes.
            this.myMap.addNode(myPosition, MapAttribute.closed);

            //2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
            String nextNode = null;
            for (Couple<Location, List<Couple<Observation, Integer>>> lob : lobs) {
                String nodeId = lob.getLeft().getLocationId();
                boolean isNewNode = this.myMap.addNewNode(nodeId);
                //the node may exist, but not necessarily the edge
                if (!myPosition.equals(nodeId)) {
                    this.myMap.addEdge(myPosition, nodeId);
                    if (nextNode == null && isNewNode) nextNode = nodeId;
                }
            }

            //3) while openNodes is not empty, continues.
            if (!this.myMap.hasOpenNode()) {
                //Explo finished
                finished = true;
                System.out.println(this.myAgent.getLocalName() + " - Exploration successufully done, behaviour removed.");
            } else {
                //4) select next move.
                //4.1 If there exist one open node directly reachable, go for it,
                //	 otherwise choose one from the openNode list, compute the shortestPath and go for it
                if (nextNode == null) {
                    //no directly accessible openNode
                    //chose one, compute the path and take the first step.
                    nextNode = this.myMap.getShortestPathToClosestOpenNode(myPosition).get(0);
                }

                //5) At each time step, the agent check if he received a graph from a teammate.
                // If it was written properly, this sharing action should be in a dedicated behaviour set.
                MessageTemplate msgTemplate = MessageTemplate.and(
                        MessageTemplate.MatchProtocol("SHARE-TOPO"),
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
                if (msgReceived != null) {
                    try {
                        SerializableSimpleGraph<String, MapAttribute> sgreceived =
                                (SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject();
                        this.myMap.mergeMap(sgreceived);
                    } catch (UnreadableException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                ((AbstractDedaleAgent) this.myAgent).moveTo(new gsLocation(nextNode));
            }
        }
    }

    @Override
    public boolean done() {
        return finished;
    }
}
