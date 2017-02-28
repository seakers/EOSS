/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.constraint;

import aos.creditassigment.Credit;
import aos.nextoperator.AbstractOperatorSelector;
import java.util.Arrays;
import java.util.HashMap;
import org.moeaframework.core.Variation;

/**
 *
 * @author nozomihitomi
 */
public class AdaptiveConstraintSelection extends AbstractOperatorSelector {

    private final KnowledgeStochasticRanking ksr;

    /**
     * This is the variation operator to be used
     */
    private final Variation var;

    /**
     * Maps which operators are responsible for each constraint. The operators
     * in the map are placeholders for constraints
     */
    private final HashMap<Variation, String> operatorConstraintMap;

    public AdaptiveConstraintSelection(KnowledgeStochasticRanking ksr, HashMap<Variation, String> operatorConstraintMap, Variation var) {
        super(Arrays.asList(new Variation[]{var}));
        this.var = var;
        this.ksr = ksr;
        this.operatorConstraintMap = operatorConstraintMap;
    }

    @Override
    public Variation nextOperator() {
        return var;
    }

    @Override
    public void update(Credit credit, Variation vrtn) {
        ksr.updateProbability(operatorConstraintMap.get(vrtn), credit.getValue());
    }
}
