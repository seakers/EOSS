/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.assignment.operators;

import eoss.problem.assignment.InstrumentAssignmentArchitecture;
import eoss.problem.EOSSDatabase;
import java.util.ArrayList;
import java.util.Collections;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;

/**
 *
 * @author nozomihitomi
 */
public abstract class AbstractEOSSOperator implements Variation {

    @Override
    public Solution[] evolve(Solution[] sltns) {
        if (!(sltns[0] instanceof InstrumentAssignmentArchitecture)) {
            throw new IllegalArgumentException("Expected EOSSArchitecture instance. Found " + sltns[0].getClass());
        }
        InstrumentAssignmentArchitecture child = (InstrumentAssignmentArchitecture) sltns[0].copy();
        return new Solution[]{evolve(child)};
    }

    protected abstract InstrumentAssignmentArchitecture evolve(InstrumentAssignmentArchitecture child);

    /**
     * Finds a random orbit with at least n instruments (inclusive)
     *
     * @param arch the EOSS architecture to examine
     * @param nInst the minimum number of instruments that a satellite should
     * have
     * @return the index of the random orbit as it is in the EOSSDatabase
     */
    public int getRandomOrbitWithAtLeastNInstruments(InstrumentAssignmentArchitecture arch, int nInst) {
        //Find a random orbit and its payload 
        ArrayList<Integer> orbitIndex = new ArrayList<>(EOSSDatabase.getNumberOfOrbits());
        for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); i++) {
            orbitIndex.add(i);
        }
        Collections.shuffle(orbitIndex);//this sorts orbits in random order

        for (Integer ind : orbitIndex) {
            if (arch.getInstrumentsInOrbit(EOSSDatabase.getOrbit(ind)).size() >= nInst) { // is there at most MAXSIZE instruments in this orbit?
                return ind;
            }
        }
        //if not returned yet, then there is no orbit with at least n instruments
        return -1;
    }

    /**
     * Finds a random orbit with at most n instruments (inclusive)
     *
     * @param arch the EOSS architecture to examine
     * @param nInst the maximum number of instruments that a satellite should
     * have
     * @return the index of the random orbit as it is in the EOSSDatabase
     */
    public int getRandomOrbitWithAtMostNInstruments(InstrumentAssignmentArchitecture arch, int nInst) {
        //Find a random orbit and its payload 
        ArrayList<Integer> orbitIndex = new ArrayList<>(EOSSDatabase.getNumberOfOrbits());
        for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); i++) {
            orbitIndex.add(i);
        }
        Collections.shuffle(orbitIndex);//this sorts orbits in random order

        for (Integer ind : orbitIndex) {
            if (arch.getInstrumentsInOrbit(EOSSDatabase.getOrbit(ind)).size() <= nInst) { // is there at most MAXSIZE instruments in this orbit?
                return ind;
            }
        }
        //if not returned yet, then there is no orbit with at least n instruments
        return -1;
    }
}
