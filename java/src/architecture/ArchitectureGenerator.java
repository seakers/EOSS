/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package architecture;

/**
 *
 * @author Nozomi Hitomi
 */
import architecturalpattern.Assigning;
import architecturalpattern.Combining;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.ParallelPRNG;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;

public class ArchitectureGenerator implements Initialization {

    /**
     * The problem.
     */
    private final Problem problem;

    /**
     * The initial population size.
     */
    private final int populationSize;

    /**
     * parallel purpose random generator
     */
    private final ParallelPRNG pprng;

    /**
     * type of initialization
     */
    private final String type;

    /**
     * Constructs a random initialization operator.
     *
     * @param problem the problem
     * @param populationSize the initial population size
     */
    public ArchitectureGenerator(Problem problem, int populationSize, String type) {
        this.problem = problem;
        this.populationSize = populationSize;
        this.pprng = new ParallelPRNG();
        this.type = type;
    }

    @Override
    public Solution[] initialize() {
        Solution[] initialPopulation = new Solution[populationSize];

        for (int i = 0; i < populationSize; i++) {
            Solution solution = problem.newSolution();

            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                Variable variable = solution.getVariable(j);
                switch (type) {
                    case "random":
                        randInitialize(variable);
                        break;
                    case "fullfactorial":
                        throw new UnsupportedOperationException("Full factorial enumeration is not yet supported");
                    default:
                        throw new IllegalArgumentException("No such initialization type: " + type);
                }
            }

            initialPopulation[i] = solution;
        }

        return initialPopulation;
    }

    /**
     * Initializes the specified decision variable randomly. This method
     * supports all built-in types, and can be extended to support custom types.
     *
     * @param variable the variable to be initialized
     */
    protected void randInitialize(Variable variable) {
        if (variable instanceof Combining) {
            Combining comb = (Combining) variable;
            comb.setValue(pprng.nextInt(comb.getNumberOfAlternatives()));
        } else if (variable instanceof Assigning) {
            //this covers initialization for both assigning and connecting
            Assigning assign = (Assigning) variable;
//            for (int i = 0; i < assign.getNumberOfLHS(); i++) {
//                for (int j = 0; j < assign.getNumberOfRHS(); j++) {
//                    if(pprng.nextBoolean())
//                        assign.connect(i,j);
//                }
//            }
            assign.connect(0,0);
            assign.connect(6,0);
            assign.connect(7,0);
            assign.connect(11,0);
            assign.connect(1,1);
            assign.connect(6,1);
            assign.connect(10,1);
            assign.connect(5,2);
            assign.connect(6,2);
            assign.connect(4,3);
            assign.connect(5,3);
            assign.connect(11,3);
            assign.connect(5,4);
            assign.connect(7,4);
            assign.connect(8,4);
            assign.connect(9,4);
            assign.connect(10,4);
            
        } else {
            System.err.println("can not initialize unknown type");
        }
    }

}
