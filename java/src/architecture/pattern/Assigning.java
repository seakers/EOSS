/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package architecture.pattern;

import java.util.Collection;
import java.util.Iterator;
import org.moeaframework.core.variable.BinaryVariable; 
import architecture.util.OrderedPair;
import architecture.util.UnorderedPair;
import static architecture.pattern.DecisionPattern.*;
import java.util.BitSet;

/**
 * This pattern is assumes there are two different sets of entities, and an
 * architecture fragment is defined by assigning each entity from one set to any
 * subset of entities from the other set.
 *
 * @author nozomihitomi
 */
public class Assigning extends BinaryVariable implements ArchitecturalDecision {

    private static final long serialVersionUID = -537105464191766990L;

    /**
     * The number of elements to assign (left hand side)
     */
    private final int mNodes;

    /**
     * The number of elements that get elements assigned (right hand side)
     */
    private final int nNodes;

    /**
     * Collection of infeasible edges
     */
    private final Collection<OrderedPair<Integer>> infeasibleAssingments;
    

    /**
     * This constuctor creates an empty assignment matrix with m items assigned to n
     * items
     *
     * @param mNodes the number of elements that are being assigned
     * @param nNodes the number of elements that will have things assigned to
     * it.
     * @param infeasibleAssingments A collection of infeasible assignments
     */
    public Assigning(int mNodes, int nNodes, Collection<OrderedPair<Integer>> infeasibleAssingments) {
        super(mNodes * nNodes);
        this.mNodes = mNodes;
        this.nNodes = nNodes;
        this.infeasibleAssingments = infeasibleAssingments;
    }

    /**
     * This constuctor creates an assignment matrix with m items assigned to n
     * items
     *
     * @param mNodes the number of elements that are being assigned
     * @param nNodes the number of elements that will have things assigned to
     * it.
     */
    public Assigning(int mNodes, int nNodes) {
        this(mNodes, nNodes, null);
    }

    @Override
    public DecisionPattern getPattern() {
        return ASSINGING;
    }

    /**
     * Checks all specified infeasible assignments. If none of the infeasible
     * assignments exist in the assignment matrix, then return true.
     *
     * @return
     */
    @Override
    public boolean isFeasible() {
        Iterator<OrderedPair<Integer>> iter = infeasibleAssingments.iterator();
        while (iter.hasNext()) {
            if (isConnected(iter.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Assigns element i from the left side to element j on the right side.
     * Elements numbered starting with 0.
     *
     * @param i element in the left hand side
     * @param j element in the right hand side
     * @return the value of the assignment matrix in cell i,j before the change.
     * True = assigned. False = not assigned.
     */
    public boolean connect(int i, int j) {
        boolean out = this.get(i * nNodes + j);
        this.set(i * nNodes + j, true);
        return out;
    }
    
    /**
     * Removes the connection between element i from the left side to element j on the right side.
     * Elements numbered starting with 0.
     *
     * @param i element in the left hand side
     * @param j element in the right hand side
     * @return the value of the assignment matrix in cell i,j before the change.
     * True = assigned. False = not assigned.
     */
    public boolean disconnect(int i, int j) {
        boolean out = this.get(i * nNodes + j);
        this.set(i * nNodes + j, false);
        return out;
    }

    /**
     * Checks if element i from the left hand side and element j from the right
     * hand side are assigned. If assigned, returns true. Else false.
     *
     * @param i element in the left hand side
     * @param j element in the right hand side
     * @return if element i from the left hand side and element j from the right
     * hand side are assigned.
     */
    public boolean isConnected(int i, int j) {
        return this.get(i * nNodes + j);
    }

    /**
     * Checks if first member element from the left hand side and second member
     * element from the right hand side are assigned. If assigned, returns true.
     * Else false. Only returns correct output if ordered pair is used. If
     * Unordered pair is used, throws an unsupported exception.
     *
     * @param edge
     * @return if specified edge connected.
     */
    public boolean isConnected(OrderedPair<Integer> edge) {
        if (edge.getClass().equals(UnorderedPair.class)) {
            throw new UnsupportedOperationException("This method does not work with unordered pairs");
        }
        int i = edge.getFirst();
        int j = edge.getSecond();
        return isConnected(i, j);
    }

    @Override
    public Assigning copy() {
        Assigning out =  new Assigning(mNodes, nNodes,infeasibleAssingments);
        BitSet bitset = this.getBitSet();
        for (int i = bitset.nextSetBit(0); i >= 0; i = bitset.nextSetBit(i + 1)) {
            out.set(i, true);
        }
        return out;
    }
    
    @Override
    public int getNumberOfVariables() {
        return super.cardinality();
    }
    
    /**
     * Returns the number of elements on the right hand side of the assigning pattern
     * @return 
     */
    public int getNumberOfLHS(){
        return mNodes;
    }
    
    /**
     * Returns the number of elements on the right hand side of the assigning pattern
     * @return 
     */
    public int getNumberOfRHS(){
        return nNodes;
    }

    /**
     * Gets the assignments that make this variable infeasible
     * @return 
     */
    public Collection<OrderedPair<Integer>> getInfeasibleAssingments() {
        return infeasibleAssingments;
    }
    
    @Override
    public String toString(){
        String str = "";
        for(int i=0; i<super.getNumberOfBits(); i++){
            if(getBitSet().get(i)){
                str += 1;
            }else{
                str += 0;
            }
        }
        return str;
    }

}
