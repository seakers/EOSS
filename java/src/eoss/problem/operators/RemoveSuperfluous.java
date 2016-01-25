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
 *
 * @author nozomihitomi
 */
public class RemoveSuperfluous extends AbstractEOSSOperator {

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
        int extraInstrumentIndex = checkNthOrderSuperfluous(thepayload, randOrbit, 2);

        //try with 3-lateral synergies
        if (extraInstrumentIndex == -1) {
            extraInstrumentIndex = checkNthOrderSuperfluous(thepayload, randOrbit, 3);
        }

        if (extraInstrumentIndex != -1) {
            child.removeInstrumentFromOrbit(extraInstrumentIndex, randOrbitIndex);
        }
        return child;
    }

    /**
     * Checks for opportunities of removing a superfluous instrument that provides redundant capabilities.
     *
     * @param thepayload the payload already assigned to the orbit
     * @param orbit the orbit to examine for opportunity
     * @param order the order is how many instruments should be considered at
     * once.
     * @return the instrument index as found in the EOSSDatabase if there is an
     * superfluous interaction. Else -1;
     */
    private int checkNthOrderSuperfluous(ArrayList<String> thepayload, Orbit orbit, int order) {
        NDSM dsm = (NDSM) Params.all_dsms.get("RDSM" + order + "@" + orbit.getName());

        TreeMap<Nto1pair, Double> tm = dsm.getAllInteractions("0","+");

        //Find a missing interference from intreaction tree
        Iterator<Nto1pair> it = tm.keySet().iterator();
        int i;
        for (i = 0; i < tm.size(); i++) {
            //get next strongest interaction
            Nto1pair nt = it.next();

            //if architecture contains the interaction
            ArrayList<String> al = new ArrayList<>(Arrays.asList(nt.getBase()));
            al.add(nt.getAdded());
            if (thepayload.containsAll(al)) {
                return findInstrument(nt.getAdded());
            }
        }

        return -1;
    }

}
