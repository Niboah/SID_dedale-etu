package eu.su.mas.dedaleEtu.mas.agents.my;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
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


import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class Agent_P2 extends AbstractDedaleAgent {

    private static int DOWAIT=50;
    public Map<String,String> agents; //other agents, position
    public MapRepresentation myMap;
    public String type;
    public String name;
    private static String P2 = "src/main/java/eu/su/mas/dedaleEtu/mas/agents/my/p2.rdf";
    private static String P2mod= "target/classes/eu/su/mas/dedaleEtu/mas/agents/my/P2";

    protected void setup() {
        super.setup();

        Object[] args =  getArguments();
        String[] myInfo =args[0].toString().split(";");
        name= myInfo[0].split(": ")[1];
        type= myInfo[1].split(": ")[1];

        regist(name,type);

        List lb = new ArrayList<>();
        this.agents = new HashMap<>();

        lb.add(new InitBehaviour(this,1000));
        addBehaviour(new startMyBehaviours(this, lb));



    }


    public void regist(String name,String type){
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setName(name);
            sd.setType(type);
            dfd.addServices(sd);
            DFService.register(this,dfd);
        } catch (FIPAException e) {
            throw new RuntimeException(e);
        }
    }

    public class InitBehaviour  extends WakerBehaviour {

        public InitBehaviour(Agent a, long timeout) {
            super(a, timeout);
        }
        public void onWake(){
            Agent_P2 agent = (Agent_P2) myAgent;
            updateAgentList(agent);
            agent.myMap = new MapRepresentation();
            agent.addBehaviour(new ExploreBehaviour(agent));
            agent.addBehaviour(new ShareInfoBehaviour(agent));
            JenaTester owl=new JenaTester("p2.rdf");
            owl.loadOntology();
            String rol="";
            if (agent.type.equals("AgentExplo")) {
                rol="explorer";
            }
            else if (agent.type.equals("AgentTanker")) {
                rol="tanker";
            }
            else if (agent.type.equals("AgentCollector")) {
                rol="collector";
            }
            owl.addAgent(name,rol,agent.getCurrentPosition().toString());
            try {
                owl.releaseOntology(name);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        public void updateAgentList(Agent_P2 agent){
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
            Agent_P2 agent = (Agent_P2) this.myAgent;

            try {
                agent.doWait(DOWAIT);
            } catch (Exception e) {
                e.printStackTrace();
            }


            String myPosition = agent.getCurrentPosition().getLocationId();
            if (myPosition == null) return;


            this.closedNodes.add(myPosition);
            this.openNodes.remove(myPosition);

            agent.myMap.addNode(myPosition, MapRepresentation.MapAttribute.closed);

            String nextNode = null;
            JenaTester owl=new JenaTester("p2-agent"+name+"-modified.rdf");
            owl.loadOntology();
            List<Couple<Location, List<Couple<Observation, Integer>>>> lobs = agent.observe();
            for (Couple<Location, List<Couple<Observation, Integer>>> lob : lobs) {
                String nodeId = lob.getLeft().getLocationId();
                owl.add_connect_node(myPosition,nodeId);
                boolean wind=false; //evitar ir al pozo
                for(Couple<Observation, Integer> c : lob.getRight()){
                    switch (c.getLeft()) {
                        case WIND:
                            owl.addObject(nodeId, c.getLeft().toString(),0);
                            wind = true;
                    }
                    if(c.getRight()!=null)
                        owl.addObject(nodeId, c.getLeft().toString(),c.getRight());

                }
                if (!this.closedNodes.contains(nodeId)) {
                    if (!this.openNodes.contains(nodeId)) {
                        if(!wind){
                            this.openNodes.add(nodeId);
                            agent.myMap.addNode(nodeId, MapRepresentation.MapAttribute.open);
                            agent.myMap.addEdge(myPosition, nodeId);
                        }
                    } else {
                        agent.myMap.addEdge(myPosition, nodeId);
                    }
                    Collection c  = agent.agents.values();
                    if (nextNode == null && !wind && !c.contains(nodeId)) nextNode = nodeId;
                }
            }


            String goal="";
            if (this.openNodes.isEmpty()) {
                this.finished = true;
                System.out.println("Exploration successufully done, behaviour removed.");
            } else {
                if ( nextNode == null ) {
                    if(nextNode==null){
                        Collections.sort(this.openNodes);
                        if(this.openNodes.size()>0) {
                            next = agent.myMap.getShortestPath(myPosition, this.openNodes.get(0));
                            if(next==null)return;
                            nextNode = next.get(0).toString();
                            goal = this.openNodes.get(0);
                        }
                    }
                }
                //System.out.println(myAgent.getLocalName()+" "+myPosition+" -> "+nextNode+" = "+goal);
                if(nextNode==null)return;
                ((AbstractDedaleAgent) this.myAgent).moveTo(new gsLocation(nextNode));
                String rol="";
                if (agent.type.equals("AgentExplo")) {
                    rol="explorer";
                }
                else if (agent.type.equals("AgentTanker")) {
                    rol="tanker";
                }
                else if (agent.type.equals("AgentCollector")) {
                    rol="collector";
                }

                owl.changePosition(myPosition,nextNode,agent.name,rol);
                try {
                    owl.releaseOntology(agent.name);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
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
            Agent_P2 agent = (Agent_P2) myAgent;
            sendPosition(agent);
            sendMap(agent);
            sendInfo(agent);
            try {
                receiveInfo(agent);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            receive(agent);
            receiveMap(agent);
        }

        private void receive(Agent_P2 agent) {
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("SHARE-POS"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msgReceived = myAgent.receive(msgTemplate);
            if (msgReceived != null) {
                if (msgReceived.getContent() != null) {
                    String[] content = msgReceived.getContent().split(" ");

                        System.out.println(agent.getLocalName() + " - Mensage recived: " +  content[0]+" "+content[1]);
                        agent.agents.put(content[0],content[1]);
                }
            }
        }

        private void receiveMap(Agent_P2 agent) {
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("SHARE-TOPO"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
            if (msgReceived != null) {
                try {
                    SerializableSimpleGraph<String, MapRepresentation.MapAttribute> sgreceived =
                            (SerializableSimpleGraph<String, MapRepresentation.MapAttribute>) msgReceived.getContentObject();
                    agent.myMap.mergeMap(sgreceived);
                    //System.out.println(myAgent.getLocalName() + " - Map recived");
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * tractar las informacions recibidas por otros agentes, añadirlas a la ontologia
         */
        private void receiveInfo(Agent_P2 agent) throws FileNotFoundException {
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("SHARE-INFO"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msgReceived = myAgent.receive(msgTemplate);
            if (msgReceived != null) {
                if (msgReceived.getContent() != null) {
                    String[] content = msgReceived.getContent().split(" ");
                    JenaTester owl=new JenaTester("p2-agent"+name+"-modified.rdf");
                    owl.loadOntology();
                    String rol="";
                    if (content[2].equals("AgentExplo")) {
                        rol="explorer";
                    }
                    else if (content[2].equals("AgentTanker")) {
                        rol="tanker";
                    }
                    else if (content[2].equals("AgentCollector")) {
                        rol="collector";
                    }
                    owl.addAgent(content[0],rol,content[1]);
                    owl.releaseOntology(name);

                }
            }
        }

        private void sendPosition(Agent_P2 agent) {

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setProtocol("SHARE-POS");
            msg.setSender(myAgent.getAID());
            for (String agentName : agent.agents.keySet()) {
                if (agentName.equals(myAgent.getLocalName())) continue;
                msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
            }
            long time=1;
            String mensage= this.myAgent.getLocalName()+" "+((AbstractDedaleAgent) this.myAgent).getCurrentPosition()+" "+time;
            msg.setContent(  mensage);
            agent.sendMessage(msg);
        }

        /**
         * funcion para enviar el nombre, la posicion y tipo del agente
         */
        private void sendInfo(Agent_P2 agent) {

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setProtocol("SHARE-INFO");
            msg.setSender(myAgent.getAID());
            for (String agentName : agent.agents.keySet()) {
                if (agentName.equals(myAgent.getLocalName())) continue;
                msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
            }
            String mensage= this.myAgent.getLocalName()+" "+((AbstractDedaleAgent) this.myAgent).getCurrentPosition()+" "+agent.type;
            msg.setContent(  mensage);
            agent.sendMessage(msg);
        }

        private void sendMap(Agent_P2 agent) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setProtocol("SHARE-TOPO");
            msg.setSender(this.myAgent.getAID());
            for (String agentName : agent.agents.keySet()) {
                if (agentName.equals(myAgent.getLocalName())) continue;
                msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
            }
            try {
                SerializableSimpleGraph<String, MapRepresentation.MapAttribute> sg = agent.myMap.getSerializableGraph();
                msg.setContentObject(sg);
            } catch (IOException e) {
                e.printStackTrace();
            }
            agent.sendMessage(msg);
        }
    }

}
