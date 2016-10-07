/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eoss.problem;

import architecture.pattern.Assigning;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.core.Variation;

/**
 * Problem specific repair operator. Removes instruments intended for GEO from LEO and vice versa
 * @author nozomihitomi
 */
public class RepairOperator implements Variation{
    
     @Override
    public int getArity() {
        return 1;
    }

    @Override
    public Solution[] evolve(Solution[] parents) {
        Solution result = parents[0].copy();

        for (int i = 0; i < result.getNumberOfVariables(); i++) {
            Variable variable = result.getVariable(i);

            if (variable instanceof Assigning) {
                evolve((Assigning) variable);
            }
        }

        return new Solution[]{result};
    }
    
    public void evolve(Assigning parent){
        for(int i=1; i<parent.getNumberOfLHS(); i++){
            if(parent.isConnected(0, i))
                parent.disconnect(0, i);
        }
        
    }
}
