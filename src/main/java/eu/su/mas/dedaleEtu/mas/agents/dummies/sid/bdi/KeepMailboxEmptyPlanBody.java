package eu.su.mas.dedaleEtu.mas.agents.dummies.sid.bdi;

import bdi4jade.annotation.Parameter;
import bdi4jade.plan.Plan;
import bdi4jade.plan.planbody.AbstractPlanBody;
import jade.lang.acl.ACLMessage;

public class KeepMailboxEmptyPlanBody extends AbstractPlanBody {
    private ACLMessage msgReceived;

    @Override
    public void action() {
        System.out.println("Emptying mailbox: " + this.msgReceived);
        setEndState(Plan.EndState.SUCCESSFUL);
    }

    @Parameter(direction = Parameter.Direction.IN)
    public void setMessage(ACLMessage msgReceived) {
        this.msgReceived = msgReceived;
    }
}
