/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.constraint.operator;

import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;

/**
 * This operator is used in conjunction with a map of constraints to operators.
 * This operator, specifically does not modify solutions but is used as a place
 * holder for the AOS-style methods in credit assignment and operator selection.
 * Selected constraintoperators will determine which constraints are applied
 * during evaluation
 *
 * @author nozomihitomi
 */
public abstract class AbstractConstraintOperator implements Variation {

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    public Solution[] evolve(Solution[] sltns) {
        return sltns;
    }

}
