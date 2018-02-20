/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.constraint;

import aos.operatorselectors.AdaptivePursuit;
import java.util.HashMap;
import org.moeaframework.core.Variation;

/**
 *
 * @author nozomihitomi
 */
public class AdaptiveConstraintSelector extends AdaptivePursuit{
    
    private final KnowledgeStochasticRanking ksr;

    /**
     * Maps which operators are responsible for each constraint. The operators
     * in the map are placeholders for constraints
     */
    private final HashMap<Variation, String> operatorConstraintMap;

    public AdaptiveConstraintSelector(KnowledgeStochasticRanking ksr, HashMap<Variation, String> operatorConstraintMap,
            double alpha, double beta, double pmin) {
        super(operatorConstraintMap.keySet(), alpha, beta, pmin);
        this.ksr = ksr;
        this.operatorConstraintMap = operatorConstraintMap;
    }

    @Override
    public Variation nextOperator() {
        Variation var = super.nextOperator();
        for(Variation v : operatorConstraintMap.keySet()){
            ksr.updateProbability(operatorConstraintMap.get(v), 0.0);
        }
        ksr.updateProbability(operatorConstraintMap.get(var), 1.0);
        return var;
    }
    
}
