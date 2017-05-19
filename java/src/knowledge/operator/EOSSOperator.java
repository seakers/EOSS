/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import eoss.problem.assignment.InstrumentAssignmentArchitecture;
import eoss.problem.EOSSDatabase;
import eoss.problem.Orbit;
import eoss.problem.assignment.operators.AbstractEOSSOperator;
import java.util.ArrayList;
import java.util.Arrays;
import org.moeaframework.core.ParallelPRNG;

/**
 *
 * @author nozomihitomi
 */
public class EOSSOperator extends AbstractEOSSOperator {

    /**
     * Mode = {0,1,2}. 0 for orbit2instrument assignment, 1 for counting number
     * of orbits, 2 for counting number of instruments
     */
    private final int mode;

    /**
     * In mode 0 this is used as a negation if 0. For modes 1 and 2, its used to
     * specify how many orbits or instruments
     */
    private final int arg;

    /**
     * the orbit id as specified in binary matrix, or -1 for wildcard
     */
    private final int orbit;

    /**
     * the instrument id as specified in binary matrix or -1 for wildcard
     */
    private final int[] instrument;

    private final ParallelPRNG pprng;

    /**
     * array of all orbit indices
     */
    private final int[] allOrbits;

    /**
     *
     * @param strMode Mode = {0,1,2}. 0 for orbit2instrument assignment, 1 for
     * counting number of orbits, 2 for counting number of instruments
     * @param strArg In mode 0 this is used as a negation if 0. For modes 1 and
     * 2, its used to specify how many orbits or instruments
     * @param strOrbit the orbit id as specified in binary matrix, or * for
     * wildcard
     * @param strInstrument the instrument id as specified in binary matrix or *
     * for wildcard
     */
    public EOSSOperator(String strMode, String strArg, String strOrbit, String[] strInstrument) {
        this.mode = Integer.parseInt(strMode);
        this.arg = Integer.parseInt(strArg);
        if (strOrbit.matches("\\*")) {
            this.orbit = -1;
        } else if (strOrbit.matches("A")) {
            this.orbit = -2;
        } else {
            this.orbit = Integer.parseInt(strOrbit);
        }

        allOrbits = new int[EOSSDatabase.getNumberOfOrbits()];
        for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); i++) {
            allOrbits[i] = i;
        }

        if (strInstrument[0].matches("A")) {
            this.instrument = new int[EOSSDatabase.getNumberOfInstruments()];
            if (strInstrument.length > 1) {
                throw new IllegalArgumentException("All instruments A cannot be used in addition to specifying other instruments");
            } else {
                for (int i = 0; i < EOSSDatabase.getNumberOfInstruments(); i++) {
                    instrument[i] = i;
                }
            }
        } else {
            this.instrument = new int[strInstrument.length];
            for (int i = 0;
                    i < strInstrument.length;
                    i++) {
                if (strInstrument[i].matches("\\*")) {
                    if (strInstrument.length > 1) {
                        throw new IllegalArgumentException("Wildcard * for instruments cannot be used in addition to specifying other instruments");
                    } else {
                        this.instrument[i] = -1;
                    }
                } else {
                    this.instrument[i] = Integer.parseInt(strInstrument[i]);
                }
            }
        }

        checkParameters();

        this.pprng = new ParallelPRNG();
    }

    /**
     * This method checks that the input parameters are valid with respect to
     * the database and the actions available.
     *
     * @return
     * @throws IllegalArgumentException
     */
    private boolean checkParameters() throws IllegalArgumentException {
        int nInst = EOSSDatabase.getNumberOfInstruments();
        int nOrb = EOSSDatabase.getNumberOfOrbits();

        //check mode is = {0, 1, 2}
        switch (this.mode) {
            case 0:
                if (arg != 0 && arg != 1) {
                    throw new IllegalArgumentException(String.format("For mode = %d expected arg = {0, 1}. Found %d", this.mode, this.arg));
                }
                break;
            case 1:
                if (arg > nOrb) {
                    throw new IllegalArgumentException(String.format("For mode = %d expected arg < %d (number of available orbits). Found %d", this.mode, nOrb, this.arg));
                }
                break;

            case 2:
                if (arg > nInst * nOrb) {
                    throw new IllegalArgumentException(String.format("For mode = %d expected arg < %d (number of available instruments * available orbits). Found %d", this.mode, nInst * nOrb, this.arg));
                }else if(this.instrument[0] > -1 && arg > nOrb){
                    throw new IllegalArgumentException(String.format("For mode = %d and specified instrument %d, expected arg < %d (number of available orbits). Found %d", this.mode, this.instrument[0], nOrb, this.arg));
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("Expected mode = {0, 1, 2}. Found %d", this.mode));
        }

        //check that the instrument id is not greater than the number of available instruments in the database
        if (this.orbit > nOrb) {
            throw new IllegalArgumentException(String.format(
                    "For mode = %d expected orbit id to be less than number of "
                    + "orbits (%d) available in the database."
                    + " Found %d", this.mode, nOrb, this.orbit));
        }

        //check that the instrument id is not greater than the number of available instruments in the database
        for (int inst : instrument) {
            if (inst > nInst) {
                throw new IllegalArgumentException(String.format(
                        "For mode = %d expected instrument id to be"
                        + " less than number of instruments"
                        + " (%d) available in the database."
                        + " Found %d", this.mode, nInst, inst));
            }
        }

        return true;
    }

    /**
     * This operator only takes in one solution and modifies it
     *
     * @return
     */
    @Override
    public int getArity() {
        return 1;
    }

    @Override
    protected InstrumentAssignmentArchitecture evolve(InstrumentAssignmentArchitecture child) {
        InstrumentAssignmentArchitecture arch = (InstrumentAssignmentArchitecture) child.copy();

        int[] orbitArray;
        if (orbit == -2) {
            orbitArray = allOrbits;
        } else {
            if (orbit == -1) {
                orbitArray = new int[]{pprng.nextInt(EOSSDatabase.getNumberOfOrbits())};
            } else {
                orbitArray = new int[]{orbit};
            }
        }

        for (int orbitInd : orbitArray) {

            switch (mode) {
                //mode when we want specific orbit to instrument patterns
                case 0:
                    int[] instID = new int[instrument.length];
                    //case when instrument id is a wildcard
                    if (instrument[0] == -1) {
                        //select a random instrument for the orbit
                        instID[0] = pprng.nextInt(EOSSDatabase.getNumberOfInstruments());
                    } else {
                        instID = instrument;
                    }

                    //Case when we want to have instruments absent or separated
                    if (arg == 0) {
                        int removedInstCount = 0;
                        int maxRemovalAllowed;

                        if (orbit == -2 && instID.length > 1) {
                            //this would be case like separate2 or separate3
                            maxRemovalAllowed = instID.length - 1;
                        } else {
                            maxRemovalAllowed = EOSSDatabase.getNumberOfInstruments();
                        }

                        for (int index : instID) {
                            arch.removeInstrumentFromOrbit(index, orbitInd);
                            removedInstCount++;
                            if (removedInstCount >= maxRemovalAllowed) {
                                break;
                            }
                        }
                    } else {
                        //case when we want to have instruments present
                        //add all instruments from instID array
                        for (int index : instID) {
                            arch.addInstrumentToOrbit(index, orbitInd);
                        }
                    }
                    break;

                //mode when we want to match the number of orbits occupied
                case 1:
                    //case when there are more orbits occupied than the target value
                    if (arch.getNorbits() > arg) {
                        while (arch.getNorbits() > arg) {
                            Orbit[] occupiedOrbits = arch.getOccupiedOrbits();
                            Orbit orbitToRemove = occupiedOrbits[pprng.nextInt(occupiedOrbits.length)];
                            int orbitIndexToRemove = EOSSDatabase.findOrbitIndex(orbitToRemove);
                            ArrayList<Integer> instrumentsToRemove = arch.getInstrumentsInOrbit(orbitIndexToRemove);
                            for (Integer inst : instrumentsToRemove) {
                                arch.removeInstrumentFromOrbit(inst, orbitIndexToRemove);
                            }
                        }
                    } else if (arch.getNorbits() < arg) {
                        while (arch.getNorbits() < arg) {
                            Orbit[] emptyOrbits = arch.getEmptyOrbits();
                            Orbit orbitToAdd = emptyOrbits[pprng.nextInt(emptyOrbits.length)];
                            int orbitIndexToAdd = EOSSDatabase.findOrbitIndex(orbitToAdd);
                            int randInstToAdd = pprng.nextInt(EOSSDatabase.getNumberOfInstruments());
                            arch.addInstrumentToOrbit(randInstToAdd, orbitIndexToAdd);
                        }
                    }
                    break;
                //mode when we want to match the number of instruments used in a particular orbit
                case 2:
                    if (arch.getInstrumentsInOrbit(orbitInd).size() > arg) {
                        while (arch.getInstrumentsInOrbit(orbitInd).size() > arg) {
                            ArrayList<Integer> insts = arch.getInstrumentsInOrbit(orbitInd);
                            arch.removeInstrumentFromOrbit(insts.get(pprng.nextInt(insts.size())), orbitInd);
                        }
                    } else if (arch.getInstrumentsInOrbit(orbitInd).size() < arg) {
                        while (arch.getInstrumentsInOrbit(orbitInd).size() < arg) {
                            ArrayList<Integer> insts = arch.getInstrumentsInOrbit(orbitInd);
                            ArrayList<Integer> unassignedInst = new ArrayList<>(insts.size());
                            for (int i = 0; i < EOSSDatabase.getNumberOfInstruments(); i++) {
                                if (!insts.contains(i)) {
                                    unassignedInst.add(i);
                                }
                            }
                            arch.addInstrumentToOrbit(unassignedInst.get(pprng.nextInt(unassignedInst.size())), orbitInd);
                        }
                    }
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("Expected mode to be {0,1,2}. Found %d", mode));
            }
        }

        return arch;
    }

    @Override
    public String toString() {
        return "EOSSOperator{" + "mode=" + mode + ", arg=" + arg + ", orbit=" + orbit + ", instrument=" + Arrays.toString(instrument) + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + this.mode;
        hash = 83 * hash + this.arg;
        hash = 83 * hash + this.orbit;
        hash = 83 * hash + Arrays.hashCode(this.instrument);
        return hash;
    }

    @Override
    public boolean equals(Object obj
    ) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EOSSOperator other = (EOSSOperator) obj;
        if (this.mode != other.mode) {
            return false;
        }
        if (this.arg != other.arg) {
            return false;
        }
        if (this.orbit != other.orbit) {
            return false;
        }
        if (!Arrays.equals(this.instrument, other.instrument)) {
            return false;
        }
        return true;
    }

        }
