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
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
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

    protected void setup() {
        super.setup();

        Object[] args = getArguments();
        String[] myInfo = args[0].toString().split(";");
        name = myInfo[0].split(": ")[1];
        type = myInfo[1].split(": ")[1];

        openNodes = new ArrayList<>();
        closedNodes = new HashSet<>();
        well = new HashSet<>();
        regist(name,type);

        List lb = new ArrayList<>();
        lb.add(new InitBehaviour(this));
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
            System.out.println("Agent regist");
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
            if(agent.map==null) agent.map = new MapRepresentation();
        }

        @Override
        public boolean done() {
            if(finish) myAgent.addBehaviour(new ImReadyBehaviour());
            return finish;
        }

        public void linkAgentBDI(Agent_P3 agent){
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            templateSd.setType("AgentExploBDI");
            templateSd.setName("AgentBDI");
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
        }

        public void receiver(){
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("BDI"),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            ACLMessage requestMenssage = myAgent.receive(msgTemplate);
            if (requestMenssage != null) {
                if (requestMenssage.getContent() != null) {
                    String content = requestMenssage.getContent();
                    System.out.println("Agent Receive "+content);
                    Boolean correct=true;//TODO
                    if(!correct){
                        ACLMessage refuse = requestMenssage.createReply(ACLMessage.REFUSE);
                        refuse.setContent("NOT VALID");
                        send(refuse);
                        System.out.println("Agent refuse NOT VALID");
                    }
                    List<String> path =  map.getShortestPath(((Agent_P3)myAgent).getCurrentPosition().toString(),content);
                    if(path!=null){
                        ACLMessage refuse = requestMenssage.createReply(ACLMessage.AGREE);
                        //refuse.setContent("AGREE");
                        send(refuse);
                        System.out.println("Agent agree");
                        addBehaviour(new GoPosBehaviour(content));
                    }else {
                        ACLMessage refuse = requestMenssage.createReply(ACLMessage.REFUSE);
                        refuse.setContent("CANT GO");
                        send(refuse);
                        System.out.println("Agent refuse CANT GO");
                    }
                }
            }
        }
    }

    public class GoPosBehaviour extends Behaviour{

        private String dest;
        private Boolean finished;

        public GoPosBehaviour(String dest) {
            super();
            this.dest = dest;
            finished=false;
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
                return;
            }
            for(Object n:next) System.out.printf(n.toString());
            nextNode = next.get(0).toString();

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
                    if(nearWell)
                        for( Couple<Observation, Integer> l : lob.getRight()){
                            if(l.getLeft().toString().equals("WIND"))well.add(node);
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
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM_IF);
            msg.setProtocol("BDI");
            msg.setSender(myAgent.getAID());
            msg.addReceiver(agentBDIAID);
            msg.setContent(dest);
            send(msg);
        }

        public void sendFailure(String menssage){
            ACLMessage msg = new ACLMessage(ACLMessage.FAILURE);
            msg.setProtocol("BDI");
            msg.setSender(myAgent.getAID());
            msg.addReceiver(agentBDIAID);
            msg.setContent(menssage);
            send(msg);
        }
        @Override
        public boolean done() {
            if (finished)sendInformDone();
            return finished;
        }
    }
}