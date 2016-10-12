/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import architecture.pattern.Combining;
import architecture.pattern.Assigning;
import architecture.Architecture;
import architecture.pattern.ArchitecturalDecision;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import org.apache.commons.lang3.StringUtils;
import org.moeaframework.core.Solution;

/**
 * This class creates a solution for the problem consisting of an assigning
 * pattern of instruments to orbits and a combining pattern for the number of
 * satellites per orbit. Assigning instruments from the left hand side to orbits
 * on the right hand side
 *
 * @author nozomi
 */
//import jess.*;
public class EOSSArchitecture extends Architecture {

    private static final long serialVersionUID = 8776271523867355732L;
    private Assigning assignment;
    private Combining combine;

    //Constructors
    /**
     * Creates an empty architecture with a default number of satellites.
     * Default value is the first value in the array given as
     * alternativesForNumberOfSatellites
     *
     * @param numberOfInstruments
     * @param numberOfOrbits
     * @param altnertivesForNumberOfSatellites
     * @param numberOfObjectives
     */
    public EOSSArchitecture(int[] altnertivesForNumberOfSatellites,
            int numberOfInstruments, int numberOfOrbits, int numberOfObjectives) {
        super(Arrays.asList(new ArchitecturalDecision[]{
            new Combining(altnertivesForNumberOfSatellites, altnertivesForNumberOfSatellites[0]),
            new Assigning(numberOfInstruments, numberOfOrbits)}), numberOfObjectives);

        this.combine = (Combining) this.getVariable(0);
        this.assignment = (Assigning) this.getVariable(1);
    }

    /**
     * Creates an empty architecture with a default number of satellites.
     * Default value is the first value in the array given as
     * alternativesForNumberOfSatellites
     *
     * @param nSatellitesChoice the decisions for the combining pattern deciding
     * the number of satellites per orbit
     * @param inst2OrbAssingment the decisions for the assigning pattern
     * deciding the allocation of instruments to orbit
     * @param numberOfObjectives
     */
    public EOSSArchitecture(Combining nSatellitesChoice, Assigning inst2OrbAssingment, int numberOfObjectives) {
        super(Arrays.asList(new ArchitecturalDecision[]{nSatellitesChoice, inst2OrbAssingment}), numberOfObjectives);
    }

    /**
     * makes a copy soltion from the input solution 
     * @param solution 
     */
    public EOSSArchitecture(Solution solution) {
        super(solution);
        if (solution instanceof EOSSArchitecture) {
            this.combine = (Combining)this.getVariable(0);
            this.assignment = (Assigning)this.getVariable(1);
        } else {
            throw new IllegalArgumentException("Expected type EOSSArchitecture class. Found " + solution.getClass().getSimpleName());
        }
    }

    //Getters
    /**
     * Returns the total number of satellites in the architecture (number of
     * satellites x number of orbits with at least one instrument assigned).
     *
     * @return
     */
    public int getTotalNumberOfSatellites() {
        return combine.getValue() * getNorbits();
    }

    /**
     * Returns the number of satellites per orbit.
     *
     * @return
     */
    public int getNumberOfSatellitesPerOrbit() {
        return combine.getValue();
    }

    /**
     * Gets the names of the payloads that are assigned within the architecture
     * at least once
     *
     * @return
     */
    public Instrument[] getPayloads() {
        BitSet inst = getUniqueInstruments();
        Instrument[] out = new Instrument[inst.cardinality()];
        int strInd = 0;
        //loop over the indices that have been set true
        for (int i = inst.nextSetBit(0); i >= 0; i = inst.nextSetBit(i + 1)) {
            out[strInd] = EOSSDatabase.getInstruments().get(i);
            strInd++;
        }
        return out;
    }

    /**
     * Gets the bitset that represents the instruments that are assigned within
     * the architecture at least once
     *
     * @return
     */
    private BitSet getUniqueInstruments() {
        BitSet bs = assignment.getBitSet();
        BitSet inst = new BitSet(assignment.getNumberOfLHS());
        for (int i = 0; i < assignment.getNumberOfLHS(); i++) {
            BitSet inst_i = bs.get(i * assignment.getNumberOfRHS(), i * assignment.getNumberOfRHS() + assignment.getNumberOfRHS());
            if(inst_i.cardinality() > 0){
                inst.set(i);
            }
        }
        return inst;
    }

    /**
     * Gets the number of unique instruments that are assigned
     *
     * @return
     */
    public int getNinstruments() {
        return getUniqueInstruments().cardinality();
    }

    /**
     * Gets the names of the orbits that have at least one instrument assigned
     * to it
     *
     * @return
     */
    public Orbit[] getOccupiedOrbits() {
        BitSet orbs = getUniqueOrbits();
        Orbit[] out = new Orbit[orbs.cardinality()];
        int strInd = 0;
        //loop over the indices that have been set true
        for (int i = orbs.nextSetBit(0); i >= 0; i = orbs.nextSetBit(i + 1)) {
            out[strInd] = EOSSDatabase.getOrbits().get(i);
            strInd++;
        }
        return out;
    }
    
    /**
     * Gets the names of the orbits that have no instruments assigned
     * to it
     *
     * @return
     */
    public Orbit[] getEmptyOrbits() {
        BitSet orbs = getUniqueOrbits();
        orbs.flip(0, orbs.length());
        Orbit[] out = new Orbit[orbs.cardinality()];
        int strInd = 0;
        //loop over the indices that have been set true
        for (int i = orbs.nextSetBit(0); i >= 0; i = orbs.nextSetBit(i + 1)) {
            out[strInd] = EOSSDatabase.getOrbits().get(i);
            strInd++;
        }
        return out;
    }

    /**
     * Gets the bitset that represents the orbits that have at least one
     * instrument assigned to it
     *
     * @return
     */
    private BitSet getUniqueOrbits() {
        BitSet bs = assignment.getBitSet();
        BitSet orbs = new BitSet(assignment.getNumberOfRHS());
        for (int o = 0; o < assignment.getNumberOfLHS(); o++) {
            orbs.or(bs.get(o * assignment.getNumberOfRHS(), o * assignment.getNumberOfRHS() + assignment.getNumberOfRHS()));
        }
        return orbs;
    }

    /**
     * Gets the number of unique orbits that are assigned
     *
     * @return
     */
    public int getNorbits() {
        return getUniqueOrbits().cardinality();
    }

    /**
     * TODO fix this up to be more efficient. Use information in the BITSET
     * Checks to see if the specified instrument has been assigned in this
     * architecture
     *
     * @param instr
     * @return
     */
    public boolean containsInst(Instrument instr) {
        ArrayList<Instrument> validInstruments = new ArrayList<>();
        validInstruments.addAll(EOSSDatabase.getInstruments());
        if (!validInstruments.contains(instr)) {
            return false;
        }
        for (int o = 0; o < assignment.getNumberOfRHS(); o++) {
            Orbit orb = EOSSDatabase.getOrbits().get(o);
            ArrayList<Instrument> payls = this.getInstrumentsInOrbit(orb);
            if (payls != null) {
                ArrayList<Instrument> paylds = new ArrayList();
                paylds.addAll(payls);
                if (paylds.contains(instr)) {
                    return true;
                }
            }
        }
        return false;
    }

    public BitSet getBitString() {
        return assignment.getBitSet();
    }

    /**
     * Gets the names of the instruments assigned to a specified orbit
     *
     * @param orb
     * @return
     */
    public ArrayList<Instrument> getInstrumentsInOrbit(Orbit orb) {
        ArrayList<Instrument> payloads = new ArrayList<>();
        int orbIndex;
        for (orbIndex = 0; orbIndex < EOSSDatabase.getOrbits().size(); orbIndex++) {
            if (orb.equals(EOSSDatabase.getOrbits().get(orbIndex))) {
                break;
            }
        }

        //loop over the instruments
        for (int i = 0; i < EOSSDatabase.getInstruments().size(); i++) {
            if (assignment.isConnected(i, orbIndex)) {
                payloads.add(EOSSDatabase.getInstruments().get((i)));
            }
        }
        return payloads;
    }

    /**
     * Gets the indices of the instruments assigned to a specified orbit
     *
     * @param orbIndex the index of the orbit as it is in the EOSSDatabase
     * @return the indices of the instruments as they are in the EOSSDatabase
     */
    public ArrayList<Integer> getInstrumentsInOrbit(int orbIndex) {
        ArrayList<Integer> payloads = new ArrayList<>();
        //loop over the instruments
        for (int i = 0; i < EOSSDatabase.getInstruments().size(); i++) {
            if (assignment.isConnected(i, orbIndex)) {
                payloads.add(i);
            }
        }
        return payloads;
    }

    /**
     * adds the instrument to the orbit
     *
     * @param instrumentIndex the index of the instrument as it is in the
     * EOSSDatabase
     * @param orbIndex the index of the orbit as it is in the EOSSDatabase
     * @return true if adding the instrument to orbit changes the architecture
     * decision
     */
    public boolean addInstrumentToOrbit(int instrumentIndex, int orbIndex) {
        boolean out = !assignment.isConnected(instrumentIndex, orbIndex);
        assignment.connect(instrumentIndex, orbIndex);
        return out;
    }

    /**
     * removes the instrument from the orbit
     *
     * @param instrumentIndex the index of the instrument as it is in the
     * EOSSDatabase
     * @param orbIndex the index of the orbit as it is in the EOSSDatabase
     * @return true if removing the instrument to orbit changes the architecture
     * decision
     */
    public boolean removeInstrumentFromOrbit(int instrumentIndex, int orbIndex) {
        boolean out = assignment.isConnected(instrumentIndex, orbIndex);
        assignment.disconnect(instrumentIndex, orbIndex);
        return out;
    }

    public String payloadToString() {
        String str = String.valueOf(this.getNumberOfSatellitesPerOrbit()) + " x ";
        for (Orbit orb : EOSSDatabase.getOrbits()) {
            ArrayList<Instrument> payls = this.getInstrumentsInOrbit(orb);
            str = str + " " + orb + "{" + StringUtils.join(payls, ",") + "}";
        }
        return str;
    }

    //Utils
    public Boolean isEmptyOrbit(Orbit orb) {
        return (getInstrumentsInOrbit(orb).isEmpty());
    }

    /**
     * TODO get rid of this and make more generic Checks problem specific
     * constraints
     *
     * @return
     */
    public boolean checkConstraints() {
        if (this == null) {
            return false;
        }

//        //Constraint 1: No PATH_GEOSTAR in LEO orbit
//        for (Orbit orb : EOSSDatabase.orbits) {
//            ArrayList<Instrument> payload_in_orbit = this.getInstrumentsInOrbit(orb);
//            if (orb.equals("GEO-35788-equat-NA")) {
//                continue;
//            }
//            for (Instrument instr : payload_in_orbit) {
//                if (instr.equals("PATH_GEOSTAR")) {
//                    return false;
//                }
//            }
//        }
//
//        //Constraint 2: No LEO instruments in GEO Orbit
//        String[] GEO_payload = this.getInstrumentsInOrbit("GEO-35788-equat-NA");
//        for (String GEO_payload1 : GEO_payload) {
//            if (!GEO_payload1.equalsIgnoreCase("PATH_GEOSTAR")) {
//                return false;
//            }
//        }
        //If has not returned false yet, return feasible=true
        return true;
    }

    @Override
    public Solution copy() {
        return new EOSSArchitecture(this);
    }

}
