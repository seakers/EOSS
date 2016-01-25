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
import org.moeaframework.core.ParallelPRNG;
import rbsa.eoss.NDSM;
import rbsa.eoss.Nto1pair;

/**
 * This domain specific heuristic checks the architecture to see if there are
 * any interfering instruments aboard the same spacecraft. If there is such an
 * interaction, an instrument causing the interference is taken from that orbit
 * and reassigned to another random orbit
 *
 * @author nozomihitomi
 */
public class RemoveInterference extends AbstractEOSSOperator {

    private final ParallelPRNG pprng;

    public RemoveInterference() {
        super();
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
        if(randOrbitIndex == -1)
            return child;
        Orbit randOrbit = EOSSDatabase.getOrbits().get(randOrbitIndex);

        ArrayList<String> thepayload = new ArrayList<>();
        for (Instrument inst : child.getInstrumentsInOrbit(randOrbit)) {
            thepayload.add(inst.getName());
        }

        //try with 2-lateral synergies
        int interferingInstrumentIndex = checkNthOrderInterference(thepayload, randOrbit, 2);

        //try with 3-lateral synergies
        if (interferingInstrumentIndex == -1) {
            interferingInstrumentIndex = checkNthOrderInterference(thepayload, randOrbit, 3);
        }

        if (interferingInstrumentIndex != -1) {
            child.removeInstrumentFromOrbit(interferingInstrumentIndex, randOrbitIndex);
            int orbitToAdd = randOrbitIndex;
            while (orbitToAdd != randOrbitIndex) {
                orbitToAdd = pprng.nextInt(EOSSDatabase.getOrbits().size());
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

        NDSM dsm = (NDSM) Params.all_dsms.get("SDSM" + order + "@" + orbit.getName());

        TreeMap<Nto1pair, Double> tm = dsm.getAllInteractions("-","-");

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
