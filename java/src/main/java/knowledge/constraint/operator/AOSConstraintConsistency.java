/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.constraint.operator;

import seakers.aos.creditassignment.Credit;
import seakers.aos.operatorselectors.AdaptivePursuit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import knowledge.constraint.ConsistencyTracker;
import org.moeaframework.core.Variation;

/**
 * Hybrid AOS for
 *
 * @author nozomihitomi
 */
public class AOSConstraintConsistency extends AdaptivePursuit {

    private final ConsistencyTracker consistency;
    
    /**
     * Maps which operators are responsible for each constraint. The operators
     * in the map are placeholders for constraints
     */
    private final HashMap<Variation, String> operatorConstraintMap;

    public AOSConstraintConsistency(ConsistencyTracker consistency, HashMap<Variation, String> operatorConstraintMap, 
            Collection<Variation> operators, double alpha, double beta, double pmin) {
        super(operators, alpha, beta, pmin);
        this.consistency = consistency;
        this.operatorConstraintMap = operatorConstraintMap;
    }

    @Override
    protected void updateProbabilities() {
        //get the selection probabilities based on normal adaptive pursuit methods
        super.updateProbabilities();

        //update the probabilities with the consistency measure
        double sum = 0.0;
        Iterator<Variation> iter = operators.iterator();
        while (iter.hasNext()) {
            Variation operator_i = iter.next();
            String constraint = operatorConstraintMap.get(operator_i);
            double newProb = probabilities.get(operator_i) * consistency.getConsistentFraction(constraint);
            sum += newProb;
            probabilities.put(operator_i, newProb);
        }
        
        //renormalize so that it is a valid probability
        iter = operators.iterator();
        while (iter.hasNext()) {
            Variation operator_i = iter.next();
            double newProb = probabilities.get(operator_i) /sum;
            sum += newProb;
            probabilities.put(operator_i, newProb);
        }
    }

}
