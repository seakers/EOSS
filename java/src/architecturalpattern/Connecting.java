/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package architecturalpattern;

import java.util.Collection;
import architecture.util.OrderedPair;
import static architecturalpattern.DecisionPattern.CONNECTING;

/**
 *
 * @author nozomihitomi
 */
public class Connecting extends Assigning implements ArchitecturalDecision {
    private static final long serialVersionUID = -193099801348840331L;

    /**
     * flag for if the graph is directed
     */
    private final boolean isDirected;

    /**
     * The number of nodes in the graph
     */
    private final int nNodes;
    

    
    /**
     * This constuctor creates a graph with no connected edges.
     *
     * @param nNodes
     * @param isDirected flag to determine if graph is directed
     * @param infeasibleEdges
     */
    public Connecting( int nNodes, boolean isDirected, Collection<OrderedPair<Integer>> infeasibleEdges) {
        super(nNodes, nNodes, infeasibleEdges);
        this.isDirected = isDirected;
        this.nNodes = nNodes;
    }
    
    /**
     * This constuctor creates a graph with no connected edges.
     *
     * @param nNodes
     * @param isDirected flag to determine if graph is directed
     */
    public Connecting(int nNodes, boolean isDirected) {
        this(nNodes,isDirected,null);
    }

    /**
     * Connects nodes i and j. If directed, this will only establish a directed
     * edge from i to j. If undirected an undirected edge will be created
     * between i and j. Node count starts with 0.
     *
     * @param i
     * @param j
     * @return the value of the adjacency matrix in cell i,j before the change. True = connected. False = not connected.
     */
    @Override
    public boolean connect(int i, int j) {
        boolean out = this.get(i*nNodes+j);
        if (isDirected) {
            this.set(i*nNodes+j,true);
        } else {
            this.set(i*nNodes+j,true);
            this.set(j*nNodes+i,true);
        }

        return out;
    }

    @Override
    public DecisionPattern getPattern() {
        return CONNECTING;
    }

    @Override
    public int getNumberOfVariables() {
        return super.cardinality();
    }

}
