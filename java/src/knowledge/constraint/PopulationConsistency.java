/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.constraint;

import aos.creditassigment.CreditFunctionInputType;
import aos.creditassigment.CreditFitnessFunctionType;
import aos.creditassigment.Credit;
import aos.creditassigment.CreditDefinedOn;
import aos.creditassignment.setcontribution.AbstractPopulationContribution;
import java.util.Collection;
import java.util.HashMap;
import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;

/**
 * 
 *
 * @author Nozomi
 */
public class PopulationConsistency extends AbstractPopulationContribution {

    /**
     * Maps which operators are responsible for each constraint
     */
    private final HashMap<Variation, String> operatorConstraintMap;

    /**
     * Constructor to specify how to update the probability of applying each
     * operator based on the consistency of solutions in a population to a
     * constraint
     *
     * @param operatorConstraintMap
     */
    public PopulationConsistency(HashMap<Variation, String> operatorConstraintMap) {
        super();
        this.operatesOn = CreditDefinedOn.ARCHIVE;
        this.fitType = CreditFitnessFunctionType.Do;
        this.inputType = CreditFunctionInputType.CS;
        this.operatorConstraintMap = operatorConstraintMap;
    }

    @Override
    public String toString() {
        return "ArchiveConsistency";
    }

    /**
     *
     * @param population for this implementation it should be the current
     * population
     * @param operators
     * @param iteration
     * @return
     */
    @Override
    public HashMap<Variation, Credit> compute(Population population, Collection<Variation> operators, int iteration) {
        HashMap<Variation, Credit> out = new HashMap();
        for (Variation op : operatorConstraintMap.keySet()) {
            int consistentCount = 0;
            for (Solution s : population) {
                if((double)s.getAttribute(operatorConstraintMap.get(op)) == 0){
                    consistentCount++;
                }
            }
            double probability = Math.max((double)consistentCount / (double)population.size(), 0.03);
            out.put(op, new Credit(iteration, probability));
        }
        return out;
    }
}
