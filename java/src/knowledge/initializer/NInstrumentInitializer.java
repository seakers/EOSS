package knowledge.initializer;

import eoss.problem.EOSSDatabase;
import eoss.problem.assignment.InstrumentAssignment2;
import eoss.problem.assignment.InstrumentAssignmentArchitecture2;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.ParallelPRNG;
import org.moeaframework.core.Solution;

/**
 * Created by nozomihitomi on 3/21/17.
 */
public class NInstrumentInitializer implements Initialization{

    private final int nSatellites;

    private final int nInstruments;

    private final InstrumentAssignment2 problem;

    private final int popSize;

    public NInstrumentInitializer(int nSatellites, int nInstruments, InstrumentAssignment2 problem, int popSize) {
        this.nSatellites = nSatellites;
        this.nInstruments = nInstruments;
        this.problem = problem;
        this.popSize = popSize;
    }

    @Override
    public Solution[] initialize() {
        Solution[] out = new Solution[popSize];
        ParallelPRNG pprng = new ParallelPRNG();
        for (int i = 0; i < popSize; i++) {
            InstrumentAssignmentArchitecture2 arch = (InstrumentAssignmentArchitecture2)problem.newSolution();
            for (int j = 0; j < nSatellites; j++) {
                for (int k = 0; k < nInstruments; k++) {
                    arch.addInstrumentToSpacecraft(pprng.nextInt(EOSSDatabase.getNumberOfInstruments()), j);
                }
                arch.setOrbit(pprng.nextInt(EOSSDatabase.getNumberOfOrbits()), j);
            }
            out[i] = arch;
        }
        return out;
    }
}
