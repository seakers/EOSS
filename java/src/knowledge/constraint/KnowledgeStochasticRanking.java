/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.constraint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import org.moeaframework.core.ParallelPRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.comparator.DominanceComparator;

/**
 * This compound operator applies a series of operators with a specified
 * probability in a random order
 *
 * @author nozomihitomi
 */
public class KnowledgeStochasticRanking implements DominanceComparator,
        Serializable {

    private static final long serialVersionUID = 3653864833347649396L;

    /**
     * The number of constraints to apply
     */
    private final int numberConstraints;

    /**
     * Parallel pseudo random number generator
     */
    private final ParallelPRNG pprng;

    /**
     * the probabilities with which to apply the constraint (string property of
     * architecture)
     */
    private final HashMap<String, Double> probabilities;

    public KnowledgeStochasticRanking(int numberOperators) {
        super();
        this.numberConstraints = numberOperators;
        this.probabilities = new HashMap<>(numberOperators);
        this.pprng = new ParallelPRNG();
    }

    public KnowledgeStochasticRanking(int numberOperators, Collection<String> constraints) {
        this(numberOperators);

        for (String str : constraints) {
            appendConstraint(str);
        }
    }

    /**
     * The constraint name that is a property of the architecture
     *
     * @param constraint name that is a property of the architecture
     */
    public void appendConstraint(String constraint) {
        probabilities.put(constraint, 1.0);
    }

    /**
     * Updates the probability of applying the constraint
     *
     * @param constraint name that is a property of the architecture
     * @param probability probability of applying the constraint
     */
    public void updateProbability(String constraint, double probability) {
        probabilities.replace(constraint, probability);
    }

    public Collection<String> getConstraints() {
        return probabilities.keySet();
    }

    @Override
    public int compare(Solution solution1, Solution solution2) {
        double constraint1 = 0;
        double constraint2 = 0;
        int numApplied = 0;
        ArrayList<String> constraints = new ArrayList(probabilities.keySet());
        Collections.shuffle(constraints);
        for (String str : constraints) {
            if (numApplied >= numberConstraints) {
                break;
            }
            if (pprng.nextDouble() < probabilities.get(str)) {
                constraint1 += (double) solution1.getAttribute(str);
                constraint2 += (double) solution2.getAttribute(str);
            }
        }

        if (constraint1 < constraint2) {
            return -1;
        } else if (constraint1 > constraint2) {
            return 1;
        } else {
            return 0;
        }
    }
}
