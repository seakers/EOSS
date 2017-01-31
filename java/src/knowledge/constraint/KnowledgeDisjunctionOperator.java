/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.constraint;

import eoss.problem.assignment.InstrumentAssignmentArchitecture2;
import java.util.HashSet;
import knowledge.operator.RandomKnowledgeOperator;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;

/**
 * Checks to see if a solution meets at least one of the constraints. If not a
 * random operator is selected to operate on the solution
 *
 * @author nozomihitomi
 */
public class KnowledgeDisjunctionOperator implements Variation{
    
    private final HashSet<String> constraints;
    
    private final Variation rko;

    public KnowledgeDisjunctionOperator(RandomKnowledgeOperator rko) {
        this.rko = rko;
        
        constraints =  new HashSet<>();
        constraints.add("dcViolationSum");
        constraints.add("massViolationSum");
        constraints.add("packingEfficiencyViolationSum");
        constraints.add("instrumentOrbitAssingmentViolationSum");
        constraints.add("synergyViolationSum");
        constraints.add("interferenceViolationSum");
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    public Solution[] evolve(Solution[] sltns) {
        InstrumentAssignmentArchitecture2 child = (InstrumentAssignmentArchitecture2) sltns[0];
        
        //check all constraints
        for(String attrib : constraints){
            if((double)child.getAttribute(attrib) == 0){
                return new Solution[]{child.copy()};
            }
        }
        
        return rko.evolve(sltns);
    }
    
    
    
}
