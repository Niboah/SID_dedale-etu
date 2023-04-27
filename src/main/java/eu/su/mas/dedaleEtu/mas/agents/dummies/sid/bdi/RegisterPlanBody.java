package eu.su.mas.dedaleEtu.mas.agents.dummies.sid.bdi;

import bdi4jade.plan.Plan;
import bdi4jade.plan.planbody.AbstractPlanBody;
import bdi4jade.plan.planbody.BeliefGoalPlanBody;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import static eu.su.mas.dedaleEtu.mas.agents.dummies.sid.bdi.Constants.I_AM_REGISTERED;

public class RegisterPlanBody extends BeliefGoalPlanBody {
    @Override
    public void execute() {
        Agent agent = this.myAgent;
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(agent.getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setName("deliberative-agent");
        sd.setType("bdi");
        try {
            DFService.register(this.myAgent, dfd);
            getBeliefBase().updateBelief(I_AM_REGISTERED, true);
            // This is valid but redundant
            // (because the goal implementation will check the belief anyway):
            // setEndState(Plan.EndState.SUCCESSFUL);
        } catch (FIPAException e) {
            setEndState(Plan.EndState.FAILED);
            e.printStackTrace();
        }
    }
}
