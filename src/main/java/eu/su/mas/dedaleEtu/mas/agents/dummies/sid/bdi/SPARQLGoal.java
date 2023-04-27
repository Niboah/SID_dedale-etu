package eu.su.mas.dedaleEtu.mas.agents.dummies.sid.bdi;

import bdi4jade.belief.Belief;
import bdi4jade.belief.BeliefBase;
import bdi4jade.goal.AbstractBeliefGoal;
import com.github.jsonldjava.shaded.com.google.common.collect.Lists;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SPARQLGoal<K> extends AbstractBeliefGoal<K> {
    private final String queryString;

    public SPARQLGoal() {
        queryString = null;
    }

    public SPARQLGoal(K beliefName, String queryString) {
        super(beliefName);
        this.queryString = queryString;
    }

    public boolean isAchieved(BeliefBase beliefBase) {
        Belief<?, ?> belief = beliefBase.getBelief(this.beliefName);
        if (belief == null) {
            return false;
        } else {
            Model model = (Model) belief.getValue();
            Query query = QueryFactory.create(this.queryString);
            QueryExecution qe = QueryExecutionFactory.create(query, model);
            List<QuerySolution> results = Lists.newArrayList(qe.execSelect());
            boolean matched = !results.isEmpty();
            qe.close();
            return matched;
        }
    }

    public String toString() {
        return this.getClass().getName() + ": " + this.getBeliefName() + " " +
                "infers from query " + this.queryString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SPARQLGoal<?> that = (SPARQLGoal<?>) o;
        return Objects.equals(queryString, that.queryString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), queryString);
    }
}
