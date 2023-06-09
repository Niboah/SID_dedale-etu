package eu.su.mas.dedaleEtu.sid.grupo10;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.ReceiveTreasureTankerBehaviour;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.*;

public class SituatedAgent10 extends AbstractDedaleAgent {
    public static String BDI_MESSAGE_PROTOCOL="BDI10";
    private static String BDI_AGENT_NAME="BDI_AGENT10";
    private static String BDI_DF_TYPE="AgentBDI10";
    private static String SITUATED_DF_TYPE ="Situated";
    private static int DOWAIT=50;
    private String name;
    public String type;
    private MapRepresentation map;
    private AID agentBDIAID;
    private List<String> openNodes; //Nodes known but no visited
    private Set<String> closedNodes; //Visited node
    private Set<String> well;

    public Map<String,String> agents; //other agents, position
    private Boolean goingPos;
    public List<String> tankers;

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
        tankers=new ArrayList<>();
        regist(name,type);

        List lb = new ArrayList<>();
        lb.add(new InitBehaviour(this));
        lb.add(new RecibeMenssageBehaviour());
        lb.add(new ShareInfoBehaviour(this));
        addBehaviour(new startMyBehaviours(this, lb));
    }

    public void regist(String name,String type){
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setName(getLocalName());
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
            SituatedAgent10 agent = (SituatedAgent10) myAgent;
            linkAgentBDI(agent);
            updateAgentList(agent,"AgentTanker");
            updateAgentList(agent,"AgentExplo");
            updateAgentList(agent,"AgentCollect");
            if(agent.map==null) agent.map = new MapRepresentation();
        }

        @Override
        public boolean done() {
            if(finish) myAgent.addBehaviour(new ImReadyBehaviour());
            return finish;
        }
        public void linkAgentBDI(SituatedAgent10 agent){

            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            templateSd.setType(BDI_DF_TYPE);
            //templateSd.setName(BDI_AGENT_NAME);

            String bdiAgent="BDI_"+name;
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
        public void updateAgentList(SituatedAgent10 agent, String type){
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            templateSd.setType(type);
            template.addServices(templateSd);
            SearchConstraints sc = new SearchConstraints();
            sc.setMaxResults(10L);
            DFAgentDescription[] results;
            try {
                results = DFService.search(myAgent, template, sc);
                if (results.length > 0) {
                    for(DFAgentDescription dfd: results){
                        AID provider = dfd.getName();
                        if(provider.getLocalName().equals(agent.getLocalName()))continue;
                        System.out.println("-----"+myAgent.getLocalName()+" detect "+provider.getLocalName());
                        agent.agents.put(provider.getLocalName(),"-1");
                        if (type=="AgentTanker")
                        {
                            tankers.add(provider.getLocalName());
                        }
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
            String currentPosition = ((SituatedAgent10)myAgent).getCurrentPosition().toString();
            map.addNode(currentPosition, MapRepresentation.MapAttribute.open);
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setProtocol(BDI_MESSAGE_PROTOCOL);
            msg.setSender(myAgent.getAID());
            msg.addReceiver(agentBDIAID);
            msg.setContent(currentPosition+":"+type+":"+((AbstractDedaleAgent) this.myAgent).getMyTreasureType());
            send(msg);
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(BDI_MESSAGE_PROTOCOL),
                    MessageTemplate.MatchPerformative(ACLMessage.AGREE));
            ACLMessage requestMessage = myAgent.receive(msgTemplate);
            if (requestMessage != null) {
                if (requestMessage.getContent() != null) {
                    String content = requestMessage.getContent();
                    finish=true;
                }
            }
        }


        @Override
        public boolean done() {
            if(finish){
                switch (((SituatedAgent10)myAgent).type){
                    case "AgentExplo":
                        myAgent.addBehaviour(new AgentBDIReciverBehaviour());
                        System.out.println(name + " AgentExplo START");
                        break;
                    case "AgentCollect":
                        myAgent.addBehaviour(new AgentBDIReciverBehaviour());
                        System.out.println(name + " AgentCollect START");
                        break;
                    case "AgentTanker":
                        myAgent.addBehaviour(new AgentBDIReciverBehaviour());
                        System.out.println(name + " AgentTanker START");
                        break;
                }
            }
            return finish;
        }
    }
    public class AgentBDIReciverBehaviour extends CyclicBehaviour{
        @Override
        public void action() {
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(BDI_MESSAGE_PROTOCOL),
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
                    List<String> path =  map.getShortestPath(((SituatedAgent10)myAgent).getCurrentPosition().toString(),content);
                    for(String node:path){
                        if(agents.values().contains(node))path=null;
                    }
                    if(path!=null){
                        ACLMessage reply = requestMenssage.createReply(ACLMessage.AGREE);
                        reply.setContent(content);
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
        private Boolean takeResource;
        public GoPosBehaviour(String dest,ACLMessage requestMenssage) {
            super();
            this.dest = dest;
            this.fail=false;
            this.finished=false;
            this.lastPos="";
            this.requestMenssage=requestMenssage;
            goingPos = false;
        }

        @Override
        public void action() {
            try {
                myAgent.doWait(DOWAIT);
            } catch (Exception e) {
                e.printStackTrace();
            }

            takeResource = false;
            SituatedAgent10 agent = (SituatedAgent10) myAgent;
            String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition().toString();

            observe(agent, myPosition);

            if(myPosition.equals(dest)){
                if(agent.type.equals("AgentCollect") && agent.observe().get(0).getRight().size()>0)
                    addBehaviour(new TakeResourceBehaviour(requestMenssage));
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
            emptyMyResource(agent);
            agent.moveTo(new gsLocation(nextNode));

        }

        public void emptyMyResource(SituatedAgent10 agent) {
            if (agent.type.equals("AgentCollect")) {
                for (String tankerName : tankers) {
                    Boolean success = agent.emptyMyBackPack(tankerName);
                    if(success){
                        sendObserve("Empty");
                    }
                }
            }
        }

        public void observe(SituatedAgent10 agent, String myPosition){
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
            shareInfo();
        }
        public void sendObserve(String content){
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setProtocol("BDI10");
            msg.setSender(myAgent.getAID());
            msg.addReceiver(agentBDIAID);
            msg.setContent(content);
            send(msg);
        }

        public void shareInfo(){
            //sendMap();
        }

        private void sendMap() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setProtocol("SHARE-TOPO");
            msg.setSender(this.myAgent.getAID());
            for (String agentName : agents.keySet()) {
                if (agentName.equals(myAgent.getLocalName())) continue;
                msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
            }
            try {
                SerializableSimpleGraph<String, MapRepresentation.MapAttribute> sg = map.getSerializableGraph();
                msg.setContentObject(sg);
            } catch (IOException e) {
                e.printStackTrace();
            }
            sendMessage(msg);
        }
        public void sendInformDone(){
            ACLMessage msg = this.requestMenssage.createReply(ACLMessage.CONFIRM);
            msg.setProtocol(BDI_MESSAGE_PROTOCOL);
            msg.setSender(myAgent.getAID());
            msg.addReceiver(agentBDIAID);
            msg.setContent(dest);
            send(msg);
        }
        public void sendFailure(String menssage){
            ACLMessage msg = this.requestMenssage.createReply(ACLMessage.FAILURE);
            msg.setProtocol(BDI_MESSAGE_PROTOCOL);
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
    public class TakeResourceBehaviour extends Behaviour{
        private Boolean finished = false;
        private ACLMessage requestMenssage;
        public TakeResourceBehaviour(ACLMessage requestMensage){
            this.requestMenssage = requestMensage;
        }
        @Override
        public void action(){
            //System.out.println(this.myAgent.getLocalName() + " - My treasure type is : " + ((AbstractDedaleAgent) this.myAgent).getMyTreasureType());
            ((AbstractDedaleAgent) this.myAgent).openLock(Observation.ANY_TREASURE);
            //System.out.println(this.myAgent.getLocalName() + " - My current backpack capacity is:" + ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace());
            System.out.println(this.myAgent.getLocalName() + " - The agent grabbed :" + ((AbstractDedaleAgent) this.myAgent).pick());
            //System.out.println(this.myAgent.getLocalName() + " - the remaining backpack capacity is: " + ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace());
            List<Couple<Observation, Integer>> l = ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace();
            //En teoria este mira el recurso que queda. Pero falta comprovar
            //Object test = ((AbstractDedaleAgent) this.myAgent).observe();
            try{
                String remainResource = String.valueOf(((AbstractDedaleAgent) this.myAgent).observe().get(0).getRight().get(0).getLeft());
                if((remainResource == "Diamond" && l.get(1).getRight() == 0) || (remainResource == "Gold" && l.get(0).getRight() == 0) || remainResource != "Gold" || remainResource != "Diamond") {
                    finished = true;
                }
            }catch (IndexOutOfBoundsException e){
                System.out.println(e);
                finished=true;
            }
        }
        public void sendInformDone(){
            ACLMessage msg;
            msg = requestMenssage.createReply(ACLMessage.CONFIRM);
            msg.setProtocol(BDI_MESSAGE_PROTOCOL);
            msg.setSender(myAgent.getAID());
            msg.addReceiver(agentBDIAID);
            if (((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace().get(0).getRight() == 0);

            msg.setContent("TAKE "+((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace().get(0).getRight() + " " + ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace().get(1).getRight());
            send(msg);
        }
        @Override
        public boolean done(){
            if(finished) sendInformDone();
            return finished;
        }
    }
    public class ShareInfoBehaviour extends CyclicBehaviour {

        public ShareInfoBehaviour(Agent agent) {
            super(agent);
        }

        @Override
        public void action() {
            SituatedAgent10 agent = (SituatedAgent10) myAgent;
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("SHARE-TOPO"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
            if (msgReceived != null) {
                try {
                    SerializableSimpleGraph<String, MapRepresentation.MapAttribute> sgreceived =
                            (SerializableSimpleGraph<String,MapRepresentation.MapAttribute>) msgReceived.getContentObject();
                    agent.map.mergeMap(sgreceived);
                    //System.out.println(myAgent.getLocalName() + " - Map recived");
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public class RecibeMenssageBehaviour extends CyclicBehaviour{

        @Override
        public void action() {

            ACLMessage tankerMsgReceived = myAgent.receive(MessageTemplate.MatchProtocol(ReceiveTreasureTankerBehaviour.PROTOCOL_TANKER));
            if (tankerMsgReceived != null) {
                if (tankerMsgReceived.getContent() != null) {
                    ACLMessage msg = tankerMsgReceived.createReply(ACLMessage.AGREE);
                    msg.setContent("Ok");
                    send(msg);
                }
            }



            MessageTemplate requestTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(BDI_MESSAGE_PROTOCOL),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            MessageTemplate shareMapTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("SHARE-TOPO"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            MessageTemplate agreeTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(BDI_MESSAGE_PROTOCOL),
                    MessageTemplate.MatchPerformative(ACLMessage.AGREE));
            MessageTemplate msgTemplate = MessageTemplate.not(
                    MessageTemplate.or(MessageTemplate.or(shareMapTemplate,MessageTemplate.MatchProtocol(ReceiveTreasureTankerBehaviour.PROTOCOL_TANKER)),
                            MessageTemplate.or(requestTemplate,agreeTemplate)
                    )
            );
            ACLMessage msgReceived = myAgent.receive(msgTemplate);
            if (msgReceived != null) {
                if (msgReceived.getContent() != null) {
                    System.out.println("Recive trash "+msgReceived.getContent());
                }
            }
        }
    }

}