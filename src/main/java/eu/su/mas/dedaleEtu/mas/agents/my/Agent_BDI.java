package eu.su.mas.dedaleEtu.mas.agents.my;

import bdi4jade.belief.Belief;
import bdi4jade.belief.TransientPredicate;
import bdi4jade.core.GoalUpdateSet;
import bdi4jade.event.GoalEvent;
import bdi4jade.event.GoalListener;
import bdi4jade.goal.Goal;
import bdi4jade.goal.GoalStatus;
import bdi4jade.goal.GoalTemplate;
import bdi4jade.goal.PredicateGoal;
import bdi4jade.plan.Plan;
import bdi4jade.core.SingleCapabilityAgent;
import bdi4jade.plan.DefaultPlan;
import bdi4jade.plan.planbody.AbstractPlanBody;
import bdi4jade.plan.planbody.BeliefGoalPlanBody;
import bdi4jade.reasoning.DefaultBeliefRevisionStrategy;
import bdi4jade.reasoning.DefaultDeliberationFunction;
import bdi4jade.reasoning.DefaultOptionGenerationFunction;
import bdi4jade.reasoning.DefaultPlanSelectionStrategy;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
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
    private enum STATE{SLEEPING, READY, RUNNING, FAIL, FINISH}
    public static String I_AM_REGISTERED = "IAmRegistered";
    public static String I_KNOW_THE_AGENT = "IKnowTheAgent";
    public static String I_KNOW_ALL_MAP = "IKnowAllMap";
    public static String I_UPDATE_MY_STATE = "IUpdateMyState";
    private AID agentAID;
    private List<String> historial;
    private STATE state;
    private List<String> openNodes; //Nodes known but no visited
    private Set<String> closedNodes; //Visited node
    private Map<String,String> recursos;
    private Set<String> pozos;

    public Agent_BDI(){
        state = STATE.SLEEPING;
        historial = new ArrayList<>();
        openNodes = new ArrayList<>();
        closedNodes = new HashSet<>();
        recursos = new HashMap<>();
        pozos = new HashSet<>();
        // Create initial beliefs
        Belief iAmRegistered = new TransientPredicate(I_AM_REGISTERED, false);
        Belief findAgent = new TransientPredicate(I_KNOW_THE_AGENT, false);
        Belief iKnowAllMap = new TransientPredicate(I_KNOW_ALL_MAP, false);
        Belief iUpdateMyState = new TransientPredicate(I_UPDATE_MY_STATE, false);
        // Add initial desires
        Goal registerGoal = new PredicateGoal(I_AM_REGISTERED, true);
        addGoal(registerGoal);
        Goal findAgentGoal = new PredicateGoal(I_KNOW_THE_AGENT,true);
        addGoal(findAgentGoal);
        Goal exploreGoal = new PredicateGoal(I_KNOW_ALL_MAP,true);
        addGoal(exploreGoal);
        Goal updateStateGoal = new PredicateGoal(I_UPDATE_MY_STATE,true);
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
       //enableGoalMonitoring();

       // Override BDI cycle meta-functions, if needed
       //overrideBeliefRevisionStrategy();
       //overrideOptionGenerationFunction();
       //overrideDeliberationFunction();
       //overridePlanSelectionStrategy();
   }
    public class RegisterPlanBody extends BeliefGoalPlanBody {
        @Override
        public void execute() {
            System.out.println("RegisterPlanBody");
            Agent agent = this.myAgent;
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(agent.getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setName("AgentBDI");
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
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            templateSd.setType("agentExplo");
            templateSd.setName("Agent_P3");
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

    public class UpdateStatePlanBody extends AbstractPlanBody{
        @Override
        public void action() {
            receiveRequest();
            receiveRefuse();
            receiveAgree();
            receiveInformDone();
            receiveFailure();
            receiveInform();
            if(STATE.FINISH==state){
                setEndState(Plan.EndState.SUCCESSFUL);
                getBeliefBase().updateBelief(I_UPDATE_MY_STATE, true);
            }

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
            if (requestMenssage != null) {
                if (requestMenssage.getContent() != null) {
                    content = requestMenssage.getContent();
                    if(state!=STATE.SLEEPING)return;
                    state=STATE.READY;
                    openNodes.add(content);
                    System.out.println("BDI Receive request "+agentAID+" start in "+content);
                    historial.add("BDI Receive request "+agentAID+" start in "+content);
                }
            }else return;

            ACLMessage reply = requestMenssage.createReply(ACLMessage.AGREE);
            reply.setContent("AGREE");
            send(reply);
            System.out.println("BDI agree");
            historial.add("BDI agree");
        }

        public void receiveRefuse(){
            ACLMessage refuseMenssage = receive(ACLMessage.REFUSE);
            String content="";
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    content = refuseMenssage.getContent();
                    state = STATE.FAIL;
                    System.out.println("BDI Receive refuse "+content);
                    historial.add("BDI Receive refuse "+content);
                }
            }
        }
        public void receiveAgree(){
            ACLMessage refuseMenssage = receive(ACLMessage.AGREE);
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    String content = refuseMenssage.getContent();
                    state = STATE.RUNNING;
                    System.out.println("BDI Receive agree "+content);
                    historial.add("BDI Receive agree "+content);
                }
            }
        }
        public void receiveInformDone(){
            ACLMessage refuseMenssage = receive(ACLMessage.INFORM_IF);
            String content="";
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    content = refuseMenssage.getContent();
                    state = STATE.READY;
                    System.out.println("BDI Receive inform_done "+content);
                    historial.add("BDI Receive inform_done "+content);
                }
            }
        }
        public void receiveFailure(){
            ACLMessage refuseMenssage = receive(ACLMessage.FAILURE);
            String content="";
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    content = refuseMenssage.getContent();
                    state = STATE.FAIL;
                    System.out.println("BDI Receive failure "+content);
                    historial.add("BDI Receive failure "+content);
                }
            }
        }
        public void receiveInform(){
            ACLMessage refuseMenssage = receive(ACLMessage.INFORM);
            String content="";
            if (refuseMenssage != null) {
                if (refuseMenssage.getContent() != null) {
                    content = refuseMenssage.getContent();
                    System.out.println("BDI Receiver inform:\n"+content);
                    historial.add("BDI Receiver inform:\n"+content);

                    String [] inform = content.split("\n");
                    String currentNode="";
                    Boolean nearWell=false;
                    for(String nodeInf: inform){
                        String[] c = nodeInf.split(" ");
                        String node = c[0];
                        if(currentNode==""){
                            currentNode=node;
                            openNodes.remove(node);
                            closedNodes.add(node);
                            for(String e:c){
                                String name=e.split(":")[0];
                                if(name.equals("WIND")) nearWell=true;
                            }
                        }else{
                            if (!closedNodes.contains(node)) {
                                if (!openNodes.contains(node)) {
                                    openNodes.add(node);
                                }
                            }
                            if(nearWell)
                                for(String e:c){
                                    String name=e.split(":")[0];
                                    if(name.equals("WIND")){
                                        pozos.add(node);
                                        openNodes.remove(node);
                                        closedNodes.remove(node);
                                    }
                                }
                        }
                    }
                }
            }
        }
    }


    public class ExploreMapPlanBody extends AbstractPlanBody {
        @Override
        public void action() {
            switch(state){
                case READY:
                    if(openNodes.size()>0){
                        String next = openNodes.get(0);
                        if(pozos.contains(next)){
                            openNodes.remove(0);
                            return;
                        }
                        request(next);
                        state=STATE.RUNNING;
                    }else if(openNodes.isEmpty()){
                        state=STATE.FINISH;
                        System.out.println("FINISH");
                        historial.add("FINISH");
                        getBeliefBase().updateBelief(I_KNOW_ALL_MAP, true);
                        setEndState(Plan.EndState.SUCCESSFUL);
                    }
                    break;
                case RUNNING:

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
            System.out.println("BDI send request "+ node);
            historial.add("BDI send request "+ node);
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
