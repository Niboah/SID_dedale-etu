package eu.su.mas.dedaleEtu.mas.agents.dummies.sid;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.RandomWalkBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SayHelloBehaviour;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;

import java.util.ArrayList;
import java.util.List;

public class LabAgent extends AbstractDedaleAgent {
    static class HelloWorldBehaviour extends Behaviour {
        public HelloWorldBehaviour(Agent agent) {
            super(agent);
        }

        @Override
        public void action() {
            System.out.println("Lab agent says: \"Hello world!\"");
        }

        @Override
        public boolean done() {
            return true;
        }
    }

    /**
     * This method is automatically called when "agent".start() is executed.
     * Consider that Agent is launched for the first time.
     * 1) set the agent attributes
     * 2) add the behaviours
     */
    protected void setup() {
        super.setup();
        //use them as parameters for your behaviours is you want
        List<Behaviour> lb = new ArrayList<>();

        // ADD the initial behaviours
        lb.add(new HelloWorldBehaviour(this));

        // MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
        addBehaviour(new startMyBehaviours(this, lb));
    }

    /**
     * This method is automatically called after doDelete()
     */
    protected void takeDown() {
        super.takeDown();
    }

    /**
     * This method is automatically called before migration.
     * You can add here all the saving you need
     */
    protected void beforeMove() {
        super.beforeMove();
    }

    /**
     * This method is automatically called after migration to reload.
     * You can add here all the info regarding the state you want your agent to restart from
     */
    protected void afterMove() {
        super.afterMove();
    }
}
