/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.operators;

import eoss.problem.EOSSArchitecture;
import eoss.problem.EOSSDatabase;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This domain specific heuristic removes a random instrument from a loaded
 * satellite in the architecture under the assumption that too many instruments
 * aboard one spacecraft is not good.
 *
 * @author nozomihitomi
 */
public class RemoveRandomFromLoadedSatellite extends AbstractEOSSOperator {

    /**
     * the largest mass of instruments that defines a small
     */
    private final double minSize;

    /**
     *
     * @param minSize the smallest mass of instruments that defines a loaded
     * satellite
     */
    public RemoveRandomFromLoadedSatellite(double minSize) {
        this.minSize = minSize;
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    protected EOSSArchitecture evolve(EOSSArchitecture child) {
        //Find random orbit with more than maxSize mass of instruments
        ArrayList<Integer> orbitIndex = new ArrayList<>(EOSSDatabase.getOrbits().size());
        for (int i = 0; i < EOSSDatabase.getOrbits().size(); i++) {
            orbitIndex.add(i);
        }
        Collections.shuffle(orbitIndex);//this sorts orbits in random order
        
        int randOrbIndex = -1;
        for(Integer j : orbitIndex){
            ArrayList<Integer> payload = child.getInstrumentsInOrbit(j);
            double payloadMass = 0;
            for(Integer k : payload){
                payloadMass += Double.parseDouble(EOSSDatabase.getInstruments().get(k).getProperty("mass#"));
            }
            if(payloadMass <= minSize){
                randOrbIndex = j;
                break;
            }
        }
        if(randOrbIndex == -1) //if this condition occurs then no orbit has a small satellite
            return child;

        //Find a random instrument that has not yet been assigned to the random orbit found above 
        ArrayList<Integer> instIndex = new ArrayList<>(EOSSDatabase.getInstruments().size());
        for (int i = 0; i < EOSSDatabase.getInstruments().size(); i++) {
            instIndex.add(i);
        }
        Collections.shuffle(instIndex);//this sorts orbits in random order
        for (Integer ind : instIndex) {
            if (child.removeInstrumentFromOrbit(ind, randOrbIndex)) {
                //checks to see if the added instrument changes the architecture. if yes, then break
                break;
            }
        }
        return child;
    }
}
