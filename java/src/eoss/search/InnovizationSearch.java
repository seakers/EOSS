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
import architecture.io.ResultIO;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Callable;
import knowledge.operator.EOSSOperatorCreator;
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

    /**
     * Constructs new search and automatically initializes Jess
     *
     * @param alg
     * @param properties
     * @param dataLabeler
     * @param ops
     * @param savePath
     * @param name
     */
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
        int nOpsToAdd = (int) properties.getInt("nOpsToAdd", 2);

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

        //keep track of each solution that is ever created, but only keep the unique ones
        HashSet<Solution> allSolutions = new HashSet();
        Population initPop = ((AbstractEvolutionaryAlgorithm) alg).getPopulation();
        for (int i = 0; i < initPop.size(); i++) {
            initPop.get(i).setAttribute("NFE", 0);
            allSolutions.add(initPop.get(i));
        }

        //count the number of times we reset operators
        int opResetCount = 0;

        while (!instAlgorithm.isTerminated() && (instAlgorithm.getNumberOfEvaluations() < maxEvaluations)) {
            Population pop = ((AbstractEvolutionaryAlgorithm) alg).getPopulation();

            int nFuncEvals = instAlgorithm.getNumberOfEvaluations();
            //Check if the operators need to be replaced
            if (ops.checkTrigger(alg)) {
                System.out.println(String.format("Operator replacement event triggered at %d func eval", nFuncEvals));
                //for now reset the qualities
                alg.getNextHeuristicSupplier().reset();

                //remove inefficient operators
                Collection<Variation> removedOperators = ops.removeOperators(alg);
                for (Variation op : removedOperators) {
                    if (op instanceof CompoundVariation) {
                        System.out.println(String.format("Removed: %s", ((CompoundVariation) op).getName()));
                    } else {
                        System.out.println(String.format("Removed: %s", op.toString()));
                    }
                }

                //conduct learning
                Population allSolnPop = new Population(allSolutions);
                dataLabeler.label(allSolnPop);
                String labledDataFile = savePath + File.separator + name + "_" + String.valueOf(opResetCount) + "_labels.csv";
                lableIO.saveLabels(allSolnPop, labledDataFile, ",");

                String featureDataFile = savePath + File.separator + name + "_" + String.valueOf(opResetCount) + "_features.txt";
                                
                //The association rule mining engine
                DrivingFeaturesGenerator dfg = new DrivingFeaturesGenerator(alg.getProblem().getNumberOfVariables());
                dfg.getDrivingFeatures(labledDataFile, featureDataFile, nOpsToAdd);

                opCreator.learnFeatures(new File(featureDataFile));

                //add new operators
                Collection<Variation> newOperators = ops.addNewOperator(alg, nOpsToAdd);
                alg.getNextHeuristicSupplier().reset();
                for (Variation op : newOperators) {
                    if (op instanceof CompoundVariation) {
                        System.out.println(String.format("Added: %s", ((CompoundVariation) op).getName()));
                    } else {
                        System.out.println(String.format("Added: %s", op.toString()));
                    }
                }
                opResetCount++;
            }

            //print out the search stats every once in a while
            if (nFuncEvals % 500 == 0) {
                System.out.println("NFE: " + instAlgorithm.getNumberOfEvaluations());
                System.out.print("Popsize: " + ((AbstractEvolutionaryAlgorithm) alg).getPopulation().size());
                System.out.println("  Archivesize: " + ((AbstractEvolutionaryAlgorithm) alg).getArchive().size());
            }
            instAlgorithm.step();

            //since new solutions are put at end of population, only check the last few to see if any new solutions entered population
            for (int i = 1; i < 3; i++) {
                Solution s = pop.get(pop.size() - i);
                s.setAttribute("NFE", instAlgorithm.getNumberOfEvaluations());
                allSolutions.add(s);
            }
        }

        Population allpop = new Population();
        Iterator<Solution> iter = allSolutions.iterator();
        while (iter.hasNext()) {
            allpop.add(iter.next());
        }

        alg.terminate();
        long finishTime = System.currentTimeMillis();
        System.out.println("Done with optimization. Execution time: " + ((finishTime - startTime) / 1000) + "s");

        String filename = savePath + File.separator + alg.getClass().getSimpleName() + "_" + name;
        ResultIO.saveSearchMetrics(instAlgorithm, filename);
        ResultIO.savePopulation(((AbstractEvolutionaryAlgorithm) alg).getPopulation(), filename);
        ResultIO.savePopulation(allpop, filename + "_all");
        ResultIO.saveObjectives(instAlgorithm.getResult(), filename);

        if (alg instanceof IAOS) {
            IAOS algAOS = (IAOS) alg;
            if (properties.getBoolean("saveQuality", false)) {
                IOQualityHistory ioqh = new IOQualityHistory();
                ioqh.saveHistory(algAOS.getQualityHistory(), savePath + File.separator + name + ".qual", ",");
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
