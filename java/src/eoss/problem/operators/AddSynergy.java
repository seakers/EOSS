/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.operators;

import eoss.problem.EOSSArchitecture;
import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Orbit;
import eoss.problem.Params;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import org.moeaframework.core.ParallelPRNG;
import rbsa.eoss.Interaction;
import rbsa.eoss.NDSM;
import rbsa.eoss.Nto1pair;

/**
 * This domain specific heuristic see if there is a missed opportunity for
 * synergistic performance (e.g. using two sounders operating in different
 * spectra to increase spatial resolution) by scanning the instruments already
 * assigned and examining a precomputed lookup table to see which added
 * instrument will increase performance
 *
 * @author nozomihitomi
 */
public class AddSynergy extends AbstractEOSSOperator {

    /**
     * The subset size relates to how many of the top costly superfluous
     * interactions the heuristics should select randomly from
     */
    private final int subsetSize;

    private final ParallelPRNG pprng;

    public AddSynergy(int subsetSize) {
        this.subsetSize = subsetSize;
        this.pprng = new ParallelPRNG();
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    protected EOSSArchitecture evolve(EOSSArchitecture child) {
        //Find a random non-empty orbit and its payload 
        int randOrbitIndex = getRandomOrbitWithAtLeastNInstruments(child, 1);
        if (randOrbitIndex == -1) {
            return child;
        }
        Orbit randOrbit = EOSSDatabase.getOrbits().get(randOrbitIndex);

        ArrayList<String> thepayload = new ArrayList<>();
        for (Instrument inst : child.getInstrumentsInOrbit(randOrbit)) {
            thepayload.add(inst.getName());
        }

        //try with 2-lateral synergies
        int missingInstrumentIndex = checkNthOrderSynergy(thepayload, randOrbit, 2);

        //try with 3-lateral synergies
        if (missingInstrumentIndex == -1) {
            missingInstrumentIndex = checkNthOrderSynergy(thepayload, randOrbit, 3);
        }

        if (missingInstrumentIndex != -1) {
            child.addInstrumentToOrbit(missingInstrumentIndex, randOrbitIndex);
        }
        return child;
    }

    /**
     * Checks for opportunities of missed synergies.
     *
     * @param thepayload the payload already assigned to the orbit
     * @param orbit the orbit to examine for missed synergies
     * @param order the order is how many instruments should be considered at
     * once.
     * @return
     */
    private int checkNthOrderSynergy(ArrayList<String> thepayload, Orbit orbit, int order) {
        NDSM dsm = (NDSM) Params.all_dsms.get("SDSM" + order + "@" + orbit.getName());

        TreeSet<Interaction> stm = dsm.getAllInteractions("+");

        //find all superfluous interactions that apply to this spacecraft
        ArrayList<String> missingSynergisticInstrument = new ArrayList();
        Iterator<Interaction> iter = stm.descendingIterator();
        while (iter.hasNext()) {
            Interaction key = iter.next();
            ArrayList<String> al = new ArrayList<>();
            al.addAll(Arrays.asList(key.getNtpair().getBase()));
            al.add(key.getNtpair().getAdded());

            int missingIndex = containsAllButOne(thepayload, al);
            if (!thepayload.containsAll(al) && missingIndex != -1) { //otherwise find missing element and return;
                missingSynergisticInstrument.add(al.get(missingIndex));
            }
        }
        if (missingSynergisticInstrument.isEmpty()) {
            return -1;
        } else {
            List<String> subset;
            if(missingSynergisticInstrument.size()<subsetSize){
                subset = missingSynergisticInstrument;
            }else{
                subset = missingSynergisticInstrument.subList(0, subsetSize); //take instruments that would add the largest amount of synergistic interaction
            }
            String chosenInstrument = subset.get(pprng.nextInt(subset.size()));
            return findInstrument(chosenInstrument);
        }
    }

    /**
     * Checks to see if the first array contains all but one element of the
     * second array.
     *
     * @param return the index of the instrument missing from the first array as
     * it is in the EOSSDatabase. If there is more than one, then return -1.
     */
    private int containsAllButOne(ArrayList<String> first, ArrayList<String> second) {
        int doesntContain = 0;
        int missing = -1;
        for (String second1 : second) {
            if (doesntContain > 1) {
                return -1;
            }
            if (!first.contains(second1)) {
                doesntContain++;
            }
            missing++;
        }
        return missing;
    }
}
