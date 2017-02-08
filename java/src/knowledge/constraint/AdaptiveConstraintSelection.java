/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.constraint;

import aos.creditassigment.Credit;
import aos.nextoperator.AbstractOperatorSelector;
import knowledge.operator.RandomKnowledgeOperator;
import org.moeaframework.core.Variation;

/**
 *
 * @author nozomihitomi
 */
public class AdaptiveConstraintSelection extends AbstractOperatorSelector {
    
    private final RandomKnowledgeOperator rko;
    
    /**
     * This is should be a compound variation operator containing rko 
     */
    private final Variation var;

    public AdaptiveConstraintSelection(RandomKnowledgeOperator rko, Variation var) {
        super(rko.getOperators());
        this.var = var;
        this.rko = rko;
    }

    @Override
    public Variation nextOperator() {
        return var;
    }

    @Override
    public void update(Credit credit, Variation vrtn) {
        rko.updateProbability(vrtn, credit.getValue());
    }
}
