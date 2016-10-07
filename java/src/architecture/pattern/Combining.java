/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package architecture.pattern;

import org.moeaframework.core.Variable;
import static architecture.pattern.DecisionPattern.COMBINING;
import org.moeaframework.core.PRNG;

/**
 * 
 * @author nozomihitomi
 */
public class Combining implements ArchitecturalDecision, Variable {
    private static final long serialVersionUID = 4142639957025157845L;

    /**
     * The numbered list of possible alternatives
     */
    private final int[] alternatives;

    /**
     * The current value of the decision
     */
    private int value;

    public Combining(int[] alternatives) {
        this.alternatives = alternatives;
    }
    
    public Combining(int[] alternatives, int value) {
        this(alternatives);
        this.value = value;
    }
    
    /**
     * Choose the index of the alternative
     * @param index the index of the alternative 
     */
    public void setValue(int index){
        this.value = alternatives[index];
    }
    
    public int getValue(){
        return value;
    }

    /**
     * Returns the number of alternatives available for this decision
     * @return 
     */
    public int getNumberOfAlternatives() {
        return alternatives.length;
    }
    
    
    @Override
    public void randomize(){
        setValue(PRNG.nextInt(alternatives.length));
    } 
    

    @Override
    public DecisionPattern getPattern() {
        return COMBINING;
    }

    /**
     * This will return true if the value of the decision takes one of the
     * alternative values. Else returns false;
     *
     * @return
     */
    @Override
    public boolean isFeasible() {
        for (int alt : alternatives) {
            if (value == alt) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getNumberOfVariables() {
        return 1;
    }

    @Override
    public Variable copy() {
        return new Combining(alternatives,value);
    }

    /**
     * Returns a string containing the value of the decision
     * @return 
     */
    @Override
    public String toString() {
        return String.valueOf(getValue());
    }
    
    

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + this.value;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Combining other = (Combining) obj;
        if (this.value != other.value) {
            return false;
        }
        return true;
    }
    
    

}
