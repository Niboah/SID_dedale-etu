package eu.su.mas.dedaleEtu.mas.agents.dummies.sid.bdi;

import bdi4jade.belief.Belief;
import bdi4jade.belief.BeliefBase;
import bdi4jade.goal.AbstractBeliefGoal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;

import java.util.Objects;

public class InferenceGoal<K> extends AbstractBeliefGoal<K> {
    private final Statement statement;

    public InferenceGoal() {
        statement = null;
    }

    public InferenceGoal(K beliefName, Statement statement) {
        super(beliefName);
        this.statement = statement;
    }

    public boolean isAchieved(BeliefBase beliefBase) {
        Belief<?, ?> belief = beliefBase.getBelief(this.beliefName);
        if (belief == null) {
            return false;
        } else {
            Model model = (Model) belief.getValue();
            return model.contains(this.statement);
        }
    }

    public String toString() {
        return this.getClass().getName() + ": " + this.getBeliefName() + " " +
                "infers " + this.statement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InferenceGoal<?> that = (InferenceGoal<?>) o;
        return Objects.equals(statement, that.statement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), statement);
    }
}
