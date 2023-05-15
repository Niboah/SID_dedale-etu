package eu.su.mas.dedaleEtu.mas.agents.my;

import dataStructures.serializableGraph.SerializableNode;
import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.startMyBehaviours;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import javafx.application.Platform;
import org.apache.jena.Jena;
import org.apache.jena.util.FileManager;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;
import org.apache.commons.compress.utils.Lists;
import org.apache.jena.ontology.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.ResultBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.*;
import java.util.stream.Collectors;

public class Agent_P1 extends AbstractDedaleAgent {
    private static int DOWAIT=50;
    private static int RESTARTTIME=10000;

    public enum MapAttribute {
        agent, open, closed;
    }
    public class MapRepresentation{


        private static final long serialVersionUID = -1333959882640838272L;

        /*********************************
         * Parameters for graph rendering
         ********************************/

        private final String defaultNodeStyle = "node {" + "fill-color: black;" + " size-mode:fit;text-alignment:under; text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";
        private final String nodeStyle_open = "node.agent {" + "fill-color: forestgreen;" + "}";
        private final String nodeStyle_agent = "node.open {" + "fill-color: blue;" + "}";
        private final String nodeStyle = defaultNodeStyle + nodeStyle_agent + nodeStyle_open;

        private Graph g; //data structure non serializable
        private Viewer viewer; //ref to the display, non-serializable
        private Integer nbEdges;//used to generate the edges ids
        private SerializableSimpleGraph<String,MapAttribute> sg;//used as a temporary dataStructure during migration

        public MapRepresentation() {
            //System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
            System.setProperty("org.graphstream.ui", "javafx");
            this.g = new SingleGraph("My world vision");
            this.g.setAttribute("ui.stylesheet", nodeStyle);
            Platform.runLater(this::openGui);
            //this.viewer = this.g.display();

            this.nbEdges = 0;
        }

        /**
         * Add or replace a node and its attribute
         *
         * @param id Identifier for the node
         * @param mapAttribute Map of attributes that are the metadata of the node
         */
        public synchronized void addNode(String id, MapAttribute mapAttribute) {
            Node n;
            if (this.g.getNode(id) == null) {
                n = this.g.addNode(id);
            } else {
                n = this.g.getNode(id);
            }
            n.clearAttributes();
            n.setAttribute("ui.class", mapAttribute.toString());
            n.setAttribute("ui.label", id);
        }

        /**
         * Add a node to the graph. Do nothing if the node already exists.
         * If new, it is labeled as open (non-visited)
         *
         * @param id id of the node
         * @return true if added
         */
        public synchronized boolean addNewNode(String id) {
            if (this.g.getNode(id) == null) {
                addNode(id, MapAttribute.open);
                return true;
            }
            return false;
        }

        /**
         * Add an undirect edge if not already existing.
         *
         * @param idNode1 Identifier of origin node
         * @param idNode2 Identifier of destination node
         */
        public synchronized void addEdge(String idNode1, String idNode2) {
            this.nbEdges++;
            try {
                this.g.addEdge(this.nbEdges.toString(), idNode1, idNode2);
            } catch (IdAlreadyInUseException e1) {
                System.err.println("ID existing");
                System.exit(1);
            } catch (EdgeRejectedException e2) {
                this.nbEdges--;
            } catch (ElementNotFoundException ignored) {
            }
        }

        /**
         * Compute the shortest Path from idFrom to IdTo. The computation is currently not very efficient
         *
         * @param idFrom id of the origin node
         * @param idTo   id of the destination node
         * @return the list of nodes to follow, null if the targeted node is not currently reachable
         */
        public synchronized List<String> getShortestPath(String idFrom, String idTo,Collection<String> obstacle) {
            List<String> shortestPath = new ArrayList<>();
            Set<String> ss = new HashSet<>();
            for(String o:obstacle){
                if(o!="-1" && g.getNode(o)!=null){
                    if(sg.getNode(o)==null)return null;
                    g.removeNode(o);
                    ss.addAll(sg.getEdges(o));
                }
            }
            if(g.getNode(idTo)==null || g.getNode(idFrom)==null){
                System.out.println(idTo+" NUll");
                for(String o: obstacle){
                    addNode(o,MapAttribute.closed);
                    if(ss!=null)
                        for(String s:ss) addEdge(o, s);
                }
                return null;
            }
            Dijkstra dijkstra = new Dijkstra();//number of edge
            dijkstra.init(g);
            dijkstra.setSource(g.getNode(idFrom));
            dijkstra.compute();//compute the distance to all nodes from idFrom

            List<Node> path = dijkstra.getPath(g.getNode(idTo)).getNodePath(); //the shortest path from idFrom to idTo
            for (Node edges : path) {
                shortestPath.add(edges.getId());
            }
            dijkstra.clear();

            for(String o: obstacle){
                addNode(o,MapAttribute.closed);
                if(ss!=null)
                    for(String s:ss) addEdge(o, s);
            }
            if (shortestPath.isEmpty()) {//The openNode is not currently reachable
                return null;
            } else {
                shortestPath.remove(0);//remove the current position
            }
            return shortestPath;
        }

        public List<String> getOpenNodes() {
            return this.g.nodes()
                    .filter(x -> x.getAttribute("ui.class") == eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute.open.toString())
                    .map(Node::getId)
                    .collect(Collectors.toList());
        }

        /**
         * Before the migration we kill all non-serializable components and store their data in a serializable form
         */
        public void prepareMigration() {
            serializeGraphTopology();
            closeGui();
            this.g = null;
        }

        /**
         * Before sending the agent knowledge of the map it should be serialized.
         */
        private void serializeGraphTopology() {
            this.sg = new SerializableSimpleGraph<>();
            for (Node n : this.g) {
                sg.addNode(n.getId(), MapAttribute.valueOf((String) n.getAttribute("ui.class")));
            }
            Iterator<Edge> iterE = this.g.edges().iterator();
            while (iterE.hasNext()) {
                Edge e = iterE.next();
                Node sn = e.getSourceNode();
                Node tn = e.getTargetNode();
                sg.addEdge(e.getId(), sn.getId(), tn.getId());
            }
        }

        public synchronized SerializableSimpleGraph<String, MapAttribute> getSerializableGraph() {
            serializeGraphTopology();
            return this.sg;
        }

        /**
         * After migration, we load the serialized data and recreate the non serializable components (Gui,..)
         */
        public synchronized void loadSavedData() {
            this.g = new SingleGraph("My world vision");
            this.g.setAttribute("ui.stylesheet", nodeStyle);

            openGui();

            int nbEd = 0;
            for (SerializableNode<String, MapAttribute> n : this.sg.getAllNodes()) {
                this.g.addNode(n.getNodeId()).setAttribute("ui.class", n.getNodeContent().toString());
                for (String s : this.sg.getEdges(n.getNodeId())) {
                    this.g.addEdge(Integer.toString(nbEd), n.getNodeId(), s);
                    nbEd++;
                }
            }
            System.out.println("Loading done");
        }

        /**
         * Method called before migration to kill all non-serializable graphStream components
         */
        private synchronized void closeGui() {
            //once the graph is saved, clear non-serializable components
            if (this.viewer != null) {
                //Platform.runLater(() -> {
                try {
                    this.viewer.close();
                } catch (NullPointerException e) {
                    System.err.println("Bug graphstream viewer.close() work-around - https://github.com/graphstream/gs-core/issues/150");
                }
                //});
                this.viewer = null;
            }
        }

        /**
         * Method called after a migration to reopen GUI components
         */
        private synchronized void openGui() {
            this.viewer = new FxViewer(this.g, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);//GRAPH_IN_GUI_THREAD)
            viewer.enableAutoLayout();
            viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
            viewer.addDefaultView(true);

            g.display();
        }

        public void mergeMap(SerializableSimpleGraph<String, MapAttribute> sgreceived) {
            //System.out.println("You should decide what you want to save and how");
            //System.out.println("We currently blindy add the topology");

            for (SerializableNode<String, MapAttribute> n : sgreceived.getAllNodes()) {
                //System.out.println(n);
                boolean alreadyIn = false;
                //1 Add the node
                Node newnode = null;
                try {
                    newnode = this.g.addNode(n.getNodeId());
                } catch (IdAlreadyInUseException e) {
                    alreadyIn = true;
                    //System.out.println("Already in"+n.getNodeId());
                }
                if (!alreadyIn) {
                    newnode.setAttribute("ui.label", newnode.getId());
                    newnode.setAttribute("ui.class", n.getNodeContent().toString());
                } else {
                    newnode = this.g.getNode(n.getNodeId());
                    //3 check its attribute. If it is below the one received, update it.
                    if (newnode.getAttribute("ui.class") == eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute.closed.toString() ||
                            Objects.equals(n.getNodeContent().toString(), eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute.closed.toString())) {
                        newnode.setAttribute("ui.class", eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute.closed.toString());
                    }
                }
            }

            //4 now that all nodes are added, we can add edges
            for (SerializableNode<String, MapAttribute> n : sgreceived.getAllNodes()) {
                for (String s : sgreceived.getEdges(n.getNodeId())) {
                    addEdge(n.getNodeId(), s);
                }
            }
            //System.out.println("Merge done");
        }

        /**
         * @return true if there exist at least one openNode on the graph
         */
        public boolean hasOpenNode() {
            return (this.g.nodes()
                    .anyMatch(n -> n.getAttribute("ui.class") == eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute.open.toString()));
        }
    }

    public enum ROLE {
        SEARCH, WAIT;
    }
    public enum STATE {
        RUNNING, STOP, FINISH;
    }

    public Map<String,String> agents; //other agents, position
    public String initPos;
    public String dest;
    public String objective;
    public ROLE role;

    public STATE state;
    public MapRepresentation myMap;
    //Ontologia
    private static String P2 = "src/main/java/eu/su/mas/dedaleEtu/mas/agents/my/";
    String JENAPath;
    String OntologyFile;
    String NamingContext;
    OntDocumentManager dm;
    //Ontologia
    protected void setup() {
        super.setup();

        Object[] args =  getArguments();
        String[] myInfo =args[0].toString().split(";");
        String name= myInfo[0].split(": ")[1];
        String type= myInfo[1].split(": ")[1];

        regist(name,type);

        List lb = new ArrayList<>();
        this.agents = new HashMap<>();

        if(name.equals("A")){
            this.state = STATE.RUNNING;
            this.role=ROLE.SEARCH;
        }else{
            this.state = STATE.STOP;
            this.role=ROLE.WAIT;
        }

        lb.add(new InitBehaviour(this,1000));
        addBehaviour(new startMyBehaviours(this, lb));
    }
    public void regist(String name,String type){
        try {
            //indicate id of agent and all services that offers
            DFAgentDescription dfd = new DFAgentDescription();
            //Set Id
            dfd.setName(getAID());
            //indicate the name of service, the type of service, ontologies, and the language of content
            ServiceDescription sd = new ServiceDescription();
            //Set name of service, in this case it is the parameters from arguments
            sd.setName(name);
            //Set type of service, in this case it is the parameters from arguments
            sd.setType(type);
            //Add this service
            dfd.addServices(sd);
            //indicate the name of service, the type of service, ontologies, and the language of content
            ServiceDescription sd2 = new ServiceDescription();
            //Set name of service, in this case it is the parameters from arguments
            sd2.setName((name));
            //Set type of service, in this case it is P1
            sd2.setType("P1");
            //Add this service
            dfd.addServices(sd2);
            //Register
            DFService.register(this,dfd);
        } catch (FIPAException e) {
            throw new RuntimeException(e);
        }
    }
    public class InitBehaviour  extends WakerBehaviour {
        //Initialization of Agent, creating...
        public InitBehaviour(Agent a, long timeout) {
            super(a, timeout);
        }
        //Action that produces after a periode of time
        public void onWake(){
            Agent_P1 agent = (Agent_P1) myAgent;
            agent.initPos = getCurrentPosition().toString();
            updateAgentList(agent);

            agent.myMap = new MapRepresentation();
            JenaTester tester = new JenaTester("p2.rdf");
            agent.addBehaviour(new ExploreBehaviour(agent));
            agent.addBehaviour(new ShareInfoBehaviour(agent));
            agent.addBehaviour(new ShareInitPosBehaviour(agent));
            //Start, load ontologia
            {

                System.out.println("----------------Starting program -------------");


                String name=getName().split("@")[0];
                tester.loadOntology();
                tester.addAgent(name,"explo",agent.initPos);
                tester.getClasses();
                //tester.getIndividuals();
                //tester.getIndividualsByClass();
                //tester.getPropertiesByClass();
                //tester.runSparqlQueryDataProperty();
                //tester.runSparqlQueryObjectProperty();
                //tester.runSparqlQueryModify();
                //tester.testEquivalentClass();
                //tester.exportStatement();
                try {
                    tester.releaseOntology(name);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }

                System.out.println("--------- Program terminated --------------------");
            }

        }

        public void updateAgentList(Agent_P1 agent){
            //ALL Agent
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            templateSd.setType("AgentExplo");
            template.addServices(templateSd);
            SearchConstraints sc = new SearchConstraints();
            sc.setMaxResults(10L);
            DFAgentDescription[] results = new DFAgentDescription[0];
            try {
                results = DFService.search(myAgent, template, sc);
                if (results.length > 0) {
                    for(DFAgentDescription dfd: results){
                        AID provider = dfd.getName();
                        if(provider.getLocalName().equals(agent.getLocalName()))continue;
                        System.out.println("-----"+myAgent.getLocalName()+" detect "+provider.getLocalName());
                        agent.agents.put(provider.getLocalName(),"-1");
                    }
                }
            } catch (FIPAException e) {
                throw new RuntimeException(e);
            }

            //P1 Agent
            templateSd.setType("P1");
            template.addServices(templateSd);
            try {
                results = DFService.search(myAgent, template, sc);
                if (results.length > 0) {
                    for(DFAgentDescription dfd: results){
                        if (dfd.getName().getLocalName().equals(myAgent.getLocalName()))continue;
                        AID provider = dfd.getName();
                        System.out.println("-----"+myAgent.getLocalName()+" detect objective "+provider.getLocalName());
                        agent.objective = provider.getLocalName();
                    }
                }
            } catch (FIPAException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public class ExploreBehaviour extends Behaviour {
        private final List<String> openNodes; //Nodes known but no visited
        private final Set<String> closedNodes; //Visited node
        private List<String> next;
        private Boolean finished;

        public ExploreBehaviour(Agent agent) {
            super(agent);
            this.openNodes = new ArrayList<>();
            this.closedNodes = new HashSet<>();
            this.finished = false;
        }

        @Override
        public void action() {
            Agent_P1 agent = (Agent_P1) this.myAgent;

            try {
                agent.doWait(DOWAIT);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String myPosition = agent.getCurrentPosition().getLocationId();
            if (myPosition == null) return;

            this.closedNodes.add(myPosition);
            this.openNodes.remove(myPosition);

            agent.myMap.addNode(myPosition, MapAttribute.closed);

            String nextNode = null;
            /** veure entorn*/
            List<Couple<Location, List<Couple<Observation, Integer>>>> lobs = agent.observe();
            for (Couple<Location, List<Couple<Observation, Integer>>> lob : lobs) {
                String nodeId = lob.getLeft().getLocationId();
                boolean wind=false; //evitar ir al pozo
                for(Couple<Observation, Integer> c : lob.getRight()){
                    switch (c.getLeft()) {
                        case WIND:
                            wind = true;
                    }
                }
                if (!this.closedNodes.contains(nodeId)) {
                    if (!this.openNodes.contains(nodeId)) {
                        if(!wind){
                            this.openNodes.add(nodeId);
                            agent.myMap.addNode(nodeId, MapAttribute.open);
                            agent.myMap.addEdge(myPosition, nodeId);
                        }
                    } else {
                        agent.myMap.addEdge(myPosition, nodeId);
                    }
                    Collection c  = agent.agents.values();
                    if (nextNode == null && !wind && !c.contains(nodeId)) nextNode = nodeId;
                }
            }

            if(agent.role.equals(ROLE.WAIT)){
                finished=true;
                return;
            }
            if(agent.role.equals(ROLE.SEARCH)){
                if(myPosition.equals(agent.dest)){
                    finished=true;
                    System.out.println(agent.getLocalName()+" FINISH");
                    agent.state=STATE.FINISH;
                    myAgent.addBehaviour(new RestartBehaviour(myAgent,1));
                    return;
                }
            }

            String goal="";
            if (this.openNodes.isEmpty()) {
                this.finished = true;
                System.out.println("Exploration successufully done, behaviour removed.");
                agent.addBehaviour(new GoPosBehaviour(agent.dest,agent));
            } else {
                if ( nextNode == null ) {
                    if(agent.dest!=null){
                        next = agent.myMap.getShortestPath(myPosition, agent.dest,agent.agents.values());
                        if(next==null){
                          nextNode =lobs.get(1).getLeft().getLocationId();
                        }else  nextNode = next.get(0).toString();
                    }
                    if(nextNode==null){
                        Collections.sort(this.openNodes);
                        if(this.openNodes.size()>0) {
                            next = agent.myMap.getShortestPath(myPosition, this.openNodes.get(0),agent.agents.values());
                            if(next==null)return;
                            nextNode = next.get(0).toString();
                            goal = this.openNodes.get(0);
                        }
                    }
                }

                //System.out.println(myAgent.getLocalName()+" "+myPosition+" -> "+nextNode+" = "+goal);
                if(nextNode==null)return;

                ((AbstractDedaleAgent) this.myAgent).moveTo(new gsLocation(nextNode));


            }
        }
        @Override
        public boolean done() {
            return finished;
        }
    }
    public class ShareInfoBehaviour extends CyclicBehaviour {

        public ShareInfoBehaviour(Agent agent) {
            super(agent);
        }

        @Override
        public void action() {
            Agent_P1 agent = (Agent_P1) myAgent;
            sendPosition(agent);
            sendMap(agent);
            receive(agent);
            receiveMap(agent);
        }

        private void receive(Agent_P1 agent) {
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("SHARE-POS"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msgReceived = myAgent.receive(msgTemplate);
            if (msgReceived != null) {
                if (msgReceived.getContent() != null) {
                    String[] content = msgReceived.getContent().split(" ");
                    if(agent.agents.get(content[0])=="-1"){
                        System.out.println(agent.getLocalName() + " - Mensage recived: " +  content[0]+" "+content[1]);
                        agent.agents.put(content[0],content[1]);
                        agent.addBehaviour(new UpdateAgentPosBehaviour(agent,Long.parseLong(content[2]),content[0]));
                    }
                }
            }
        }

        private void receiveMap(Agent_P1 agent) {
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("SHARE-TOPO"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
            if (msgReceived != null) {
                try {
                    SerializableSimpleGraph<String, MapAttribute> sgreceived =
                            (SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject();
                    agent.myMap.mergeMap(sgreceived);
                    //System.out.println(myAgent.getLocalName() + " - Map recived");
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendPosition(Agent_P1 agent) {

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setProtocol("SHARE-POS");
            msg.setSender(myAgent.getAID());
            for (String agentName : agent.agents.keySet()) {
                if (agentName.equals(myAgent.getLocalName())) continue;
                msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
            }
            long time=1;
            if(agent.state.equals(STATE.STOP) || agent.state.equals(STATE.FINISH)) time=1000;
            String mensage= this.myAgent.getLocalName()+" "+((AbstractDedaleAgent) this.myAgent).getCurrentPosition()+" "+time;
            msg.setContent(  mensage);
            agent.sendMessage(msg);
        }

        private void sendMap(Agent_P1 agent) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setProtocol("SHARE-TOPO");
            msg.setSender(this.myAgent.getAID());
            for (String agentName : agent.agents.keySet()) {
                if (agentName.equals(myAgent.getLocalName())) continue;
                msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
            }
            try {
                SerializableSimpleGraph<String, MapAttribute> sg = agent.myMap.getSerializableGraph();
                msg.setContentObject(sg);
            } catch (IOException e) {
                e.printStackTrace();
            }
            agent.sendMessage(msg);
        }
    }
    public class ShareInitPosBehaviour extends Behaviour {
        private Boolean finished;
        public ShareInitPosBehaviour(Agent agent) {
            super(agent);
            finished=false;
        }
        @Override
        public void action() {
            Agent_P1 agent = (Agent_P1) myAgent;
            sendPosition(agent);
            receive(agent);
        }
        protected void sendPosition(Agent_P1 agent) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setProtocol("SHARE-INIT-POS");
            msg.setSender(agent.getAID());
            msg.addReceiver(new AID(agent.objective, AID.ISLOCALNAME));
            if(agent.state.equals(STATE.FINISH))msg.setContent("");
            else msg.setContent(agent.initPos);
            agent.sendMessage(msg);
        }

        protected void receive(Agent_P1 agent) {
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("SHARE-INIT-POS"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msgReceived = agent.receive(msgTemplate);
            String mensage = "";
            if (msgReceived != null) {
                if (msgReceived.getContent() != null) {
                    mensage = msgReceived.getContent();
                    if (!mensage.equals("")){
                        agent.dest = mensage;
                        if (agent.role.equals(ROLE.WAIT) && agent.state.equals(STATE.STOP)) {
                            System.out.println(agent.getLocalName() + " - Init pos recived: " + mensage);
                            agent.addBehaviour(new GoPosBehaviour(mensage, agent));
                            agent.state = STATE.RUNNING;
                        }
                    }
                }
            }
        }
        @Override
        public boolean done() {
            return finished;
        }
    }
    public class GoPosBehaviour extends Behaviour {
        private String dest;
        private Agent myAgent;

        private Boolean finished;

        public GoPosBehaviour( String dest,Agent myAgent) {
            super();
            this.dest = dest;
            this.myAgent=myAgent;
            finished=false;
        }

        @Override
        public void action() {
            Agent_P1 agent = (Agent_P1) myAgent;
            try {
                myAgent.doWait(DOWAIT);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition().toString();

            if(myPosition.equals(dest))finished=true;

            String nextNode="";
            List next = agent.myMap.getShortestPath(myPosition, dest,agent.agents.values());
            if(next==null ||next.isEmpty()){
                /*List<Couple<Location, List<Couple<Observation, Integer>>>> lobs = agent.observe();
                for (Couple<Location, List<Couple<Observation, Integer>>> lob : lobs) {
                    String nodeId = lob.getLeft().getLocationId();
                    if(!agent.agents.values().contains(nodeId)) nextNode=nodeId;
                }*/
            }else nextNode = next.get(0).toString();

            //System.out.println("Dest "+this.myAgent.getLocalName() + " " + myPosition + " -> " + nextNode + " = " + dest);
            if(nextNode=="")return;
            agent.moveTo(new gsLocation(nextNode));
        }
        @Override
        public boolean done() {
            if(finished){
                ((Agent_P1)myAgent).state=STATE.FINISH;
                System.out.println(this.myAgent.getLocalName()+" FINISH");
                myAgent.addBehaviour(new RestartBehaviour(myAgent,RESTARTTIME));
            }
            return finished;
        }
    }
    public class UpdateAgentPosBehaviour extends  WakerBehaviour{

        private String a;
        public UpdateAgentPosBehaviour(Agent agent, long timeout,String a) {
            super(agent, timeout);
            this.a=a;
        }

        public void onWake(){
            Agent_P1 agent = (Agent_P1) myAgent;
            System.out.println(agent.getLocalName()+" UPDATE "+a);
            agent.agents.replace(a,"-1");
        }
    }
    public class RestartBehaviour extends WakerBehaviour{

        public RestartBehaviour(Agent a, long timeout) {
            super(a, timeout);
        }

        public void onWake(){
            Agent_P1 agent = (Agent_P1) myAgent;
            agent.initPos = getCurrentPosition().toString();
            agent.dest=null;
            System.out.println(agent.getLocalName()+" RESTART");

            if(agent.role.equals(ROLE.WAIT)){
                agent.role=ROLE.SEARCH;
                agent.state=STATE.RUNNING;
            }else{
                agent.role=ROLE.WAIT;
                agent.state=STATE.STOP;
            }
            agent.addBehaviour(new ExploreBehaviour(agent));
            //agent.addBehaviour(new ShareInitPosBehaviour(agent));
        }
    }
}
