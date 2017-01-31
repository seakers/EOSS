/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.constraint;

import aos.creditassigment.Credit;
import aos.nextoperator.AbstractOperatorSelector;
import java.util.Arrays;
import knowledge.operator.RandomKnowledgeOperator;
import org.moeaframework.core.Variation;

/**
 *
 * @author nozomihitomi
 */
public class AdaptiveConstraintSelection extends AbstractOperatorSelector {
    
    private final RandomKnowledgeOperator rko;

    public AdaptiveConstraintSelection(Variation[] operators) {
        super(Arrays.asList(operators));
        this.rko = new RandomKnowledgeOperator(operators.length, operators);
    }

    @Override
    public Variation nextOperator() {
        return rko;
    }

    @Override
    public void update(Credit credit, Variation vrtn) {
        rko.updateProbability(vrtn, credit.getValue());
    }
}
