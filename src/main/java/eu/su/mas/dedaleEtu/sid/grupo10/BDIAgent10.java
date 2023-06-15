package eu.su.mas.dedaleEtu.sid.grupo10;

import bdi4jade.belief.Belief;
import bdi4jade.belief.TransientBelief;
import bdi4jade.belief.TransientPredicate;
import bdi4jade.core.GoalUpdateSet;
import bdi4jade.core.SingleCapabilityAgent;
import bdi4jade.event.GoalEvent;
import bdi4jade.event.GoalListener;
import bdi4jade.goal.*;
import bdi4jade.plan.DefaultPlan;
import bdi4jade.plan.Plan;
import bdi4jade.plan.planbody.BeliefGoalPlanBody;
import bdi4jade.reasoning.DefaultBeliefRevisionStrategy;
import bdi4jade.reasoning.DefaultDeliberationFunction;
import bdi4jade.reasoning.DefaultOptionGenerationFunction;
import bdi4jade.reasoning.DefaultPlanSelectionStrategy;
import dataStructures.serializableGraph.SerializableNode;
import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.Serializable;
import java.util.*;

public class BDIAgent10 extends SingleCapabilityAgent {
    public static String BDI_MESSAGE_PROTOCOL="BDI10";
    private static String BDI_DF_TYPE="AgentBDI10";
    private static String AGENT_NAME="SituatedAgent10";
    public enum STATE{SLEEPING, READY, RUNNING, FAIL, WAIT,FINISH,COLLECT}
    public enum AGENT_TYPE{EXPLO,COLLECT,TANKER}
    private static String I_AM_REGISTERED = "IAmRegistered";
    private static String I_KNOW_THE_AGENT = "IKnowTheAgent";
    private static String I_KNOW_ALL_MAP = "IKnowAllMap";
    private static String I_KNOW_INFO = "IKnowInfo";
    private static String IN_FINISH_STATE = "InFinishState";
    private static String I_COLLECT = "IAmCollecting";
    private static String I_STORE = "IAmStoring";
    private AID agentAID;
    public STATE state;
    private AGENT_TYPE agent_type;
    private List<String> openNodes; //Nodes known but no visited
    private Set<String> closedNodes; //Visited node
    private Queue<String> resourceNodes; //Node with resource
    private Map<String, Couple<Long,String>> recursos; // Nodo : (Recurso:Quatitat)
    private Map<String,Set<String>> adjList;
    private String goal;
    private String currentNode;
    private Set<String> failList;
    private Boolean goCollect = false;
    private String tresureType;
    private Boolean iAmFull = false;
    private int log;

    public BDIAgent10(){

        openNodes = new ArrayList<>();
        closedNodes = new HashSet<>();
        recursos = new HashMap<>();
        adjList = new HashMap<>();
        failList=new HashSet<>();
        resourceNodes = new LinkedList<>();

        goal="-1";
        state = STATE.SLEEPING;
        log = 0;

        // Create initial beliefs
        Belief iAmRegistered = new TransientPredicate(I_AM_REGISTERED, false);
        Belief findAgent = new TransientPredicate(I_KNOW_THE_AGENT, false);
        Belief iKnowAllMap = new TransientPredicate(I_KNOW_ALL_MAP, false);
        Belief iKonwInfo = new TransientPredicate(I_KNOW_INFO,false);
        Belief iUpdateMyState = new TransientBelief(IN_FINISH_STATE, state);
        Belief iCollect = new TransientPredicate(I_COLLECT,false);
        Belief iStore =new TransientPredicate(I_STORE,false);


        // Add initial desires
        Goal registerGoal = new PredicateGoal(I_AM_REGISTERED, true);
        addGoal(registerGoal);
        Goal findAgentGoal = new PredicateGoal(I_KNOW_THE_AGENT,true);
        addGoal(findAgentGoal);

        Goal exploreGoal = new PredicateGoal(I_KNOW_ALL_MAP,true);
        addGoal(exploreGoal);

        Goal getInfoGoal  = new PredicateGoal(I_KNOW_INFO,true);
        addGoal(getInfoGoal);

        Goal updateStateGoal = new BeliefValueGoal(IN_FINISH_STATE,STATE.FINISH);
        addGoal(updateStateGoal);

        Goal collectGoal = new PredicateGoal(I_COLLECT, true);
        addGoal(collectGoal);

        Goal storeGoal=new PredicateGoal(I_STORE,true);
        addGoal(storeGoal);

        // Declare goal templates
        GoalTemplate registerGoalTemplate = matchesGoal(registerGoal);
        GoalTemplate findAgentGoalTemplate = matchesGoal(findAgentGoal);

        GoalTemplate collectGoalTemplate = matchesGoal(collectGoal);
        GoalTemplate exploreGoalTemplate = matchesGoal(exploreGoal);
        GoalTemplate storeGoalTemplate= matchesGoal(storeGoal);

        GoalTemplate updateStateGoalTemplate = matchesGoal(updateStateGoal);

        GoalTemplate getInfoGoalTemplate = matchesGoal(getInfoGoal);

        // Assign plan bodies to goals
        Plan registerPlan = new DefaultPlan(registerGoalTemplate, RegisterPlanBody.class);
        Plan findAgentPlan = new DefaultPlan(findAgentGoalTemplate,FindAgentPlanBody.class);

        Plan collectPlan = new DefaultPlan(collectGoalTemplate, CollectAllPlanBody.class);
        Plan explorePlan = new DefaultPlan(exploreGoalTemplate,ExploreMapPlanBody.class);
        Plan storePlan= new DefaultPlan(storeGoalTemplate,StoreAllPlanBody.class);

        Plan updateStatePlan = new DefaultPlan(updateStateGoalTemplate, UpdateStatePlanBody.class);

        Plan reciveInfoPlan = new DefaultPlan(getInfoGoalTemplate, ReceiveInformPlanBody.class);

        // Init plan library
        getCapability().getPlanLibrary().addPlan(registerPlan);
        getCapability().getPlanLibrary().addPlan(findAgentPlan);

        getCapability().getPlanLibrary().addPlan(collectPlan);
        getCapability().getPlanLibrary().addPlan(explorePlan);
        getCapability().getPlanLibrary().addPlan(storePlan);
        getCapability().getPlanLibrary().addPlan(updateStatePlan);

        getCapability().getPlanLibrary().addPlan(reciveInfoPlan);

        // Init belief base
        getCapability().getBeliefBase().addBelief(iAmRegistered);
        getCapability().getBeliefBase().addBelief(findAgent);
        getCapability().getBeliefBase().addBelief(iKnowAllMap);
        getCapability().getBeliefBase().addBelief(iCollect);
        getCapability().getBeliefBase().addBelief(iStore);
        getCapability().getBeliefBase().addBelief(iUpdateMyState);
        getCapability().getBeliefBase().addBelief(iKonwInfo);

        // Add a goal listener to track events
        enableGoalMonitoring();

        // Override BDI cycle meta-functions, if needed
        overrideBeliefRevisionStrategy();
        overrideOptionGenerationFunction();
        overrideDeliberationFunction();
        overridePlanSelectionStrategy();
    }
    public class RegisterPlanBody extends BeliefGoalPlanBody {
        @Override
        public void execute() {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setName(getLocalName());
            sd.setType(BDI_DF_TYPE);
            dfd.addServices(sd);
            try {
                DFService.register(myAgent, dfd);
                getBeliefBase().updateBelief(I_AM_REGISTERED, true);
            } catch (FIPAException e) {
                setEndState(Plan.EndState.FAILED);
                e.printStackTrace();
            }
        }
    }
    public class FindAgentPlanBody extends BeliefGoalPlanBody{
        @Override
        public void execute() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            //Todo
            //templateSd.setName(AGENT_NAME);
            String agentName =myAgent.getLocalName().substring(4);
            templateSd.setName(agentName);

            template.addServices(templateSd);
            try {
                DFAgentDescription[] results = DFService.search(myAgent, template);
                if (results.length > 0) {
                    for(DFAgentDescription dfd: results){
                        agentAID = dfd.getName();
                        System.out.println("-----"+myAgent.getLocalName()+" detect "+agentAID.getLocalName());
                        getBeliefBase().updateBelief(I_KNOW_THE_AGENT, true);
                    }
                }
            } catch (FIPAException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public class UpdateStatePlanBody extends BeliefGoalPlanBody{
        @Override
        public void execute() {
            receiveRequest();
            receiveRefuse();
            receiveAgree();
            receiveInformDone();
            receiveFailure();
            getBeliefBase().updateBelief(IN_FINISH_STATE, state);
        }

        private ACLMessage receive(int performative){
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(BDI_MESSAGE_PROTOCOL),
                    MessageTemplate.MatchPerformative(performative));
            return myAgent.receive(msgTemplate);
        }
        private void receiveRequest(){
            ACLMessage requestMenssage = receive(ACLMessage.REQUEST);
            String[] content; //InitPos:AgentType:TresureType
            if (requestMenssage == null) return;
            if (requestMenssage.getContent() == null)  return;

            content = requestMenssage.getContent().split(":");

            if(!state.equals(STATE.SLEEPING)) return;

            tresureType=content[2];
            switch (content[1]){
                case "AgentExplo":
                    agent_type = AGENT_TYPE.EXPLO;
                    getBeliefBase().updateBelief(I_COLLECT, true);
                    getBeliefBase().updateBelief(I_STORE, true);
                    break;
                case "AgentCollect":
                    agent_type = AGENT_TYPE.COLLECT;
                    getBeliefBase().updateBelief(I_STORE, true);
                    getBeliefBase().updateBelief(I_KNOW_ALL_MAP, true);
                    break;
                case "AgentTanker":
                    agent_type = AGENT_TYPE.TANKER;
                    getBeliefBase().updateBelief(I_COLLECT, true);
                    getBeliefBase().updateBelief(I_KNOW_ALL_MAP, true);
                    break;
            }
            openNodes.add(content[0]);

            ACLMessage reply = requestMenssage.createReply(ACLMessage.AGREE);
            reply.setContent(content[0]);
            send(reply);

            stateREADY();
        }
        private void receiveAgree(){
            ACLMessage refuseMenssage = receive(ACLMessage.AGREE);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();
                    System.out.println(getLocalName()+" RECEIVE AGREE"+"("+refuseMenssage.getConversationId()+") "+content);
                    stateRUNNING();
                }
            }
        }
        private void receiveRefuse(){
            ACLMessage refuseMenssage = receive(ACLMessage.REFUSE);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();
                    System.out.println(getLocalName()+" RECEIVE REFUSE"+"("+refuseMenssage.getConversationId()+") "+content);
                    stateFAIL(content);
                }
            }
        }
        private void receiveInformDone(){
            ACLMessage refuseMenssage = receive(ACLMessage.CONFIRM);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();
                    System.out.println(getLocalName()+" RECEIVE DONE"+"("+refuseMenssage.getConversationId()+") "+content);
                    switch (agent_type){
                        case EXPLO:
                        case TANKER:

                            if(content.equals(goal)){
                                state=STATE.WAIT;
                                failList.clear();
                                if (currentNode!=null && currentNode.equals(goal))
                                    stateREADY();
                            }

                            break;
                        case COLLECT:

                            if(content.equals(goal)){
                                if(goCollect)
                                    state= STATE.COLLECT;
                                else state = STATE.WAIT;
                            }else if(content.contains("TAKE")){
                                String [] info = content.split(" ");
                                if(info[1].equals("0") && info[2].equals("0")) iAmFull = true;
                                //stateREADY();
                            }

                            break;
                    }

                }
            }
        }
        private void receiveFailure(){
            ACLMessage refuseMenssage = receive(ACLMessage.FAILURE);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();
                    System.out.println(getLocalName()+" RECEIVE FAILURE"+"("+refuseMenssage.getConversationId()+") "+content);
                    stateFAIL(content);
                }
            }
        }
    }
    //Buscar nodo objetivo mas proximo
    private String BFS(String s) {
        boolean visited[] = new boolean[1000];

        LinkedList<String> queue = new LinkedList<String>();

        visited[Integer.parseInt(s)] = true;
        queue.add(s);

        while (queue.size() != 0) {

            s = queue.poll();

            Set<String> adj = adjList.get(s);
            if(adj==null) continue;
            Iterator<String> i = adj.iterator();
            while (i.hasNext()) {
                String n = i.next();

                if(failList.contains(n+"")) continue;
                if (!visited[Integer.parseInt(n)]) {
                    visited[Integer.parseInt(n)] = true;
                    queue.add(n);
                }

                if(openNodes.contains(n+"")) return n+"";
            }
        }
        String next ="";
        for (Iterator<String> iter = closedNodes.iterator(); iter.hasNext(); ) {
            next = iter.next();
            if(failList.contains(next)) continue;
            return next;
        }
        return next;
    }
    private void request(Agent agent, String node){

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setProtocol(BDI_MESSAGE_PROTOCOL);
        msg.setSender(agent.getAID());
        msg.addReceiver(agentAID);
        msg.setContent(node);
        log++;
        msg.setConversationId(log+"");
        System.out.println(agent.getLocalName()+" SEND REQUEST"+"("+msg.getConversationId()+") "+node);
        send(msg);

    }
    public class ExploreMapPlanBody extends BeliefGoalPlanBody {
        int time;
        public ExploreMapPlanBody(){
            super();
            time=0;
        }
        @Override
        public void execute() {
            switch (state){
                case WAIT:
                    time=0;
                    if (currentNode!=null && currentNode.equals(goal))
                        stateREADY();
                    break;
                case READY:
                    time=0;
                    if(openNodes.size()>0){
                        if(goal!="-1")break;
                        String next=openNodes.get(0);
                        if(currentNode!=null) next = BFS(currentNode);
                        goal=next;
                        request(myAgent,next);
                    }else if(openNodes.isEmpty()){
                        String aux = closedNodes.iterator().next();
                        closedNodes.clear();
                        openNodes.add(aux);
                        adjList.clear();
                        closedNodes.add(currentNode);
                    }
                    break;
                case FAIL:
                    time=0;
                    if(goal!="-1")break;
                    if(currentNode==null)break;
                    String next = BFS(currentNode);
                    goal=next;
                    request(myAgent,next);
                    break;
                case RUNNING:
                    if(time++>900000000)
                        stateFAIL(goal);
                    break;
            }
        }
    }
    public class CollectAllPlanBody extends BeliefGoalPlanBody {
        int time;
        public CollectAllPlanBody(){
            super();
            time=0;
        }
        @Override
        public void execute() {
            switch (state){
                case WAIT:
                    time=0;
                    if (currentNode!=null && currentNode.equals(goal))
                        stateREADY();
                    break;
                case READY:
                    time=0;
                    if(!resourceNodes.isEmpty() && !iAmFull){
                        if (goal != "-1") break;
                        String next = resourceNodes.poll();
                        resourceNodes.add(next);
                        goal = next;
                        goCollect = true;
                        request(myAgent,next);
                    }else if(openNodes.size()>0){
                        if(goal!="-1")break;
                        String next=openNodes.get(0);
                        if(currentNode!=null) next = BFS(currentNode);
                        goal=next;
                        request(myAgent,next);
                    }else if(openNodes.isEmpty()){
                        String aux = closedNodes.iterator().next();
                        closedNodes.clear();
                        openNodes.add(aux);
                        adjList.clear();
                        closedNodes.add(currentNode);
                    }
                    break;
                case FAIL:
                    time=0;
                    if(goal!="-1")break;
                    if(currentNode==null)break;
                    //stateREADY();
                    String next = BFS(currentNode);
                    goal=next;
                    request(myAgent,next);
                    break;
                case RUNNING:
                    if(time++>900000000){
                        System.out.println(getLocalName()+" RUNNING TIME OUT "+goal);
                        stateFAIL(goal);
                    }

                    break;
                case COLLECT:
                    if(time++>900){
                        System.out.println(getLocalName()+" COLLECT TIME OUT "+goal);
                        stateFAIL(goal);
                    }

                    break;
            }
        }
    }
    public class StoreAllPlanBody extends BeliefGoalPlanBody {
        int time;
        public StoreAllPlanBody() {
            super();
            time=0;
        }
        @Override
        public void execute() {

            switch(state){
                case WAIT:
                    time=0;
                    if (currentNode!=null && currentNode.equals(goal))
                        stateREADY();
                    break;
                case READY:

                    time=0;

                    if(openNodes.size()>0){
                        if(goal!="-1")break;
                        String next=openNodes.get(0);
                        if(currentNode!=null) next = BFS(currentNode);
                        goal=next;

                        request(myAgent,next);
                    }else if(openNodes.isEmpty()){
                        String aux = closedNodes.iterator().next();
                        closedNodes.clear();
                        openNodes.add(aux);
                        adjList.clear();
                        closedNodes.add(currentNode);

                    }
                    break;
                case RUNNING:
                    if(time++>900000000)
                        stateFAIL(goal);
                    break;
                case FAIL:
                    time=0;
                    if(goal!="-1")break;
                    if(currentNode==null)break;
                    //stateREADY();
                    String next = BFS(currentNode);
                    goal=next;
                    request(myAgent,next);
                    break;
            }


        }
    }
    public class ReceiveInformPlanBody extends BeliefGoalPlanBody{
        @Override
        protected void execute() {
            recibeRecursos();
            receiveInform();
            recieveMap();
        }
        public void recieveMap(){
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("BDI10MAP"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage menssage = receive(msgTemplate);
            if (menssage != null) {
                if (menssage.getContent() != null) {
                    try {
                        SerializableSimpleGraph<String, MapRepresentation.MapAttribute> sgreceived =
                                (SerializableSimpleGraph<String, MapRepresentation.MapAttribute>) menssage.getContentObject();
                        //processMap(sgreceived);
                    }catch (Exception e){
                        System.out.println(e);
                    }

                }
            }
        }

        public void receiveInform(){
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(BDI_MESSAGE_PROTOCOL),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage refuseMenssage = receive(msgTemplate);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();
                    processInfo(content);
                }
            }
        }
        public void recibeRecursos(){
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("BDI10R"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage refuseMenssage = receive(msgTemplate);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();
                    String [] l = content.split(";");
                    for(String ll : l){
                        String[] lll = ll.split(":");
                        if(lll.length==3){
                            String node = lll[0];
                            Long a = 0L;
                            if(recursos.containsKey(node))  a = recursos.get(node).getLeft();

                            Long b = Long.valueOf(lll[1]);
                            if(a>=b) continue;

                            recursos.put(lll[0],new Couple<>(Long.valueOf(lll[1]),lll[2]));

                            if(tresureType.equals("Gold")){
                                if(lll[2].equals("Gold") && !resourceNodes.contains(node)){
                                    resourceNodes.add((node));
                                }else if(!lll[2].equals("Gold") && resourceNodes.contains(node)) resourceNodes.remove(node);
                            }else if(tresureType.equals("Diamond")){
                                if(lll[2].equals("Diamond") && !resourceNodes.contains(node)){
                                    resourceNodes.add((node));
                                }else if(!lll[2].equals("Diamond") && resourceNodes.contains(node)) resourceNodes.remove(node);
                            }else{
                                if(resourceNodes.contains(node)) {
                                    if (!lll[2].equals("Gold") && !lll[2].equals("Diamond"))
                                        resourceNodes.remove(node);
                                }else if (lll[2].equals("Gold") || lll[2].equals("Diamond")) resourceNodes.add(node);
                            }
                        }
                    }
                    System.out.println(resourceNodes);
                }
            }
        }
        public void processInfo(String content){
            if(content.equals("Empty")){//Recolector vuelve ha estar vacio
                iAmFull = false;
                return;
            }
            String [] inform = content.split("\n");
            Boolean nearWell=false;
            Set<String> nearList = new HashSet<>();
            if(inform.length>0) currentNode="";
            for(String nodeInf: inform){
                String[] c = nodeInf.split(" ");
                String node = c[0];
                if(currentNode==""){
                    Map aux = new HashMap();

                    currentNode=node;
                    openNodes.remove(node);
                    closedNodes.add(node);
                    int i=0;
                    for(String e:c){
                        String[] info = e.split(":");
                        String name=info[0];
                        String quantity="0";
                        if(i++>0){
                            if(info.length>1) quantity=info[1];
                            aux.put(name,quantity);
                        }
                        if(name.equals("WIND"))nearWell=true;
                    }

                    if(tresureType.equals("Gold")){
                        if(aux.containsKey("Gold") && !resourceNodes.contains(node)){
                            resourceNodes.add((node));
                        }else if(!aux.containsKey("Gold") && resourceNodes.contains(node)) resourceNodes.remove(node);
                    }else if(tresureType.equals("Diamond")){
                        if(aux.containsKey("Diamond") && !resourceNodes.contains(node)){
                            resourceNodes.add((node));
                        }else if(!aux.containsKey("Diamond") && resourceNodes.contains(node)) resourceNodes.remove(node);
                    }else{
                        if(resourceNodes.contains(node)) {
                            if (!aux.containsKey("Gold") && !aux.containsKey("Diamond"))
                                resourceNodes.remove(node);
                        }else if (aux.containsKey("Gold") || aux.containsKey("Diamond")) resourceNodes.add(node);
                    }
                }else{
                    nearList.add(node);
                    if (!closedNodes.contains(node) && !openNodes.contains(node)) openNodes.add(node);

                    for(String e:c){
                        String[] info = e.split(":");
                        String name=info[0];
                        if(name.equals("WIND") && nearWell){
                            openNodes.remove(node);
                            closedNodes.remove(node);
                        }
                    }
                }
                adjList.put(currentNode,nearList);
            }
        }

        public void processMap(SerializableSimpleGraph<String, MapRepresentation.MapAttribute> sgreceived){
            if(agent_type.equals(AGENT_TYPE.EXPLO))return;
            for(SerializableNode<String, MapRepresentation.MapAttribute> node :sgreceived.getAllNodes()){
                String n = node.getNodeId();
                if(node.getNodeContent().equals(MapRepresentation.MapAttribute.closed)){
                    if(openNodes.contains(n)) openNodes.remove(n);
                    closedNodes.add(n);
                    adjList.put(n,sgreceived.getEdges(n));
                }
                if(node.getNodeContent().equals(MapRepresentation.MapAttribute.open)){
                    if(closedNodes.contains(n)) continue;
                    if(openNodes.contains(n)) continue;
                    openNodes.add(n);
                    adjList.put(n,sgreceived.getEdges(n));
                }
            }
        }

    }

    private void stateREADY(){
        state=STATE.READY;
        goal="-1";
        goCollect=false;
        failList.clear();

        System.out.println(getLocalName()+" READY\n");
    }
    private void stateFAIL(String content){
        state=STATE.FAIL;
        failList.add(content);
        failList.add(goal);
        goCollect=false;
        goal="-1";
        System.out.println(getLocalName()+" FAIL "+content);
    }
    private void stateRUNNING(){
        state=STATE.RUNNING;
        System.out.println(getLocalName()+" RUNNING");
    }

    private void enableGoalMonitoring() {
        this.addGoalListener(new GoalListener() {
            @Override
            public void goalPerformed(GoalEvent goalEvent) {
                if(goalEvent.getStatus() == GoalStatus.ACHIEVED) {
                    System.out.println(" ------- BDI: " + goalEvent.getGoal() + " " + "fulfilled!");
                }
            }
        });
    }

    private GoalTemplate matchesGoal(Goal goalToMatch) {
        return new GoalTemplate() {
            @Override
            public boolean match(Goal goal) {
                return goal == goalToMatch;
            }
        };
    }

    private void overrideBeliefRevisionStrategy() {
        this.getCapability().setBeliefRevisionStrategy(new DefaultBeliefRevisionStrategy() {
            @Override
            public void reviewBeliefs() {
                // This method should check belief base consistency,
                // make new inferences, etc.
                // The default implementation does nothing
            }
        });
    }

    private void overrideOptionGenerationFunction() {
        this.getCapability().setOptionGenerationFunction(new DefaultOptionGenerationFunction() {
            @Override
            public void generateGoals(GoalUpdateSet agentGoalUpdateSet) {
                // A GoalUpdateSet contains the goal status for the agent:
                // - Current goals (.getCurrentGoals)
                // - Generated goals, existing but not adopted yet (.getGeneratedGoals)
                // - Dropped goals, discarded forever (.getDroppedGoals)
                // This method should update these three sets (current,
                // generated, dropped).
                // The default implementation does nothing
            }
        });
    }

    private void overrideDeliberationFunction() {
        this.getCapability().setDeliberationFunction(new DefaultDeliberationFunction() {
            @Override
            public Set<Goal> filter(Set<GoalUpdateSet.GoalDescription> agentGoals) {
                // This method should choose which of the current goal
                // of the agent should become intentions in this iteration
                // of the BDI cycle.
                // The default implementation chooses all goals with no
                // actual filtering.
                return super.filter(agentGoals);
            }
        });
    }

    private void overridePlanSelectionStrategy() {
        this.getCapability().setPlanSelectionStrategy(new DefaultPlanSelectionStrategy() {
            @Override
            public Plan selectPlan(Goal goal, Set<Plan> capabilityPlans) {
                // This method should return a plan from a list of
                // valid (ordered) plans for fulfilling a particular goal.
                // The default implementation just chooses
                // the first plan of the list.
                return super.selectPlan(goal, capabilityPlans);
            }
        });
    }
}
