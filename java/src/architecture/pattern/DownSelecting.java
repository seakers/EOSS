/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package architecture.pattern;

import java.util.BitSet;
import java.util.Collection;
import org.moeaframework.core.variable.BinaryVariable;

/**
 * This is the downselecting architectural pattern where the decision is to
 * select a subset from a set of entities. For each entity a decision is made to
 * select it or not. The decision can be represented as a binary vector.
 *
 * @author nozomihitomi
 */
public class DownSelecting extends BinaryVariable implements ArchitecturalDecision {

    private static final long serialVersionUID = 1082536400479697102L;
    
    private Collection<BitSet> constraints;

    /**
     * Creates a new downselecting decision where all entities are set to "not
     * selected". Constraints are to be specified if certain entities are not allowed to be selected together
     *
     * @param numberOfEntities the number of entities available to choose from
     * @param constraints a collection of BitSet objections that contain invalid combinations of downselections.
     */
    public DownSelecting(int numberOfEntities, Collection<BitSet> constraints) {
        super(numberOfEntities);
        for (int i = 0; i < numberOfEntities; i++) {
            super.set(i, false);
        }
    }
    
    /**
     * Creates a new downselecting decision where all entities are set to "not
     * selected". This constructor assumes that there no constraints in the selections
     *
     * @param numberOfEntities the number of entities available to choose from
     */
    public DownSelecting(int numberOfEntities) {
        this(numberOfEntities,null);
        for (int i = 0; i < numberOfEntities; i++) {
            super.set(i, false);
        }
    }

    /**
     * Creates a downselecting decsion that reflects the binary vector where
     * true bits represents "selected" and false bits represent "not selected".
     *
     * @param binaryVector
     */
    public DownSelecting(BitSet binaryVector) {
        this(binaryVector.length());
        for (int i = 0; i < binaryVector.length(); i++) {
            if (binaryVector.get(i)) {
                super.set(i, true);
            } else {
                super.set(i, false);
            }
        }

    }

    @Override
    public DecisionPattern getPattern() {
        return DecisionPattern.DOWNSELECTING;
    }

    @Override
    public boolean isFeasible() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getNumberOfVariables() {
        return super.getNumberOfBits();
    }

}
