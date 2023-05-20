package eu.su.mas.dedaleEtu.mas.agents.my;

import bdi4jade.belief.Belief;
import bdi4jade.belief.TransientBelief;
import bdi4jade.belief.TransientPredicate;
import bdi4jade.core.GoalUpdateSet;
import bdi4jade.event.GoalEvent;
import bdi4jade.event.GoalListener;
import bdi4jade.goal.*;
import bdi4jade.plan.Plan;
import bdi4jade.core.SingleCapabilityAgent;
import bdi4jade.plan.DefaultPlan;

import bdi4jade.plan.planbody.BeliefGoalPlanBody;
import bdi4jade.reasoning.DefaultBeliefRevisionStrategy;
import bdi4jade.reasoning.DefaultDeliberationFunction;
import bdi4jade.reasoning.DefaultOptionGenerationFunction;
import bdi4jade.reasoning.DefaultPlanSelectionStrategy;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.jena.atlas.iterator.Iter;

import java.util.*;

public class Agent_BDI extends SingleCapabilityAgent {
    private enum STATE{SLEEPING, READY, RUNNING, FAIL, WAIT,FINISH}
    public static String I_AM_REGISTERED = "IAmRegistered";
    public static String I_KNOW_THE_AGENT = "IKnowTheAgent";
    public static String I_KNOW_ALL_MAP = "IKnowAllMap";
    public static String In_FINISH_STATE = "InFinishSte";
    private AID agentAID;
    private List<String> historial;
    private STATE state;
    private List<String> openNodes; //Nodes known but no visited
    private Set<String> closedNodes; //Visited node
    private Map<String,Map<String,String>> recursos; // Recurso : (Nodo:Quatitat)
    private Set<String> pozos;
    private Map<Integer,List<Integer>> adjList;
    private String goal;
    private String currentNode;
    public Set<String> failList;
    public Agent_BDI(){
        state = STATE.SLEEPING;
        historial = new ArrayList<>();
        openNodes = new ArrayList<>();
        closedNodes = new HashSet<>();
        recursos = new HashMap<>();
        pozos = new HashSet<>();
        adjList = new HashMap<>();
        failList=new HashSet<>();
        currentNode="";
        goal="-1";
        // Create initial beliefs
        Belief iAmRegistered = new TransientPredicate(I_AM_REGISTERED, false);
        Belief findAgent = new TransientPredicate(I_KNOW_THE_AGENT, false);
        Belief iKnowAllMap = new TransientPredicate(I_KNOW_ALL_MAP, false);
        Belief iUpdateMyState = new TransientBelief(In_FINISH_STATE, state);
        // Add initial desires
        Goal registerGoal = new PredicateGoal(I_AM_REGISTERED, true);
        addGoal(registerGoal);
        Goal findAgentGoal = new PredicateGoal(I_KNOW_THE_AGENT,true);
        addGoal(findAgentGoal);
        Goal exploreGoal = new PredicateGoal(I_KNOW_ALL_MAP,true);
        addGoal(exploreGoal);
        Goal updateStateGoal = new BeliefValueGoal(In_FINISH_STATE,STATE.FINISH);
        addGoal(updateStateGoal);

       // Declare goal templates
       GoalTemplate registerGoalTemplate = matchesGoal(registerGoal);
       GoalTemplate findAgentGoalTemplate = matchesGoal(findAgentGoal);
       GoalTemplate exploreGoalTemplate = matchesGoal(exploreGoal);
       GoalTemplate updateStateGoalTemplate = matchesGoal(updateStateGoal);

       // Assign plan bodies to goals
       Plan registerPlan = new DefaultPlan(registerGoalTemplate, RegisterPlanBody.class);
       Plan findAgentPlan = new DefaultPlan(findAgentGoalTemplate,FindAgentPlanBody.class);
       Plan explorePlan = new DefaultPlan(exploreGoalTemplate,ExploreMapPlanBody.class);
       Plan updateStatePlan = new DefaultPlan(updateStateGoalTemplate, UpdateStatePlanBody.class);

       // Init plan library
       getCapability().getPlanLibrary().addPlan(registerPlan);
       getCapability().getPlanLibrary().addPlan(findAgentPlan);
       getCapability().getPlanLibrary().addPlan(explorePlan);
       getCapability().getPlanLibrary().addPlan(updateStatePlan);

       // Init belief base
       getCapability().getBeliefBase().addBelief(iAmRegistered);
       getCapability().getBeliefBase().addBelief(findAgent);
        getCapability().getBeliefBase().addBelief(iKnowAllMap);
        getCapability().getBeliefBase().addBelief(iUpdateMyState);

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
            sd.setType("AgentExploBDI");
            dfd.addServices(sd);
            try {
                DFService.register(this.myAgent, dfd);
                getBeliefBase().updateBelief(I_AM_REGISTERED, true);
                // This is valid but redundant
                // (because the goal implementation will check the belief anyway):
                //setEndState(Plan.EndState.SUCCESSFUL);
            } catch (FIPAException e) {
                setEndState(Plan.EndState.FAILED);
                e.printStackTrace();
            }
        }
    }

    public class FindAgentPlanBody extends BeliefGoalPlanBody{
        @Override
        public void execute() {

            String agentName =myAgent.getLocalName().substring(4);
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            templateSd.setType("agentExplo");
            templateSd.setName(agentName);
            template.addServices(templateSd);
            DFAgentDescription[] results = new DFAgentDescription[0];
            try {
                results = DFService.search(myAgent, template);
                if (results.length > 0) {
                    for(DFAgentDescription dfd: results){
                        agentAID = dfd.getName();
                        System.out.println("-----"+myAgent.getLocalName()+" detect "+agentAID.getLocalName());
                        historial.add(myAgent.getLocalName()+" detect "+agentAID.getLocalName());
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
            if(state.equals(STATE.SLEEPING)) receiveRequest();
            else {
                receiveRefuse();
                receiveAgree();
                receiveInformDone();
                receiveFailure();
                receiveInform();
                getBeliefBase().updateBelief(In_FINISH_STATE, state);}
        }

        public ACLMessage receive(int performative){
            MessageTemplate msgTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol("BDI"),
                    MessageTemplate.MatchPerformative(performative));
            return myAgent.receive(msgTemplate);
        }

        public void receiveRequest(){
            ACLMessage requestMenssage = receive(ACLMessage.REQUEST);
            String content="";
            if (requestMenssage == null) return;
            if (requestMenssage.getContent() == null)  return;

            content = requestMenssage.getContent();
            openNodes.add(content);
            System.out.println(myAgent.getLocalName()+" Receive request "+agentAID+" start in "+content);
            historial.add(myAgent.getLocalName()+" Receive request "+agentAID+" start in "+content);

            ACLMessage reply = requestMenssage.createReply(ACLMessage.AGREE);
            reply.setContent("AGREE");
            send(reply);
            System.out.println(myAgent.getLocalName()+" agree");
            historial.add(myAgent.getLocalName()+" agree");
            state=STATE.READY;
        }
        public void receiveAgree(){
            ACLMessage refuseMenssage = receive(ACLMessage.AGREE);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();
                    state = STATE.RUNNING;
                    System.out.println(myAgent.getLocalName()+" Receive agree "+content);
                    historial.add(myAgent.getLocalName()+" Receive agree "+content);
                }
            }
        }
        public void receiveRefuse(){
            ACLMessage refuseMenssage = receive(ACLMessage.REFUSE);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();

                    System.out.println(myAgent.getLocalName()+" Receive refuse "+content);
                    historial.add(myAgent.getLocalName()+" Receive refuse "+content);
                    goal="-1";
                    failList.add(content);
                    state = STATE.FAIL;
                }
            }
        }
        public void receiveInformDone(){
            ACLMessage refuseMenssage = receive(ACLMessage.INFORM_IF);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();

                    System.out.println(myAgent.getLocalName()+ " Receive inform_done "+content);
                    historial.add(myAgent.getLocalName()+ " Receive inform_done "+content);

                    if(content.equals(goal)){
                        state=STATE.WAIT;
                        failList.clear();
                        if (currentNode.equals(goal)) {
                            state = STATE.READY;
                            goal="-1";
                        }
                    }
                }
            }
        }
        public void receiveFailure(){
            ACLMessage refuseMenssage = receive(ACLMessage.FAILURE);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();
                    failList.add(content);
                    goal="-1";
                    state = STATE.FAIL;
                    System.out.println(myAgent.getLocalName()+" Receive failure "+content);
                    historial.add(myAgent.getLocalName()+" Receive failure "+content);
                }
            }
        }
        public void receiveInform(){
            ACLMessage refuseMenssage = receive(ACLMessage.INFORM);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();
                    System.out.println(myAgent.getLocalName()+" Receiver inform:\n"+content);
                    historial.add(myAgent.getLocalName()+" Receiver inform:\n"+content);
                    processInfo(content);
                }
            }
        }

        public void processInfo(String content){
            String [] inform = content.split("\n");
            Boolean nearWell=false;
            List<Integer> nearList=new ArrayList<>();
            if(inform.length>0) currentNode="";
            for(String nodeInf: inform){
                String[] c = nodeInf.split(" ");
                String node = c[0];
                if(currentNode==""){
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
                            Map<String,String> recursoNodo;
                            if(!recursos.containsKey(name)) recursoNodo=new HashMap<>();
                            else recursoNodo = recursos.get(name);
                            recursoNodo.put(node,quantity);
                            recursos.put(name,recursoNodo);
                        }
                        if(name.equals("WIND"))nearWell=true;
                    }
                }else{
                    nearList.add(Integer.parseInt(node));
                    if (!closedNodes.contains(node) && !openNodes.contains(node)) openNodes.add(node);

                    for(String e:c){
                        String[] info = e.split(":");
                        String name=info[0];
                        if(name.equals("WIND") && nearWell){
                            pozos.add(node);
                            openNodes.remove(node);
                            closedNodes.remove(node);
                        }
                    }

                }
                adjList.put(Integer.parseInt(currentNode),nearList);
            }
        }
    }

    public class ExploreMapPlanBody extends BeliefGoalPlanBody {
        @Override
        public void execute() {
            switch (state){
                case WAIT:
                    if (currentNode.equals(goal)) {
                        state = STATE.READY;
                        goal="-1";
                    }
                    break;
                case READY:
                    if(openNodes.size()>0){
                        if(goal!="-1")break;
                        String next=openNodes.get(0);
                        if(!currentNode.equals("")) next = BFS(Integer.parseInt(currentNode));
                        goal=next;
                        request(next);
                    }else if(openNodes.isEmpty()){
                        state=STATE.FINISH;
                        System.out.println("FINISH");
                        historial.add("FINISH");

                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.setProtocol("BDI");
                        msg.setSender(myAgent.getAID());
                        msg.addReceiver(agentAID);
                        msg.setContent("FINISH");
                        send(msg);

                        printResult();

                        getBeliefBase().updateBelief(I_KNOW_ALL_MAP, true);
                        // setEndState(Plan.EndState.SUCCESSFUL);
                    }
                    break;
                case FAIL:
                    if(goal!="-1")break;
                    String next = BFS(Integer.parseInt(currentNode));
                    goal=next;
                    request(next);

                    break;
            }
        }

        public void request(String node){
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setProtocol("BDI");
            msg.setSender(myAgent.getAID());
            msg.addReceiver(agentAID);
            msg.setContent(node);
            send(msg);
            System.out.println(myAgent.getLocalName()+" send request "+ node);
            historial.add(myAgent.getLocalName()+" send request "+ node);
        }

        public void printResult(){
            System.out.println("Result");
            for(String recursoName: recursos.keySet()){
                System.out.println(recursoName);
                String text="";
                Map<String,String> recurso=recursos.get(recursoName);
                for(String node:recurso.keySet())
                    text+=" Nodo "+node+":"+recurso.get(node);
                System.out.println(text);
            }
        }

        private String BFS(int s) {
            // Mark all the vertices as not visited(By default
            // set as false)
            boolean visited[] = new boolean[1000];

            // Create a queue for BFS
            LinkedList<Integer> queue = new LinkedList<Integer>();

            // Mark the current node as visited and enqueue it
            visited[s] = true;
            queue.add(s);

            while (queue.size() != 0) {

                // Dequeue a vertex from queue and print it
                s = queue.poll();
                System.out.print(s + " ");

                // Get all adjacent vertices of the dequeued
                // vertex s If a adjacent has not been visited,
                // then mark it visited and enqueue it
                List<Integer> adj =adjList.get(s);
                if(adj==null)
                    continue;
                Iterator<Integer> i = adj.listIterator();
                while (i.hasNext()) {
                    int n = i.next();

                    //contains no funciona :c
                    Boolean contain=false;
                    for(String f: failList){
                        if(f.equals(Integer.toString(n))){
                            contain = true;
                            break;
                        }
                    }
                    if(contain)
                        continue;

                    if (!visited[n]) {
                        visited[n] = true;
                        queue.add(n);
                    }

                    if(openNodes.contains(n+""))
                        return n+"";
                }
            }
            String next ="";
            for (Iterator<String> iter = closedNodes.iterator(); iter.hasNext(); ) {
                next = iter.next();
                if(failList.contains(next))
                    continue;
                return next;
            }
            return next;
        }
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
