/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.assignment.operators;

import eoss.problem.assignment.InstrumentAssignmentArchitecture;
import eoss.problem.EOSSDatabase;
import eoss.problem.Orbit;
import eoss.problem.evaluation.ArchitectureEvaluatorParams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import org.moeaframework.core.ParallelPRNG;

/**
 * This domain-specific heuristic looks for opportunities where an instrument
 * can be moved to a better orbit. The increase in performance is based on a
 * look up table containing the instrument's assignment in all possible orbits.
 *
 * @author nozomihitomi
 */
public class ImproveOrbit extends AbstractEOSSOperator {
    /**
     * The subset size relates to how many of the top costly superfluous
     * interactions the heuristics should select randomly from
     */
    private final int subsetSize;

    private final ParallelPRNG pprng;
    
    public ImproveOrbit(int subsetSize){
        this.subsetSize = subsetSize;
        this.pprng = new ParallelPRNG();
    }
        
    @Override
    public int getArity() {
        return 1;
    }

    @Override
    protected InstrumentAssignmentArchitecture evolve(InstrumentAssignmentArchitecture child) {
        //Find a random non-empty orbit and its payload 
        int randOrbitIndex = getRandomOrbitWithAtLeastNInstruments(child, 1);
        if (randOrbitIndex == -1) {
            return child;
        }
        Orbit randOrbit = EOSSDatabase.getOrbit(randOrbitIndex);

        ArrayList<Integer> instIndices = new ArrayList<>(child.getInstrumentsInOrbit(randOrbitIndex));
        Collections.shuffle(instIndices);//this sorts orbits in random order
        
        //choose the size of the subset
        int chosensize = pprng.nextInt(subsetSize) + 1;
        
        ArrayList<String> instSubsetNames = new ArrayList<>();
        ArrayList<Integer> instSubsetIndices = new ArrayList<>();
        for(int i=0; i<instIndices.size() && i<chosensize; i++) {
            int instrInd = instIndices.get(i);
            instSubsetNames.add(EOSSDatabase.getInstrument(instrInd).getName());
            instSubsetIndices.add(instrInd);
        }
        //getallorbit scores
        ArrayList<Map.Entry<String, Double>> list2 = new ArrayList<>();
        try{
            list2.addAll(ArchitectureEvaluatorParams.scores.get(instSubsetNames).entrySet());
        }catch(NullPointerException ex){
            //There isn't all permutaions of a subset in the scores map so may need to reorder the arraylist of instrument names
            ArrayList<String> reorder = new ArrayList<>(Arrays.asList(new String[]{instSubsetNames.get(1),instSubsetNames.get(0)}));
            list2.addAll(ArchitectureEvaluatorParams.scores.get(reorder).entrySet());
            instSubsetNames = reorder;
        }

        //sort orbits and get best_orbit
        Collections.sort(list2, Collections.reverseOrder(ByValueComparator));
        String best_orbit = list2.get(0).getKey();
        int best_orbit_index = EOSSDatabase.findOrbitIndex(EOSSDatabase.getOrbit(best_orbit));
        Double new_score = list2.get(0).getValue();
        Double old_score = ArchitectureEvaluatorParams.scores.get(instSubsetNames).get(randOrbit.getName());

        if (new_score > old_score) {
            for (int index : instSubsetIndices) {
                child.removeInstrumentFromOrbit(index, randOrbitIndex);
                child.addInstrumentToOrbit(index, best_orbit_index);
            }
        }

        return child;

    }

    private final Comparator<Map.Entry<String, Double>> ByValueComparator = new Comparator<Map.Entry<String, Double>>() {
        @Override
        public int compare(Map.Entry<String, Double> a1, Map.Entry<String, Double> a2) {
            return a1.getValue().compareTo(a2.getValue());
        }
    };

}
