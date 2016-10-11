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
import aos.operatorselectors.replacement.OperatorReplacementStrategy;
import architecture.ResultIO;
import eoss.problem.EOSSArchitecture;
import eoss.problem.EOSSProblem;
import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;
import knowledge.operator.EOSSOperatorCreator;
import mining.DrivingFeature;
import mining.DrivingFeaturesGenerator;
import mining.label.AbstractPopulationLabeler;
import mining.label.LabelIO;
import org.moeaframework.Instrumenter;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.analysis.collector.InstrumentedAlgorithm;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Population;
import org.moeaframework.core.PopulationIO;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;
import org.moeaframework.core.operator.CompoundVariation;
import org.moeaframework.util.TypedProperties;

/**
 * This method applies innovization to increase the efficiency of the search
 *
 * @author nozomihitomi
 */
public class InnovizationSearch implements Callable<Algorithm> {

    /**
     * The path to save the results
     */
    private final String savePath;
    /**
     * the name of the result files
     */
    private final String name;

    /**
     * the adaptive operator selector algorithm to use
     */
    private final IAOS alg;

    /**
     * the properties associated with the algorithm
     */
    private final TypedProperties properties;

    /**
     * Class that supports method to label the interesting data
     */
    private final AbstractPopulationLabeler dataLabeler;

    /**
     * Responsible for exporting the labels
     */
    private final LabelIO lableIO;

    /**
     * operator creator for EOSS assignment problems
     */
    private final EOSSOperatorCreator opCreator;

    /**
     * the strategy for how to and when to remove and add operators
     */
    private final OperatorReplacementStrategy ops;

    public InnovizationSearch(IAOS alg, TypedProperties properties, AbstractPopulationLabeler dataLabeler, OperatorReplacementStrategy ops, String savePath, String name) {
        this.alg = alg;
        this.properties = properties;
        this.savePath = savePath;
        this.name = name;
        this.dataLabeler = dataLabeler;
        this.lableIO = new LabelIO();
        this.ops = ops;
        if (!(ops.getOperatorCreator() instanceof EOSSOperatorCreator)) {
            throw new IllegalArgumentException(String.format("Expected EOSSOperatorCreator as operator creation strategy. Found %s", ops.getOperatorCreator().getClass().getSimpleName()));
        } else {
            this.opCreator = (EOSSOperatorCreator) ops.getOperatorCreator();
        }
    }

    @Override
    public Algorithm call() throws Exception {
        int populationSize = (int) properties.getDouble("populationSize", 600);
        int maxEvaluations = (int) properties.getDouble("maxEvaluations", 10000);

        Population referencePopulation = PopulationIO.readObjectives(new File(savePath + File.separator + "ref.obj"));

        Instrumenter instrumenter = new Instrumenter().withFrequency(5)
                .withReferenceSet(new NondominatedPopulation(referencePopulation))
                .attachHypervolumeJmetalCollector(new Solution(new double[]{1.0, 2.0}))
                .attachElapsedTimeCollector();

        InstrumentedAlgorithm instAlgorithm = instrumenter.instrument(alg);

        // run the executor using the listener to collect results
        System.out.println("Starting " + alg.getClass().getSimpleName() + " on " + alg.getProblem().getName() + " with pop size: " + populationSize);
        long startTime = System.currentTimeMillis();

        //keep track of each solution that is ever created, but only keep the unique ones
        HashMap<BitSet, Solution> allSolutions = new HashMap();
        Population initPop = ((AbstractEvolutionaryAlgorithm) alg).getPopulation();
        for (int i = 0; i < initPop.size(); i++) {
            allSolutions.put(((EOSSArchitecture) initPop.get(i)).getBitString(), initPop.get(i));
        }

        //The association rule mining engine
        DrivingFeaturesGenerator dfg = new DrivingFeaturesGenerator();

        while (!instAlgorithm.isTerminated() && (instAlgorithm.getNumberOfEvaluations() < maxEvaluations)) {
            Population pop = ((AbstractEvolutionaryAlgorithm) alg).getPopulation();
            //since new solutions are put at end of population, only check the last few to see if any new solutions entered population
            for (int i = pop.size() - 3; i < pop.size(); i++) {
                if (!allSolutions.containsKey(((EOSSArchitecture) pop.get(i)).getBitString())) {
                    allSolutions.put(((EOSSArchitecture) pop.get(i)).getBitString(), pop.get(i));
                }
            }

            int nFuncEvals = instAlgorithm.getNumberOfEvaluations();

            //Check if the operators need to be replaced
            if (ops.checkTrigger(alg)) {
                System.out.println(String.format("Operator replacement event triggered at %d func eval", nFuncEvals));
                //for now reset the qualities
                alg.getNextHeuristicSupplier().reset();

                //remove inefficient operators
                Collection<Variation> removedOperators = ops.removeOperators(alg);
                for (Variation op : removedOperators) {
                    System.out.println(String.format("Removed: %s", op.toString()));
                }

                //conduct learning
                Population allSolnPop = new Population(allSolutions.values());
                dataLabeler.label(allSolnPop);
                String labledDataFile = savePath + File.separator + name + "_labels.csv";
                lableIO.saveLabels(allSolnPop, labledDataFile, ",");
                // Find driving features
                dfg.getDrivingFeatures(labledDataFile);
                // Sort driving features based on the metric of your choice (0: support, 1: lift, 2: confidence)
                String featureDataFile = savePath + File.separator + name + "_features.txt";
                dfg.exportDrivingFeatures(1, featureDataFile);

                opCreator.learnFeatures(new File(featureDataFile));

                //add new operators
                Collection<Variation> newOperators = ops.addNewOperator(alg, 1);
                for (Variation op : newOperators) {
                    if (op instanceof CompoundVariation) {
                        System.out.println(String.format("Added: %s", ((CompoundVariation) op).getName()));
                    } else {
                        System.out.println(String.format("Added: %s", op.toString()));
                    }
                }
            }

            //print out the search stats every once in a while
            if (nFuncEvals % 500 == 0) {
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
        resio.savePopulation(allpop, filename + "allpop");
        resio.saveObjectives(instAlgorithm.getResult(), filename);

        if (properties.getBoolean("saveQuality", false)) {
            IOQualityHistory ioqh = new IOQualityHistory();
            ioqh.saveHistory(alg.getQualityHistory(), savePath + File.separator + name + ".credit", ",");
        }
        if (properties.getBoolean("saveCredits", false)) {
            IOCreditHistory ioch = new IOCreditHistory();
            ioch.saveHistory(alg.getCreditHistory(), savePath + File.separator + name + ".credit", ",");
        }
        if (properties.getBoolean("saveSelection", false)) {
            IOSelectionHistory iosh = new IOSelectionHistory();
            iosh.saveHistory(alg.getSelectionHistory(), savePath + File.separator + name + ".hist", ",");
        }

        return alg;
    }

}
