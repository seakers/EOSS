/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.operators;

import eoss.problem.EOSSArchitecture;
import eoss.problem.EOSSDatabase;
import eoss.problem.Orbit;
import eoss.problem.Params;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;
import rbsa.eoss.NDSM;
import rbsa.eoss.Nto1pair;

/**
 *
 * @author nozomihitomi
 */
public abstract class AbstractEOSSOperator implements Variation {

    @Override
    public Solution[] evolve(Solution[] sltns) {
        if (!(sltns[0] instanceof EOSSArchitecture)) {
            throw new IllegalArgumentException("Expected EOSSArchitecture instance. Found " + sltns[0].getClass());
        }
        EOSSArchitecture child = (EOSSArchitecture) sltns[0].copy();
        return new Solution[]{evolve(child)};
    }

    protected abstract EOSSArchitecture evolve(EOSSArchitecture child);

    /**
     * Finds a random orbit with at least n instruments (inclusive)
     *
     * @param arch the EOSS architecture to examine
     * @param nInst the minimum number of instruments that a satellite should
     * have
     * @return the index of the random orbit as it is in the EOSSDatabase
     */
    public int getRandomOrbitWithAtLeastNInstruments(EOSSArchitecture arch, int nInst) {
        //Find a random orbit and its payload 
        ArrayList<Integer> orbitIndex = new ArrayList<>(EOSSDatabase.getOrbits().size());
        for (int i = 0; i < EOSSDatabase.getOrbits().size(); i++) {
            orbitIndex.add(i);
        }
        Collections.shuffle(orbitIndex);//this sorts orbits in random order

        for (Integer ind : orbitIndex) {
            if (arch.getInstrumentsInOrbit(EOSSDatabase.getOrbits().get(ind)).size() >= nInst) { // is there at most MAXSIZE instruments in this orbit?
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
    public int getRandomOrbitWithAtMostNInstruments(EOSSArchitecture arch, int nInst) {
        //Find a random orbit and its payload 
        ArrayList<Integer> orbitIndex = new ArrayList<>(EOSSDatabase.getOrbits().size());
        for (int i = 0; i < EOSSDatabase.getOrbits().size(); i++) {
            orbitIndex.add(i);
        }
        Collections.shuffle(orbitIndex);//this sorts orbits in random order

        for (Integer ind : orbitIndex) {
            if (arch.getInstrumentsInOrbit(EOSSDatabase.getOrbits().get(ind)).size() <= nInst) { // is there at most MAXSIZE instruments in this orbit?
                return ind;
            }
        }
        //if not returned yet, then there is no orbit with at least n instruments
        return -1;
    }

    /**
     * Finds the index of the given instrument as it is found in the EOSSDatabase. If not found, then return -1.
     * @param instName the name of the instrument
     * @return the index of the given instrument as it is found in the EOSSDatabase. If the instrument is not in the database, then return -1.
     */
    protected int findInstrument(String instName) {
        for (int j = 0; j < EOSSDatabase.getInstruments().size(); j++) {
            if (instName.equalsIgnoreCase(EOSSDatabase.getInstruments().get(j).getName())) {
                return j;
            }
        }
        return -1;
    }
    /**
     * 
     * Finds the index of the given orbit as it is found in the EOSSDatabase. If not found, then return -1.
     * @param orbName the name of the orbit
     * @return the index of the given orbit as it is found in the EOSSDatabase. If the orbit is not in the database, then return -1.
     */
    protected int findOrbit(String orbName) {
        for (int j = 0; j < EOSSDatabase.getOrbits().size(); j++) {
            if (orbName.equalsIgnoreCase(EOSSDatabase.getOrbits().get(j).getName())) {
                return j;
            }
        }
        return -1;
    }
}
