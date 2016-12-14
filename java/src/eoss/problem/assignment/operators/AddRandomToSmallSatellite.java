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

/**
 * This domain specific heuristic adds a random instrument to a small satellite
 * under the assumption that only having one instrument aboard a spacecraft is
 * not an efficient use of the bus.
 *
 * @author nozomihitomi
 */
public class AddRandomToSmallSatellite extends AbstractEOSSOperator {

    /**
     * the largest number of instruments that defines a small
     */
    private final double maxSize;

    /**
     *
     * @param maxSize the largest instrument mass (in kg) that defines a small
     * satellite
     */
    public AddRandomToSmallSatellite(double maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    protected InstrumentAssignmentArchitecture evolve(InstrumentAssignmentArchitecture child) {
        //Find random orbit with less than maxSize mass of instruments
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
            if(payloadMass >= maxSize){
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
            if (child.addInstrumentToOrbit(ind, randOrbIndex)) {
                //checks to see if the added instrument changes the architecture. if yes, then break
                break;
            }
        }
        return child;
    }

}
