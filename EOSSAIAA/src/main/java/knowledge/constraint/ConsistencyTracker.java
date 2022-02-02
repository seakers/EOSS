/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.constraint;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;

/**
 * Tracks the consistency of each knowledge-based constraint
 * @author nozomihitomi
 */
public class ConsistencyTracker extends Population{
    
    private final Population pop;
    
    private final HashMap<String, Integer> knowledgeConstraints;

    public ConsistencyTracker(Population pop, HashSet<String> knowledgeConstraints) {
        this.pop = pop;
        this.knowledgeConstraints = new HashMap();
        for(String constraint : knowledgeConstraints){
            this.knowledgeConstraints.put(constraint, 0);
        }
    }

    @Override
    public boolean add(Solution solution) {
        boolean out = pop.add(solution);
        for(String constraint : knowledgeConstraints.keySet()){
            if((double)solution.getAttribute(constraint) == 0){
                int count = knowledgeConstraints.get(constraint);
                knowledgeConstraints.put(constraint, count + 1);
            }
        }
        return out;
    }

    @Override
    public boolean remove(Solution solution) {
        boolean out = pop.remove(solution);
        for(String constraint : knowledgeConstraints.keySet()){
            if((double)solution.getAttribute(constraint) == 0){
                int count = knowledgeConstraints.get(constraint);
                knowledgeConstraints.put(constraint, count - 1);
            }
        }
        return out;
    }

    @Override
    public void remove(int index) {
        Solution solution = pop.get(index);
        pop.remove(index);
        for(String constraint : knowledgeConstraints.keySet()){
            if((double)solution.getAttribute(constraint) == 0){
                int count = knowledgeConstraints.get(constraint);
                knowledgeConstraints.put(constraint, count - 1);
            }
        }
    }

    @Override
    public void replace(int index, Solution solution) {
        Solution incumbent = pop.get(index);
        for(String constraint : knowledgeConstraints.keySet()){
            if((double)incumbent.getAttribute(constraint) == 0){
                int count = knowledgeConstraints.get(constraint);
                knowledgeConstraints.put(constraint, count - 1);
            }
            if((double)solution.getAttribute(constraint) == 0){
                int count = knowledgeConstraints.get(constraint);
                knowledgeConstraints.put(constraint, count + 1);
            }
        }
        pop.replace(index, solution);
    }

    @Override
    public int size() {
        return pop.size();
    }

    @Override
    public Iterator<Solution> iterator() {
        return pop.iterator();
    }

    @Override
    public Solution get(int index) {
        return pop.get(index);
    }

    @Override
    public void clear() {
        pop.clear();
    }

    @Override
    public boolean contains(Solution solution) {
        return pop.contains(solution);
    }

    @Override
    public boolean isEmpty() {
        return pop.isEmpty();
    }

    @Override
    public void truncate(int size, Comparator<? super Solution> comparator) {
        pop.truncate(size, comparator);
    }

    @Override
    public void sort(Comparator<? super Solution> comparator) {
        pop.sort(comparator);
    }

    /**
     *
     * @param constraint
     * @return
     */
    public double getConsistentFraction(String constraint){
        if(knowledgeConstraints.containsKey(constraint)){
            return ((double)knowledgeConstraints.get(constraint))/((double)pop.size());
        }else{
            return 1;
        }
    }
}
