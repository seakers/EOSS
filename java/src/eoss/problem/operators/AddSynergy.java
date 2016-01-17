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
import java.util.TreeMap;
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

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    protected EOSSArchitecture evolve(EOSSArchitecture child) {
        //Find a random non-empty orbit and its payload 
        int randOrbitIndex = getRandomOrbitWithAtLeastNInstruments(child, 1);
        if(randOrbitIndex == -1)
            return child;
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
     * @param order the order is how many instruments should be considered at once.
     * @return
     */
    private int checkNthOrderSynergy(ArrayList<String> thepayload, Orbit orbit, int order) {
        NDSM dsm = (NDSM) Params.all_dsms.get("SDSM" + order + "@" + orbit.getName());

        TreeMap<Nto1pair, Double> tm = dsm.getAllInteractions("+");

        //Find a missing synergy from intreaction tree
        Iterator<Nto1pair> it = tm.keySet().iterator();
        int i;
        for (i = 0; i < tm.size(); i++) {
            //get next strongest interaction
            Nto1pair nt = it.next();

            //if architecture already contains that interaction, OR if does not contain N-1 elements from the interaction continue
            ArrayList<String> al = new ArrayList<>(Arrays.asList(nt.getBase()));
            al.add(nt.getAdded());
            int missingIndex = containsAllButOne(thepayload, al);
            if (!thepayload.containsAll(al) && missingIndex != -1) { //otherwise find missing element and return;
                return findInstrument(al.get(missingIndex));
            }
        }

        return -1;
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
