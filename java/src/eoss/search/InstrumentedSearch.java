/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.search;

import aos.IO.IOCreditHistory;
import aos.IO.IOQualityHistory;
import aos.IO.IOSelectionHistory;
import aos.aos.IAOS;
import architecture.ResultIO;
import eoss.problem.EOSSArchitecture;
import eoss.problem.EOSSProblem;
import java.io.File;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;
import org.moeaframework.Instrumenter;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.analysis.collector.InstrumentedAlgorithm;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Population;
import org.moeaframework.core.PopulationIO;
import org.moeaframework.core.Solution;
import org.moeaframework.util.TypedProperties;

/**
 *
 * @author nozomihitomi
 */
public class InstrumentedSearch implements Callable<Algorithm> {

    private final String savePath;
    private final String name;
    private final Algorithm alg;
    private final TypedProperties properties;

    /**
     * parameter to decide whether to start jess in constructor
     */
    private final boolean jessInit;

    public InstrumentedSearch(Algorithm alg, TypedProperties properties, String savePath, String name, boolean init) {
        this.alg = alg;
        this.properties = properties;
        this.savePath = savePath;
        this.name = name;
        this.jessInit = init;
        if (init) {
            //initialize Jess
            ((EOSSProblem) alg.getProblem()).renewJess();
        }
    }

    @Override
    public Algorithm call() throws Exception {
        if (!jessInit) {
            ((EOSSProblem) alg.getProblem()).renewJess();
        }

        int populationSize = (int) properties.getDouble("populationSize", 600);
        int maxEvaluations = (int) properties.getDouble("maxEvaluations", 10000);

        Population referencePopulation = PopulationIO.readObjectives(new File(savePath + File.separator + "ref.obj"));

        Instrumenter instrumenter = new Instrumenter().withFrequency(5)
                .withReferenceSet(new NondominatedPopulation(referencePopulation))
                .attachHypervolumeJmetalCollector(new Solution(new double[]{2.0, 2.0}))
                .attachElapsedTimeCollector();

        InstrumentedAlgorithm instAlgorithm = instrumenter.instrument(alg);

        // run the executor using the listener to collect results
        System.out.println("Starting " + alg.getClass().getSimpleName() + " on " + alg.getProblem().getName() + " with pop size: " + populationSize);
        alg.step();
        long startTime = System.currentTimeMillis();

        HashMap<BitSet, Solution> allSolutions = new HashMap();
        Population initPop = ((AbstractEvolutionaryAlgorithm) alg).getPopulation();
        for (int i = 0; i < initPop.size(); i++) {
            allSolutions.put(((EOSSArchitecture) initPop.get(i)).getBitString(), initPop.get(i));
        }

        while (!instAlgorithm.isTerminated() && (instAlgorithm.getNumberOfEvaluations() < maxEvaluations)) {
            if (instAlgorithm.getNumberOfEvaluations() % 500 == 0) {
                ((EOSSProblem) instAlgorithm.getProblem()).renewJess();
                System.out.println("NFE: " + instAlgorithm.getNumberOfEvaluations());
                System.out.print("Popsize: " + ((AbstractEvolutionaryAlgorithm) alg).getPopulation().size());
                System.out.println("  Archivesize: " + ((AbstractEvolutionaryAlgorithm) alg).getArchive().size());
            }
            instAlgorithm.step();
        }

        Population allpop = new Population();
        Iterator<BitSet> iter = allSolutions.keySet().iterator();
        while (iter.hasNext()) {
            allpop.add(allSolutions.get(iter.next()));
        }

        alg.terminate();
        long finishTime = System.currentTimeMillis();
        System.out.println("Done with optimization. Execution time: " + ((finishTime - startTime) / 1000) + "s");

        ResultIO resio = new ResultIO();
        String filename = savePath + File.separator + alg.getClass().getSimpleName() + "_" + name;
        resio.saveSearchMetrics(instAlgorithm, filename);
        resio.savePopulation(((AbstractEvolutionaryAlgorithm) alg).getPopulation(), filename);
        resio.saveObjectives(instAlgorithm.getResult(), filename);

        if (alg instanceof IAOS) {
            IAOS algAOS = (IAOS) alg;
            if (properties.getBoolean("saveQuality", false)) {
                IOQualityHistory ioqh = new IOQualityHistory();
                ioqh.saveHistory(algAOS.getQualityHistory(), savePath + File.separator + name + ".credit", ",");
            }
            if (properties.getBoolean("saveCredits", false)) {
                IOCreditHistory ioch = new IOCreditHistory();
                ioch.saveHistory(algAOS.getCreditHistory(), savePath + File.separator + name + ".credit", ",");
            }
            if (properties.getBoolean("saveSelection", false)) {
                IOSelectionHistory iosh = new IOSelectionHistory();
                iosh.saveHistory(algAOS.getSelectionHistory(), savePath + File.separator + name + ".hist", ",");
            }
        }
        return alg;
    }

}
