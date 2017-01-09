/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Mission;
import eoss.problem.Orbit;
import eoss.problem.Spacecraft;
import eoss.problem.assignment.InstrumentAssignmentArchitecture;
import eoss.problem.assignment.operators.AbstractEOSSOperator;
import java.util.ArrayList;
import java.util.Collection;
import org.moeaframework.core.ParallelPRNG;

/**
 * Checks that the data rate duty cycle of the spacecraft fall within the
 * acceptable bounds. Multiple changes may occur within a spacecraft to ensure
 * the threshold bound. Random instruments are removed until acceptable bounds
 * are met. User can define whether to change one or multiple spacecraft
 *
 * @author nozomihitomi
 */
public class RepairDataDutyCycle extends AbstractEOSSOperator {

    /**
     * The data rate duty cycle that a spacecraft must be at or higher
     */
    private final double threshold;

    /**
     * The number of modifications that this operators is allowed to make
     */
    private final int numModifications;

    private final ParallelPRNG pprng;

    public RepairDataDutyCycle(double threshold, int numModifications) {
        this.threshold = threshold;
        this.numModifications = numModifications;
        this.pprng = new ParallelPRNG();
    }
    
    @Override
    protected InstrumentAssignmentArchitecture evolve(InstrumentAssignmentArchitecture child) {
        child.setMissions();
        InstrumentAssignmentArchitecture copy = (InstrumentAssignmentArchitecture) child.copy();
        ArrayList<Orbit> candidateOrbits = new ArrayList();
        for (String name : child.getMissionNames()) {
            Mission mission = child.getMission(name);
            for (Spacecraft s : mission.getSpacecraft().keySet()) {
                if(checkDataRate(copy, mission.getSpacecraft().get(s), threshold)){
                    candidateOrbits.add(mission.getSpacecraft().get(s));
                }
            }
        }
        for (int i = 0; i < numModifications; i++) {
            if (i > child.getNorbits() || i >= candidateOrbits.size()) {
                break;
            }
            Orbit change = candidateOrbits.get(pprng.nextInt(candidateOrbits.size()));
            int index = EOSSDatabase.findOrbitIndex(change);
            while (!checkDataRate(copy, change, threshold)) {
                ArrayList<Integer> instruments = copy.getInstrumentsInOrbit(index);
                copy.removeInstrumentFromOrbit(instruments.get(pprng.nextInt(instruments.size())), index);
            }
        }
        return copy;
    }
    
    /**
     * Computes the data rate duty cycle from the data rate per orbit
     * @param dataRatePerOrbit
     * @return 
     */
    private double computeDataRateDutyCycle(double dataRatePerOrbit){
        return (1. * 7. * 60. * 500. * (1. / 8192.)) / dataRatePerOrbit;
    }
    
    /**
     * Check to see if the data rate duty cycle of this spacecraft meets the threshold
     * @param arch
     * @param orbit
     * @param threshold
     * @return 
     */
    private boolean checkDataRate(InstrumentAssignmentArchitecture arch, Orbit orbit, double threshold){
        Collection<Instrument> payload = arch.getInstrumentsInOrbit(orbit);
        double dataRate = 0;
        for(Instrument inst : payload){
            dataRate += Double.parseDouble(inst.getProperty("average-data-rate#"));
        }
        double dataRatePerOrbit = (dataRate * 1.2 * orbit.getPeriod()) / (1024 * 8); //(GByte/orbit) 20% overhead
        return computeDataRateDutyCycle(dataRatePerOrbit) > threshold;
    }

    @Override
    public int getArity() {
        return 1;
    }

}
