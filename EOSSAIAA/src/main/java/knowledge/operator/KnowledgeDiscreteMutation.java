/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.core.Variation;
import org.moeaframework.core.variable.BinaryVariable;

/**
 * This type of mutation shall capture expert knowledge that will enhance the
 * search efficiency. This mutator only modifies one solution at a time and is
 * intended to only operate on discrete value decision vectors.
 *
 * @author nozomihitomi
 */
public class KnowledgeDiscreteMutation implements Variation, Serializable {

    private static final long serialVersionUID = -2550619371541476502L;

    /**
     * The indices of the variables to mutate
     */
    private final ArrayList<Integer> variablesToMutate;

    /**
     * Hashmap stores the mapping between which variables to mutate and their
     * target values
     */
    private final HashMap<Integer, Integer> mutationMap;

    /**
     * The number of variables to mutate in each call to evolve()
     */
    private final int numVarToMutate;

    /**
     * Creates a new instance of a knowledge-dependent mutation operator that
     * operates on the given variable indices and mutates the variables to the
     * target values. If the user does not want to mutate all variables at once
     * to takes small mutation step sizes, the number of mutations per call to
     * evolve() can be specified
     *
     * @param variablesToMutate The indices of the variables to mutate
     * @param targetValue The target decision value. The indices must match that
     * of variablesToMutate
     * @param numVarToMutate specify the number of variables to mutate at once
     */
    public KnowledgeDiscreteMutation(int[] variablesToMutate, int[] targetValue, int numVarToMutate) {
        if (variablesToMutate.length != targetValue.length) {
            throw new IllegalArgumentException(String.format("Expected the lengths of "
                    + "varaiblesToMutate and targetValue to be equal. "
                    + "Found variablesToMutate contains %d values and "
                    + "targetValue contains %d values", variablesToMutate.length, targetValue.length));
        }
        if (numVarToMutate < 0 || numVarToMutate > targetValue.length) {
            throw new IllegalArgumentException(String.format("Number of "
                    + "varaibles to mutate must be a number in  the range "
                    + "[0, %d]. Found %d", targetValue.length, numVarToMutate));
        }
        this.variablesToMutate = new ArrayList(Arrays.asList(variablesToMutate));
        this.mutationMap = new HashMap<>(targetValue.length);
        for(int i=1; i<targetValue.length; i++){
            mutationMap.put(variablesToMutate[i], targetValue[i]);
        }
        this.numVarToMutate = numVarToMutate;
    }

    /**
     * Creates a new instance of a knowledge-dependent mutation operator that
     * operates on the given variable indices and mutates the variables to the
     * target values. If the user does not want to mutate all variables at once
     * to takes small mutation step sizes, the number of mutations per call to
     * evolve() can be specified
     *
     * @param variablesToMutate The indices of the variables to mutate
     * @param targetValue The target decision value. The indices must match that
     * of variablesToMutate
     */
    public KnowledgeDiscreteMutation(int[] variablesToMutate, int[] targetValue) {
        this(variablesToMutate, targetValue, targetValue.length);
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    public Solution[] evolve(Solution[] parents) {
        Solution result = parents[0].copy();

        Collections.shuffle(variablesToMutate);
        for (int i = 0; i < numVarToMutate; i++) {
            int indexToMutate = variablesToMutate.get(i);
            int targetValue = mutationMap.get(indexToMutate);
            Variable var = result.getVariable(indexToMutate);
            if (var instanceof BinaryVariable) {
                switch (targetValue) {
                    case 0:
                        ((BinaryVariable) var).set(i, false);
                        break;
                    case 1:
                        ((BinaryVariable) var).set(i, true);
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("Expected 0 or 1 value for BinaryVariable. Found %d", targetValue));
                }
            } else {
                throw new UnsupportedOperationException("Currently not supported");
            }
        }

        return new Solution[]{result};
    }

    @Override
    public String toString() {
        String out = "KnowledgeDiscreteMutation{";
        for (int indexToMutate : variablesToMutate) {
            out += String.format(" var%d=%d", indexToMutate, mutationMap.get(indexToMutate));
        }
        out = "}";
        return out;
    }

}
