/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import java.util.Collection;
import java.util.HashMap;
import org.moeaframework.core.FrameworkException;
import org.moeaframework.core.ParallelPRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;
import org.moeaframework.core.operator.CompoundVariation;
import org.moeaframework.core.variable.Permutation;

/**
 * This compound operator applies a series of operators with a specified
 * probability in a random order
 *
 * @author nozomihitomi
 */
public class RandomKnowledgeOperator extends CompoundVariation {

    private static final long serialVersionUID = 3653864833347649396L;

    /**
     * The number of operators to apply
     */
    private final int numberOperators;

    private final ParallelPRNG pprng;

    /**
     * the probabilities with which to apply the operators
     */
    private final HashMap<Variation, Double> probabilities;

    public RandomKnowledgeOperator(int numberOperators) {
        super();
        this.numberOperators = numberOperators;
        this.probabilities = new HashMap<>(numberOperators);
        pprng = new ParallelPRNG();
    }

    public RandomKnowledgeOperator(int numberOperators, Variation... operators) {
        this(numberOperators);

        for (Variation operator : operators) {
            appendOperator(operator);
        }
    }

    @Override
    public void appendOperator(Variation variation) {
        super.appendOperator(variation);
        probabilities.put(variation, 1.0);
    }

    public void updateProbability(Variation variation, double probability) {
        probabilities.replace(variation, probability);
    }
    
    public Collection<Variation> getOperators(){
        return operators;
    }

    @Override
    public Solution[] evolve(Solution[] parents) {
        Solution[] result = new Solution[parents.length];
        for(int i=0; i<parents.length; i++){
            result[i] = parents[i].copy();
        }
        Permutation order = new Permutation(operators.size());
        order.randomize();
        for (int i = 0; i < numberOperators; i++) {
            Variation operator = operators.get(order.get(i));
            if (pprng.nextDouble() <= probabilities.get(operator)) {
                if (result.length == operator.getArity()) {
                    result = operator.evolve(result);
                } else if (operator.getArity() == 1) {
                    for (int j = 0; j < result.length; j++) {
                        result[j] = operator.evolve(new Solution[]{result[j]})[0];
                    }
                } else {
                    throw new FrameworkException("invalid number of parents");
                }
            }
        }
        return result;
    }

    @Override
    public int getArity() {
        return 1;
    }
    
    

}
