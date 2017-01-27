/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.assignment;

import java.io.Serializable;
import org.moeaframework.core.Solution;
import org.moeaframework.core.comparator.DominanceComparator;

/**
 *
 * Compares solutions based on the Pareto efficiency of their constraints.
 *
 * @author nozomihitomi
 */
public class KnowledgeConstraintComparator implements DominanceComparator,
        Serializable {

    private static final long serialVersionUID = -5411858051618916035L;

    /**
     * Constructs a Pareto constraint comparator.
     */
    public KnowledgeConstraintComparator() {
        super();
    }

    @Override
    public int compare(Solution solution1, Solution solution2) {

        double d1 = (double)solution1.getAttribute("constraint");
        double d2 = (double)solution2.getAttribute("constraint");
        
        if(d1 < d2){
            return -1;
        }else if(d1 > d2){
            return 1;
        }else {
            return 0;
        }
    }
        
}
