/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.constraint;

import seakers.aos.creditassignment.AbstractSetContribution;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;

/**
 * 
 *
 * @author Nozomi
 */
public class SetConsistency extends AbstractSetContribution {

    /**
     * Maps which operators are responsible for each constraint
     */
    private final HashMap<Variation, String> operatorConstraintMap;

    /**
     * Constructor to specify how to update the probability of applying each
     * operator based on the consistency of solutions in a population to a
     * constraint
     *
     * @param solutionSet solution set to check for consistency
     * @param operatorConstraintMap
     */
    public SetConsistency(Population solutionSet, HashMap<Variation, String> operatorConstraintMap) {
        super(solutionSet, 0);
        this.operatorConstraintMap = operatorConstraintMap;
    }

    @Override
    public String toString() {
        return "ArchiveConsistency";
    }

    /**
     * Computes the selection probabilities based on the consistency of the solutions in the solution set created by each operator 
     * @param operators
     * @return
     */
    @Override
    public Map<String, Double> computeCredit(Set<String> operators) {
        HashMap<String, Double> out = new HashMap<>();
        for (Variation op : operatorConstraintMap.keySet()) {
            int consistentCount = 0;
            for (Solution s : solutionSet) {
                if((double)s.getAttribute(operatorConstraintMap.get(op)) == 0){
                    consistentCount++;
                }
            }
            double probability = Math.max((double)consistentCount / (double)solutionSet.size(), 0.03);
            out.put(op.toString(), probability);
        }
        return out;
    }
}
