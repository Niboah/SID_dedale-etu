package eu.su.mas.dedaleEtu.mas.agents.my;

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
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class Agent_P3 extends AbstractDedaleAgent {
    private static int DOWAIT=50;
    private String name;
    private String type;
    private MapRepresentation map;
    private AID agentBDIAID;
    private List<String> openNodes; //Nodes known but no visited
    private Set<String> closedNodes; //Visited node
    private Set<String> well;
    public Map<String,String> agents; //other agents, position
    protected void setup() {
        super.setup();
        Object[] args = getArguments();
        String[] myInfo = args[0].toString().split(";");
        name = myInfo[0].split(": ")[1];
        type = myInfo[1].split(": ")[1];
        agents = new HashMap<>();
        openNodes = new ArrayList<>();
        closedNodes = new HashSet<>();
        well = new HashSet<>();
        regist(name,type);

        List lb = new ArrayList<>();
        lb.add(new InitBehaviour(this));
        lb.add(new ShareMyPosBehaviour(this));
        lb.add(new RefleshAgents(this,1000));
        lb.add(new ShareInfoBehaviour());
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
            System.out.println(name+" regist");
        } catch (FIPAException e) {
            throw new RuntimeException(e);
        }
    }

    public class InitBehaviour  extends Behaviour {
        public boolean finish=false;
        public InitBehaviour(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            Agent_P3 agent = (Agent_P3) myAgent;
            linkAgentBDI(agent);
            updateAgentList(agent);
            if(agent.map==null) agent.map = new MapRepresentation();
        }

        @Override
        public boolean done() {
            if(finish) myAgent.addBehaviour(new ImReadyBehaviour());
            return finish;
        }

        public void linkAgentBDI(Agent_P3 agent){
            String bdiAgent="BDI_"+name;
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            templateSd.setType("AgentExploBDI");
            templateSd.setName(bdiAgent);
            template.addServices(templateSd);
            DFAgentDescription[] results = new DFAgentDescription[0];
            try {
                results = DFService.search(myAgent, template);
                if (results.length > 0) {
                    for(DFAgentDescription dfd: results){
                        AID provider = dfd.getName();
                        if(provider.getLocalName().equals(agent.getLocalName()))continue;
                        System.out.println("-----"+myAgent.getLocalName()+" detect "+provider.getLocalName());
                        agent.agentBDIAID = provider;
                        finish=true;
                    }
                }
            } catch (FIPAException e) {
                throw new RuntimeException(e);
            }
        }

        public void updateAgentList(Agent_P3 agent){
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
        }
    }
    public class ImReadyBehaviour extends Behaviour{
        public boolean finish=false;
        @Override
        public void action() {
            String currentPosition = ((Agent_P3)myAgent).getCurrentPosition().toString();
            map.addNode(currentPosition, MapRepresentation.MapAttribute.open);
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setProtocol("BDI");
            msg.setSender(myAgent.getAID());
            msg.addReceiver(agentBDIAID);
            msg.setContent(currentPosition);
            send(msg);

            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("BDI"),
                    MessageTemplate.MatchPerformative(ACLMessage.AGREE));
            ACLMessage requestMenssage = myAgent.receive(msgTemplate);
            if (requestMenssage != null) {
                if (requestMenssage.getContent() != null) {
                    String content = requestMenssage.getContent();
                    finish=true;
                }
            }
        }

        @Override
        public boolean done() {
            if(finish) myAgent.addBehaviour(new AgentBDIReciverBehaviour());
            return finish;
        }
    }
    public class AgentBDIReciverBehaviour extends CyclicBehaviour{
        @Override
        public void action() {
            receiver();
            inform();
        }

        public void inform(){
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("BDI"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage requestMenssage = myAgent.receive(msgTemplate);
            if (requestMenssage != null) {
                if (requestMenssage.getContent() != null) {
                    String content = requestMenssage.getContent();
                    if(content.equals("FINISH"))myAgent.doDelete();
                }
            }


        }
        public void receiver(){
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("BDI"),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            ACLMessage requestMenssage = myAgent.receive(msgTemplate);
            if (requestMenssage != null) {
                if (requestMenssage.getContent() != null) {
                    String content = requestMenssage.getContent();
                    System.out.println(name+ " Receive "+content);
                    Boolean correct=true;//TODO
                    if(!correct){
                        ACLMessage refuse = requestMenssage.createReply(ACLMessage.REFUSE);
                        refuse.setContent("NOT VALID");
                        send(refuse);
                        System.out.println(name+ " refuse NOT VALID");
                    }
                    List<String> path =  map.getShortestPath(((Agent_P3)myAgent).getCurrentPosition().toString(),content);
                    for(String node:path){
                        if(agents.values().contains(node))path=null;
                    }
                    if(path!=null){
                        ACLMessage reply = requestMenssage.createReply(ACLMessage.AGREE);
                        //reply.setContent(jena.sendMessage("AGREE"));
                        send(reply);
                        System.out.println(name+ " agree "+content);
                        addBehaviour(new GoPosBehaviour(content,requestMenssage));
                    }else {
                        ACLMessage reply = requestMenssage.createReply(ACLMessage.REFUSE);
                        reply.setContent(content);
                        send(reply);
                        System.out.println(name+" refuse CANT GO "+content);
                    }
                }
            }
        }
    }
    public class GoPosBehaviour extends Behaviour{

        private String dest;
        private Boolean finished;
        private Boolean fail;
        private String lastPos;
        private ACLMessage requestMenssage;

        public GoPosBehaviour(String dest,ACLMessage requestMenssage) {
            super();
            this.dest = dest;
            this.fail=false;
            this.finished=false;
            this.lastPos="";
            this.requestMenssage=requestMenssage;
        }

        @Override
        public void action() {
            try {
                myAgent.doWait(DOWAIT);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Agent_P3 agent = (Agent_P3) myAgent;
            String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition().toString();

            observe(agent, myPosition);

            if(myPosition.equals(dest)){
                finished=true;
                return;
            }

            String nextNode="";
            List next = agent.map.getShortestPath(myPosition, dest);
            if(next==null || next.isEmpty()){
                sendFailure("FAIL");
                fail=true;
                return;
            }

            nextNode = next.get(0).toString();
            if(lastPos==myPosition){
                sendFailure(nextNode);
                fail=true;
                return;
            }

            lastPos=myPosition;
            agent.moveTo(new gsLocation(nextNode));
        }

        public void observe(Agent_P3 agent, String myPosition){
            String content="";
            List<Couple<Location, List<Couple<Observation, Integer>>>> lobs = agent.observe();
            Boolean nearWell=false;
            for (Couple<Location, List<Couple<Observation, Integer>>> lob : lobs) {
                String node = lob.getLeft().toString();
                if(myPosition==node){
                    agent.openNodes.remove(node);
                    agent.closedNodes.add(node);
                    agent.map.addNode(node, MapRepresentation.MapAttribute.closed);
                    for( Couple<Observation, Integer> l : lob.getRight()){
                        if(l.getLeft().toString().equals("WIND"))
                            nearWell=true;
                    }
                }else{
                    for( Couple<Observation, Integer> l : lob.getRight()){
                        if(nearWell && l.getLeft().toString().equals("WIND"))well.add(node);
                        if(l.getLeft().toString().equals("Stench")) agents.put("Stench",node);
                    }


                    if(well.contains(node))
                        continue;
                    if (!agent.closedNodes.contains(node)) {
                        if (!agent.openNodes.contains(node)) {
                            agent.openNodes.add(node);
                            agent.map.addNode(node, MapRepresentation.MapAttribute.open);
                            agent.map.addEdge(myPosition, node);

                        } else {
                            agent.map.addEdge(myPosition, node);
                        }
                    }
                }

                content+=node;
                for(Couple<Observation, Integer> c : lob.getRight())
                    content+= " "+c.getLeft().toString()+":"+c.getRight();
                content+="\n";
            }
            sendObserve(content);
        }

        public void sendObserve(String content){
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setProtocol("BDI");
            msg.setSender(myAgent.getAID());
            msg.addReceiver(agentBDIAID);
            msg.setContent(content);
            send(msg);
        }

        public void sendInformDone(){
            ACLMessage msg = this.requestMenssage.createReply(ACLMessage.INFORM_IF);
            msg.setProtocol("BDI");
            msg.setSender(myAgent.getAID());
            msg.addReceiver(agentBDIAID);
            msg.setContent(dest);
            send(msg);
        }

        public void sendFailure(String menssage){
            ACLMessage msg = this.requestMenssage.createReply(ACLMessage.FAILURE);
            msg.setProtocol("BDI");
            msg.setSender(myAgent.getAID());
            msg.addReceiver(agentBDIAID);
            msg.setContent(menssage);
            send(msg);
        }
        @Override
        public boolean done() {
            if (finished) sendInformDone();
            return finished || fail;
        }
    }
    public class ShareMyPosBehaviour extends CyclicBehaviour {

        public ShareMyPosBehaviour(Agent agent) {
            super(agent);
        }

        @Override
        public void action() {
            Agent_P3 agent = (Agent_P3) myAgent;
            sendPosition(agent);
            receive(agent);
        }

        private void receive(Agent_P3 agent) {
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("SHARE-POS"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msgReceived = myAgent.receive(msgTemplate);
            if (msgReceived != null) {
                if (msgReceived.getContent() != null) {
                    String[] content = msgReceived.getContent().split(" ");

                    System.out.println("-" + agent.getLocalName() + " - Mensage recived: " +  content[0]+" "+content[1]);
                    agent.agents.put(content[0],content[1]);

                }
            }
        }


        private void sendPosition(Agent_P3 agent) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setProtocol("SHARE-POS");
            msg.setSender(myAgent.getAID());
            for (String agentName : agent.agents.keySet()) {
                if (agentName.equals(myAgent.getLocalName())) continue;
                msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
            }
            String mensage= this.myAgent.getLocalName()+" "+((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
            msg.setContent(mensage);
            agent.sendMessage(msg);
            //System.out.println(agent.getLocalName() + " - Send mensage: " +  mensage);
        }


    }

    public class ShareInfoBehaviour extends CyclicBehaviour{

        @Override
        public void action() {

        }
    }

    public class RefleshAgents extends TickerBehaviour{

        public RefleshAgents(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            Agent_P3 agentP3 = (Agent_P3) myAgent;
            for(String agent: agentP3.agents.keySet()){
                agentP3.agents.put(agent,"-1");
            }
        }
    }
}