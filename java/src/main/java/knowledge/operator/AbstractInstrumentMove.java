/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import seakers.aos.operator.CheckParents;
import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Mission;
import eoss.problem.Orbit;
import eoss.problem.assignment.InstrumentAssignmentArchitecture2;
import eoss.spacecraft.Spacecraft;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;

/**
 *
 * @author nozomihitomi
 */
public abstract class AbstractInstrumentMove implements Variation, CheckParents {

    /**
     * The number of changes to make to the architecture
     */
    private final int nChanges;

    /**
     * The list of possible swaps that can improve the architecture
     */
    private final ArrayList<Move> moves;

    /**
     *
     * @param nChanges
     */
    public AbstractInstrumentMove(int nChanges) {
        this.nChanges = nChanges;
        this.moves = new ArrayList<>();
    }

    @Override
    public Solution[] evolve(Solution[] sltns) {
        InstrumentAssignmentArchitecture2 child = (InstrumentAssignmentArchitecture2) sltns[0];
        InstrumentAssignmentArchitecture2 copy = (InstrumentAssignmentArchitecture2) child.copy();
        copy.setMissions();
        for (Mission mi : copy.getMissions()) {
            for (Spacecraft si : mi.getSpacecraft().keySet()) {
                for (Instrument inst : checkThisSpacecraft(si, mi.getSpacecraft().get(si))) {
                    //search for a satellite in the architecture with missing instrument
                    for (String namej : copy.getMissionNames()) {
                        Mission mj = copy.getMission(namej);
                        for (Spacecraft sj : mj.getSpacecraft().keySet()) {
                            if (!si.equals(sj) && checkOtherSpacecraft(sj, mj.getSpacecraft().get(sj), inst)) {
                                if (addToCurrentSpacecraft()) {
                                    moves.add(new Move(mi, mj, inst));
                                } else {
                                    moves.add(new Move(mj, mi, inst));
                                }
                            }
                        }
                    }
                }
            }
        }

        //repair the architecture
        Collections.shuffle(moves);
        int i = 0;
        int changes = 0;

        while (i < moves.size() && changes < nChanges) {
            Move swap = moves.get(i);
            int instInd = EOSSDatabase.findInstrumentIndex(swap.getInst());
            if (addToCurrentSpacecraft()) {
                //if other spacecraft still has the instrument to remove then add it to the original spacecraft
                if (copy.removeInstrumentFromSpacecraft(instInd, swap.getRemove())) {
                    if (!copy.addInstrumentToSpacecraft(instInd, swap.getAdd())) {
                        //since the swap wasn't successful, return the instrument to the other spacecraft
                        copy.addInstrumentToSpacecraft(instInd, swap.getRemove());
                    }
                    changes++;
                }
            } else {
                //if other spacecraft still can accept the instrument to add then remove it from the original spacecraft
                if (copy.addInstrumentToSpacecraft(instInd, swap.getAdd())) {
                    if (!copy.removeInstrumentFromSpacecraft(instInd, swap.getRemove())) {
                        //since the swap wasn't successful, remove the instrument that was added to the other spacecraft
                        copy.removeInstrumentFromSpacecraft(instInd, swap.getAdd());
                    }
                    changes++;
                }
            }
            i++;
        }
        return new Solution[]{copy
        };
    }

    /**
     * Checks the given spacecraft in the given orbit for instruments that can
     * be added or removed (must be either or) to improve the spacecraft
     *
     * @param s the spacecraft
     * @param o the orbit occupied by the given spacecraft
     * @return the list of instruments to be added or removed from the
     * spacecraft that will improve the performance/cost of the spacecraft or
     * architecture
     */
    protected abstract Collection<Instrument> checkThisSpacecraft(Spacecraft s, Orbit o);

    /**
     * Checks the other spacecraft if the given instrument is present or absent
     *
     * @param s the other spacecraft
     * @param o the orbit occupied by the other spacecraft
     * @param inst the instrument to swap
     * @return true if the other spacecraft is a candidate for an instrument
     * swap. else false.
     */
    protected abstract boolean checkOtherSpacecraft(Spacecraft s, Orbit o, Instrument inst);

    /**
     * When comparing the current spacecraft with other spacecraft, this method
     * returns true if instruments should be added to the current spacecraft and
     * returns false if instruments should be removed from the current
     * spacecraft
     *
     * @return
     */
    protected abstract boolean addToCurrentSpacecraft();

    @Override
    public boolean check(Solution[] sltns) {
        for (Solution sol : sltns) {
            InstrumentAssignmentArchitecture2 arch = (InstrumentAssignmentArchitecture2) sol;
            for (Mission mi : arch.getMissions()) {
                for (Spacecraft si : mi.getSpacecraft().keySet()) {
                    for (Instrument inst : checkThisSpacecraft(si, mi.getSpacecraft().get(si))) {
                        //search for a satellite in the architecture with missing instrument
                        for (Mission mj : arch.getMissions()) {
                            for (Spacecraft sj : mj.getSpacecraft().keySet()) {
                                if (!si.equals(sj) && checkOtherSpacecraft(sj, mj.getSpacecraft().get(sj), inst)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Class that stores information on which instrument to swap between two
     * spacecraft
     */
    private class Move {

        private final Instrument inst;

        private final Mission mAdd;

        private final Mission mRemove;

        /**
         * Creates a new synergy pair where the instrument shall be added to the
         * "add" spacecraft and removed from the "remove" spacecraft
         *
         * @param inst Instrument to add and remove
         * @param add spacecraft to add instrument
         * @param remove spacecraft to remove instrument
         */
        public Move(Mission mAdd, Mission mRemove, Instrument inst) {
            this.inst = inst;
            this.mAdd = mAdd;
            this.mRemove = mRemove;
        }

        /**
         * Gets the instrument to remove and add
         *
         * @return
         */
        public Instrument getInst() {
            return inst;
        }

        /**
         * Gets the spacecraft from which the instrument shall be added
         *
         * @return the spacecraft from which the instrument shall be added
         */
        public Mission getAdd() {
            return mAdd;
        }

        /**
         * Gets the spacecraft from which the instrument shall be removed
         *
         * @return the spacecraft from which the instrument shall be removed
         */
        public Mission getRemove() {
            return mRemove;
        }

    }

    @Override
    public int getArity() {
        return 1;
    }

}
