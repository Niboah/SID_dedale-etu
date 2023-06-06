package eu.su.mas.dedaleEtu.mas.agents.my;

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
import dataStructures.MapOfMap;
import dataStructures.tuple.Couple;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class Agent_BDI extends SingleCapabilityAgent {
    public static String BDI_MESSAGE_PROTOCOL="BDI10";
    private static String BDI_DF_TYPE="AgentBDI10";
    private static String AGENT_NAME="AGENT10";
    private static String SITUATED_DF_TYPE ="Situated";
    public enum STATE{SLEEPING, READY, RUNNING, FAIL, WAIT,FINISH,COLLECT}
    public enum AGENT_TYPE{EXPLO,COLLECT,TANKER}
    private static String I_AM_REGISTERED = "IAmRegistered";
    private static String I_KNOW_THE_AGENT = "IKnowTheAgent";
    private static String I_KNOW_ALL_MAP = "IKnowAllMap";
    private static String I_KNOW_INFO = "IKnowInfo";
    private static String IN_FINISH_STATE = "InFinishState";
    private static String I_COLLECT = "IAmCollecting";
    private AID agentAID;
    public STATE state;
    private AGENT_TYPE agent_type;
    private List<String> openNodes; //Nodes known but no visited
    private Set<String> closedNodes; //Visited node
    private Queue<String> resourceNodes;
    private Map<String,Map<String,String>> recursos; // Nodo : (Recurso:Quatitat)
    private Map<Integer,List<Integer>> adjList;
    private String goal;
    private String currentNode;
    private Set<String> failList;
    private Boolean goCollect = false;
    private String tresureType;

    public Agent_BDI(){

        openNodes = new ArrayList<>();
        closedNodes = new HashSet<>();
        recursos = new HashMap<>();
        adjList = new HashMap<>();
        failList=new HashSet<>();
        resourceNodes = new LinkedList<>();

        goal="-1";
        state = STATE.SLEEPING;

        // Create initial beliefs
        Belief iAmRegistered = new TransientPredicate(I_AM_REGISTERED, false);
        Belief findAgent = new TransientPredicate(I_KNOW_THE_AGENT, false);
        Belief iKnowAllMap = new TransientPredicate(I_KNOW_ALL_MAP, false);
        Belief iKonwInfo = new TransientPredicate(I_KNOW_INFO,false);
        Belief iUpdateMyState = new TransientBelief(IN_FINISH_STATE, state);
        Belief iCollect = new TransientPredicate(I_COLLECT,false);

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

       // Declare goal templates
       GoalTemplate registerGoalTemplate = matchesGoal(registerGoal);
       GoalTemplate findAgentGoalTemplate = matchesGoal(findAgentGoal);

       GoalTemplate collectGoalTemplate = matchesGoal(collectGoal);
       GoalTemplate exploreGoalTemplate = matchesGoal(exploreGoal);

       GoalTemplate updateStateGoalTemplate = matchesGoal(updateStateGoal);

       GoalTemplate getInfoGoalTemplate = matchesGoal(getInfoGoal);

       // Assign plan bodies to goals
       Plan registerPlan = new DefaultPlan(registerGoalTemplate, RegisterPlanBody.class);
       Plan findAgentPlan = new DefaultPlan(findAgentGoalTemplate,FindAgentPlanBody.class);

       Plan collectPlan = new DefaultPlan(collectGoalTemplate, CollectAllPlanBody.class);
       Plan explorePlan = new DefaultPlan(exploreGoalTemplate,ExploreMapPlanBody.class);

       Plan updateStatePlan = new DefaultPlan(updateStateGoalTemplate, UpdateStatePlanBody.class);

       Plan reciveInfoPlan = new DefaultPlan(getInfoGoalTemplate,ReciveInformPlanBody.class);

        // Init plan library
       getCapability().getPlanLibrary().addPlan(registerPlan);
       getCapability().getPlanLibrary().addPlan(findAgentPlan);

       getCapability().getPlanLibrary().addPlan(collectPlan);
       getCapability().getPlanLibrary().addPlan(explorePlan);

       getCapability().getPlanLibrary().addPlan(updateStatePlan);

       getCapability().getPlanLibrary().addPlan(reciveInfoPlan);

       // Init belief base
       getCapability().getBeliefBase().addBelief(iAmRegistered);
       getCapability().getBeliefBase().addBelief(findAgent);
       getCapability().getBeliefBase().addBelief(iKnowAllMap);
       getCapability().getBeliefBase().addBelief(iCollect);
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
            System.out.println("RegisterPlanBody");
            Agent agent = this.myAgent;
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(agent.getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setName(myAgent.getLocalName());
            sd.setType(BDI_DF_TYPE);
            dfd.addServices(sd);
            try {
                DFService.register(this.myAgent, dfd);
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
            templateSd.setType(SITUATED_DF_TYPE);

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
            getBeliefBase().updateBelief(IN_FINISH_STATE, state);}

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
                    break;
                case "AgentCollect":
                    agent_type = AGENT_TYPE.COLLECT;
                    getBeliefBase().updateBelief(I_KNOW_ALL_MAP, true);
                    break;
                case "AgentTanker":
                    agent_type = AGENT_TYPE.TANKER;
                    getBeliefBase().updateBelief(I_KNOW_ALL_MAP, true);
                    getBeliefBase().updateBelief(I_COLLECT, true);
                    break;
            }
            openNodes.add(content[0]);
            System.out.println(myAgent.getLocalName()+" Receive request "+agentAID+" start in "+content[0]+" "+content[1]+" "+content[2]);

            ACLMessage reply = requestMenssage.createReply(ACLMessage.AGREE);
            reply.setContent("AGREE");
            send(reply);
            System.out.println(myAgent.getLocalName()+" agree");

            stateREADY();
        }
        private void receiveAgree(){
            ACLMessage refuseMenssage = receive(ACLMessage.AGREE);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();
                    System.out.println(myAgent.getLocalName()+" Receive agree "+content);
                    state = STATE.RUNNING;
                }
            }
        }
        private void receiveRefuse(){
            ACLMessage refuseMenssage = receive(ACLMessage.REFUSE);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();
                    System.out.println(myAgent.getLocalName()+" Receive refuse "+content);

                    stateFAIL(content);
                }
            }
        }
        private void receiveInformDone(){
            ACLMessage refuseMenssage = receive(ACLMessage.CONFIRM);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();

                    System.out.println(myAgent.getLocalName()+ " Receive inform_done "+content);
                    switch (agent_type){
                        case EXPLO:
                            if(content.equals(goal)){
                                state=STATE.WAIT;
                                failList.clear();
                                if (currentNode!=null && currentNode.equals(goal))
                                    stateREADY();
                            }
                            break;
                        case COLLECT:

                            if(content.equals(goal)){
                                if(goCollect) state= STATE.COLLECT;
                                else state = STATE.WAIT;
                                failList.clear();
                            }else if(content.contains("TAKE"))
                                stateREADY();

                            break;
                        case TANKER:
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
                    System.out.println(myAgent.getLocalName()+" Receive failure "+content);

                    stateFAIL(content);
                }
            }
        }
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
                        if(currentNode!=null) next = BFS(Integer.parseInt(currentNode));
                        goal=next;
                        request(next);
                    }else if(openNodes.isEmpty()){
                        if(!resourceNodes.isEmpty()){
                            if (goal != "-1") break;
                            String next = resourceNodes.poll();
                            resourceNodes.add(next);
                            goal = next;
                            request(next);
                        }else{
                            state=STATE.FINISH;
                            System.out.println("FINISH");

                            getBeliefBase().updateBelief(I_KNOW_ALL_MAP, true);
                            // setEndState(Plan.EndState.SUCCESSFUL);
                        }

                    }
                    break;
                case FAIL:
                    time=0;
                    if(goal!="-1")break;
                    if(currentNode==null)break;
                    String next = BFS(Integer.parseInt(currentNode));
                    goal=next;
                    request(next);
                    break;
                case RUNNING:
                    if(time++>900000000)
                       stateFAIL(goal);
                    break;
            }
        }

        public void request(String node){
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setProtocol(BDI_MESSAGE_PROTOCOL);
            msg.setSender(myAgent.getAID());
            msg.addReceiver(agentAID);
            msg.setContent(node);
            send(msg);
            System.out.println(myAgent.getLocalName()+" send request "+ node);
        }


    }
    private String BFS(int s) {
        // Mark all the vertices as not visited(By default
        // set as false)
        boolean visited[] = new boolean[1000];

        LinkedList<Integer> queue = new LinkedList<Integer>();

        visited[s] = true;
        queue.add(s);

        while (queue.size() != 0) {

            s = queue.poll();
            System.out.print(s + " ");

            List<Integer> adj = adjList.get(s);
            if(adj==null) continue;
            Iterator<Integer> i = adj.listIterator();
            while (i.hasNext()) {
                int n = i.next();

                if(failList.contains(n+""))
                    continue;

                if (!visited[n]) {
                    visited[n] = true;
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
                    if(!resourceNodes.isEmpty()){
                        if (goal != "-1") break;
                        String next = resourceNodes.poll();
                        resourceNodes.add(next);
                        goal = next;
                        goCollect = true;
                        requestRunning(next);
                    }else if(openNodes.size()>0){
                        if(goal!="-1")break;
                        String next=openNodes.get(0);
                        if(currentNode!=null) next = BFS(Integer.parseInt(currentNode));
                        goal=next;
                        requestRunning(next);
                    }else if(openNodes.isEmpty()){
                        state=STATE.FINISH;
                        System.out.println("FINISH");

                        getBeliefBase().updateBelief(I_KNOW_ALL_MAP, true);
                        // setEndState(Plan.EndState.SUCCESSFUL);
                    }

                    break;
                case FAIL:
                    time=0;
                    if(goal!="-1")break;
                    if(currentNode==null)break;
                    stateREADY();
                    break;
                case RUNNING:
                    if(time++>900000000)
                        stateFAIL(goal);
                    break;
                case COLLECT:
                    if(time++>900)
                        stateFAIL(goal);
                    break;
            }
        }

        public void requestRunning(String node){
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setProtocol(BDI_MESSAGE_PROTOCOL);
            msg.setSender(myAgent.getAID());
            msg.addReceiver(agentAID);
            msg.setContent(node);
            send(msg);
            System.out.println(myAgent.getLocalName()+" send request "+ node);
        }
    }
    public class ReciveInformPlanBody extends BeliefGoalPlanBody{
        @Override
        protected void execute() {
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(BDI_MESSAGE_PROTOCOL),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage refuseMenssage = receive(msgTemplate);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();
                    System.out.println(myAgent.getLocalName()+" Receiver inform:\n"+content);
                    processInfo(content);
                }
            }
        }
        public void processInfo(String content){
            String [] inform = content.split("\n");
            Boolean nearWell=false;
            List<Integer> nearList = new ArrayList<>();
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
                        if(aux.containsKey("Gold")){
                            resourceNodes.add((node));
                        }else if(resourceNodes.contains(node)) resourceNodes.remove(node);
                    }else if(tresureType.equals("Diamond")){
                        if(aux.containsKey("Diamond")){
                            resourceNodes.add((node));
                        }else if(resourceNodes.contains(node)) resourceNodes.remove(node);
                    }else{
                        if(resourceNodes.contains(node)) {
                            if (!aux.containsKey("Gold") && !aux.containsKey("Diamond"))
                                resourceNodes.remove(node);
                        }else resourceNodes.add(node);
                    }


                    recursos.put(node,aux);
                }else{
                    nearList.add(Integer.parseInt(node));
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
                adjList.put(Integer.parseInt(currentNode),nearList);
            }
        }
    }

    private void stateREADY(){
        state=STATE.READY;
        goal="-1";
    }

    private void stateFAIL(String content){
        state=STATE.FAIL;
        goal="-1";
        failList.add(content);
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
