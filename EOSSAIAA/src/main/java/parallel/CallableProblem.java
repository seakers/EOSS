/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallel;

import java.util.concurrent.Callable;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;

/**
 * A problem that implements callable in order to evaluate solutions on multiple threads. 
 * @author nozomihitomi
 */
public class CallableProblem implements Callable<Problem>{
    
    private final Problem problem;
    
    private Solution solution;

    public CallableProblem(Problem problem) {
        this.problem = problem;
    }
    
    public void evaluate(Solution solution){
        this.solution = solution;
    }

    @Override
    public Problem call() throws Exception {
        problem.evaluate(solution);
        return problem;
    }
    
}
