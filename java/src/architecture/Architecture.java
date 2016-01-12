/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package architecture;

import architecturalpattern.ArchitecturalDecision;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import jess.Fact;
import org.moeaframework.core.Solution;
import architecture.util.FuzzyValue;

/**
 * The architecture is defined by the types of decisions and the values of those
 * decisions
 *
 * @author nozomihitomi
 */
public class Architecture extends Solution {

    private static final long serialVersionUID = -2195550924166538032L;

    private ArrayList<FuzzyValue> fuzzyObjectives;

    private ArrayList<Fact> capabilities;

    /**
     * Explanations for each objective
     */
    private ArrayList<Explanation> explanations;

    public Architecture(Solution solution) {
        super(solution);
        if (solution instanceof Architecture) {
            Architecture arch = (Architecture) solution;
            this.fuzzyObjectives = arch.getFuzzyObjectives();
            this.capabilities = arch.getCapabilities();
            this.explanations = arch.getExplanations();
        }
    }

    public Architecture(Collection<ArchitecturalDecision> decisions, int numberOfObjectives) {
        super(decisions.size(), numberOfObjectives);
        int ind = 0;
        for (ArchitecturalDecision dec : decisions) {
            setVariable(ind, dec);
            ind++;
        }
        this.fuzzyObjectives = new ArrayList<>(numberOfObjectives);
        this.explanations = new ArrayList<>(numberOfObjectives);
        this.capabilities = new ArrayList<>(numberOfObjectives);
        for (int i = 0; i < numberOfObjectives; i++) {
            fuzzyObjectives.add(null);
            explanations.add(null);
            capabilities.add(null);
        }
    }

    public boolean isFeasible() {
        for (int i = 0; i < getNumberOfVariables(); i++) {
            if (!((ArchitecturalDecision) getVariable(i)).isFeasible()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the objective to a fuzzy values at the specified index.
     *
     * @param index index of the objective to set
     * @param val the new fuzzy value of the objective being set
     * @throws IndexOutOfBoundsException if the index is out of range
     * {@code (index < 0) || (index >= getNumberOfObjectives())}
     */
    public void setFuzzyObjective(int index, FuzzyValue val) {
        fuzzyObjectives.set(index, val);
    }

    /**
     * Sets all objectives of this solution to fuzzy values.
     *
     * @param values the new fuzzy objectives for this solution
     * @throws IllegalArgumentException if {@code objectives.length !=
     *         getNumberOfObjectives()}
     */
    public void setFuzzyObjectives(ArrayList<FuzzyValue> values) {
        if (values.size() != fuzzyObjectives.size()) {
            throw new IllegalArgumentException("invalid number of fuzzy objectives");
        }

        for (int i = 0; i < values.size(); i++) {
            fuzzyObjectives.set(i, values.get(i));
        }
    }

    /**
     * Gets the fuzzy value of the specified objective
     *
     * @param index
     * @return
     */
    public FuzzyValue getFuzzyObjective(int index) {
        return fuzzyObjectives.get(index);
    }

    /**
     * Gets the fuzzy values of all the objective
     *
     * @return
     */
    public ArrayList<FuzzyValue> getFuzzyObjectives() {
        return fuzzyObjectives;
    }

    public ArrayList<Fact> getCapabilities() {
        return capabilities;
    }

    /**
     * Gets the explanations for the architecture's objectives.
     *
     * @param index The index of the objective for which to obtain an
     * explanation
     * @return
     */
    public Explanation getExplanation(int index) {
        return explanations.get(index);
    }

    /**
     * Gets the explanations for the all architecture's objectives.
     *
     * @return
     */
    public ArrayList<Explanation> getExplanations() {
        return explanations;
    }

    /**
     *
     * @param index The index of the objective for which to set an explanation
     * @param explanation the explanation describing why the objective value was
     * obtained
     */
    public void setExplanation(int index, Explanation explanation) {
        this.explanations.set(index, explanation);
    }

    /**
     * Sets the capabilities of this architecture
     *
     * @param capabilities
     */
    public void setCapabilities(ArrayList<Fact> capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public Solution copy() {
        return new Architecture(this);
    }

    /**
     * Returns the values of each decision
     *
     * @return
     */
    @Override
    public String toString() {
        String out = "";
        for (int i = 0; i < getNumberOfObjectives(); i++) {
            out += getVariable(i).toString() + "::";
        }
        return out;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        for (int i = 0; i < getNumberOfVariables(); i++) {
            hash = 67 * hash + Objects.hashCode(this.getVariable(i));
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Architecture other = (Architecture) obj;
        for (int i = 0; i < getNumberOfVariables(); i++) {
            if (!this.getVariable(i).equals(other.getVariable(i))) {
                return false;
            }
        }
        return true;
    }

}
