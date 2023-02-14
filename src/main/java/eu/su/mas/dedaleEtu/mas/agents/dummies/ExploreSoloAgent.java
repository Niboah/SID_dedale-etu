package eu.su.mas.dedaleEtu.mas.agents.dummies;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploSoloBehaviour;
import jade.core.behaviours.Behaviour;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * ExploreSolo agent.
 * It explore the map using a DFS algorithm.
 * It stops when all nodes have been visited.
 *  </pre>
 *
 * @author hc
 */

public class ExploreSoloAgent extends AbstractDedaleAgent {
    private static final long serialVersionUID = -6431752665590433727L;

    /**
     * This method is automatically called when "agent".start() is executed.
     * Consider that Agent is launched for the first time.
     * 1) set the agent attributes
     * 2) add the behaviours
     */
    protected void setup() {
        super.setup();

        List<Behaviour> lb = new ArrayList<>();

        // ADD the initial behaviours of the Agent here
        lb.add(new ExploSoloBehaviour(this, null));

        // MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
        addBehaviour(new startMyBehaviours(this, lb));

        System.out.println("the  agent " + this.getLocalName() + " is started");
    }
}
