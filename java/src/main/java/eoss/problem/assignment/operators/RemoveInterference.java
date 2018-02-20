/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.assignment.operators;

import eoss.problem.assignment.InstrumentAssignmentArchitecture;
import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Orbit;
import eoss.problem.evaluation.ArchitectureEvaluatorParams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import org.moeaframework.core.PRNG;
import rbsa.eoss.Interaction;
import rbsa.eoss.NDSM;

/**
 * This domain specific heuristic checks the architecture to see if there are
 * any interfering instruments aboard the same spacecraft. If there is such an
 * interaction, an instrument causing the interference is taken from that orbit
 * and reassigned to another random orbit
 *
 * @author nozomihitomi
 */
public class RemoveInterference extends AbstractEOSSOperator {

    /**
     * The subset size relates to how many of the top costly superfluous
     * interactions the heuristics should select randomly from
     */
    private final int subsetSize;

    public RemoveInterference(int subsetSize) {
        this.subsetSize = subsetSize;
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    protected InstrumentAssignmentArchitecture evolve(InstrumentAssignmentArchitecture child) {
        //Find a random non-empty orbit and its payload 
        int randOrbitIndex = getRandomOrbitWithAtLeastNInstruments(child, 1);
        if(randOrbitIndex == -1)
            return child;
        Orbit randOrbit = EOSSDatabase.getOrbit(randOrbitIndex);

        ArrayList<String> thepayload = new ArrayList<>();
        for (Instrument inst : child.getInstrumentsInOrbit(randOrbit)) {
            thepayload.add(inst.getName());
        }

        //try with 2-lateral synergies
        int interferingInstrumentIndex = checkNthOrderInterference(thepayload, randOrbit, 2);

        //try with 3-lateral synergies
//        if (interferingInstrumentIndex == -1) {
//            interferingInstrumentIndex = checkNthOrderInterference(thepayload, randOrbit, 3);
//        }

        if (interferingInstrumentIndex != -1) {
            child.removeInstrumentFromOrbit(interferingInstrumentIndex, randOrbitIndex);
            int orbitToAdd = randOrbitIndex;
            while (orbitToAdd == randOrbitIndex) {
                orbitToAdd = PRNG.nextInt(EOSSDatabase.getNumberOfOrbits());
            }
            child.addInstrumentToOrbit(interferingInstrumentIndex, orbitToAdd);
        }
        return child;
    }

    /**
     * Checks for opportunities of reducing interferences between instruments .
     *
     * @param thepayload the payload already assigned to the orbit
     * @param orbit the orbit to examine for opportunity
     * @param order the order is how many instruments should be considered at
     * once.
     * @return the instrument index as found in the EOSSDatabase if there is an
     * interference interaction. Else -1;
     */
    private int checkNthOrderInterference(ArrayList<String> thepayload, Orbit orbit, int order) {

        NDSM dsm = (NDSM) ArchitectureEvaluatorParams.all_dsms.get("EDSM" + order + "@" + orbit.getName());

        TreeSet<Interaction> stm = dsm.getAllInteractions("+");

         //find all superfluous interactions that apply to this spacecraft
        ArrayList<String> interferingInstrument = new ArrayList();
        Iterator<Interaction> iter = stm.descendingIterator();
        while (iter.hasNext()) {
            Interaction key = iter.next();
            ArrayList<String> al = new ArrayList<>();
            al.addAll(Arrays.asList(key.getNtpair().getBase()));
            al.add(key.getNtpair().getAdded());

            if (thepayload.containsAll(al)) { //otherwise find missing element and return;
                interferingInstrument.add(key.getNtpair().getAdded());
            }
        }
        if (interferingInstrument.isEmpty()) {
            return -1;
        } else {
            List<String> subset;
            if(interferingInstrument.size()<subsetSize){
                subset = interferingInstrument;
            }else{
                subset = interferingInstrument.subList(0, subsetSize); //take instruments that would add the largest amount of cost
            }
            String chosenInstrument = subset.get(PRNG.nextInt(subset.size()));
            return EOSSDatabase.findInstrumentIndex(EOSSDatabase.getInstrument(chosenInstrument));
        }
    }

}
