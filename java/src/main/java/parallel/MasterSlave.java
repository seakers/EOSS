/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallel;

import java.io.NotSerializableException;
import java.io.Serializable;
import org.moeaframework.algorithm.AbstractAlgorithm;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;

/**
 * This master slave model uses one master that distributes evaluations to multiple slaves and is a wrapper for the desired algorithm 
 * @author nozomihitomi
 */
public class MasterSlave extends AbstractAlgorithm{

    private final AbstractAlgorithm algorithm;
    
    public MasterSlave(AbstractAlgorithm algorithm, int num) {
        super(algorithm.getProblem());
        this.algorithm = algorithm;
    }

    @Override
    public void evaluate(Solution solution) {
        algorithm.evaluate(solution); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void iterate() {
        algorithm.step();
    }

    @Override
    public void evaluateAll(Iterable<Solution> solutions) {
        super.evaluateAll(solutions); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NondominatedPopulation getResult() {
        return algorithm.getResult();
    }

    @Override
    public void setState(Object state) throws NotSerializableException {
        algorithm.setState(state); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Serializable getState() throws NotSerializableException {
        return algorithm.getState(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void terminate() {
        algorithm.terminate(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isTerminated() {
        return algorithm.isTerminated(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void step() {
        algorithm.step(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isInitialized() {
        return algorithm.isInitialized(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Problem getProblem() {
        return algorithm.getProblem(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getNumberOfEvaluations() {
        return algorithm.getNumberOfEvaluations(); //To change body of generated methods, choose Tools | Templates.
    }
    
}
