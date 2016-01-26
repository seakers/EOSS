/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import java.io.File;
import org.moeaframework.Instrumenter;
import org.moeaframework.algorithm.NSGAII;
import org.moeaframework.analysis.collector.InstrumentedAlgorithm;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.NondominatedSortingPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Variation;
import org.moeaframework.core.comparator.ChainedComparator;
import org.moeaframework.core.comparator.CrowdingComparator;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.operator.GAVariation;
import org.moeaframework.core.operator.OnePointCrossover;
import org.moeaframework.core.operator.TournamentSelection;
import org.moeaframework.core.operator.binary.BitFlip;
import org.moeaframework.util.TypedProperties;
import architecture.ArchitectureGenerator;
import architecture.ResultIO;
import eoss.problem.operators.AddRandomToSmallSatellite;
import eoss.problem.operators.AddSynergy;
import eoss.problem.operators.ImproveOrbit;
import eoss.problem.operators.RemoveInterference;
import eoss.problem.operators.RemoveRandomFromLoadedSatellite;
import eoss.problem.operators.RemoveSuperfluous;
import hh.hyperheuristics.HHFactory;
import hh.hyperheuristics.HeMOEA;
import hh.nextheuristic.INextHeuristic;
import hh.rewarddefinition.IRewardDefinition;
import hh.rewarddefinition.RewardDefFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.moeaframework.algorithm.EpsilonMOEA;
import org.moeaframework.core.EpsilonBoxDominanceArchive;
import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;
import org.moeaframework.core.comparator.DominanceComparator;

/**
 *
 * @author dani
 */
public class RBSAEOSSSMAP {

    /**
     * First argument is the path to the project folder. Second argument is the
     * mode. Third argument is the number of ArchitecturalEvaluators to
     * initialize.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        //PATH
        args = new String[3];
        args[0] = "/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric";
//          args[0] = "C:\\Users\\SEAK1\\Dropbox\\EOSS\\problems\\climateCentric";
        args[1] = "3"; //mode
        args[2] = "3";

        System.out.println("Path set to " + args[0]);
        System.out.println("Running mode " + args[1]);
        System.out.println("Will get " + args[2] + " resources");

        String path = args[0];

        int MODE = Integer.parseInt(args[1]);
        int numCPU = Integer.parseInt(args[2]);

        Problem problem = initEOSSProblem(path, "FUZZY-ATTRIBUTES", "test", "normal", true, numCPU);

        //parameters and operators for search
        TypedProperties properties = new TypedProperties();
        //search paramaters set here
        int popSize = 2;
        properties.setInt("maxEvaluations", 60);
        properties.setInt("populationSize", popSize);
        double crossoverProbability = 1.0;
        double mutationProbability = 0.01;
        Variation singlecross = new OnePointCrossover(crossoverProbability);
        Variation BitFlip = new BitFlip(mutationProbability);
        Variation GAVariation = new GAVariation(singlecross, BitFlip);
        Initialization initialization = new ArchitectureGenerator(problem, popSize, "random");

        //setup for epsilon MOEA
        Population population = new Population();
        DominanceComparator comparator = new ParetoDominanceComparator();
        EpsilonBoxDominanceArchive archive = new EpsilonBoxDominanceArchive(new double[]{0.001, 10});
        final TournamentSelection selection = new TournamentSelection(2, comparator);

        switch (MODE) {
            case 1: //NSGAII Search

                //setup NSGAII
                NondominatedSortingPopulation ndsPopulation = new NondominatedSortingPopulation();

                TournamentSelection tSelection = new TournamentSelection(2,
                        new ChainedComparator(
                                new ParetoDominanceComparator(),
                                new CrowdingComparator()));

                //NSGA crossover probability should be less than 1
                double crossoverProbability08 = 0.8;
                Variation singlecross08 = new OnePointCrossover(crossoverProbability08);
                Variation NSGAVariation = new GAVariation(singlecross08, BitFlip);
                
                Algorithm nsga2 = new NSGAII(problem, ndsPopulation, null, tSelection, GAVariation,
                        initialization);

                runSearch(nsga2, properties, path);

                break;
            case 2: //Use epsilonMOEA
                Algorithm eMOEA = new EpsilonMOEA(problem, population, archive, selection, GAVariation, initialization);

                runSearch(eMOEA, properties, path);

            case 3://Hyperheuristic search
                IRewardDefinition creditAssignment;
                try {
                    creditAssignment = RewardDefFactory.getInstance().getCreditDef("CEA", properties, problem);

                    int injectionRate = (int) properties.getDouble("injectionRate", 0.25);
                    //for injection
                    int lagWindow = (int) properties.getDouble("lagWindow", 50);

                    ArrayList<Variation> heuristics = new ArrayList();
                    //add domain-specific heuristics
                    heuristics.add(new AddRandomToSmallSatellite(300));
                    heuristics.add(new RemoveRandomFromLoadedSatellite(1500));
                    heuristics.add(new RemoveSuperfluous(10));
                    heuristics.add(new ImproveOrbit());
                    heuristics.add(new RemoveInterference(10));
                    heuristics.add(new AddSynergy(10));
                    //add domain-independent heuristics
                    heuristics.add(BitFlip);
                    heuristics.add(singlecross);

                    //all other properties use default parameters
                    INextHeuristic selector = HHFactory.getInstance().getHeuristicSelector("AP", properties, heuristics);

                    HeMOEA hemoea = new HeMOEA(problem, population, archive, selection,
                            initialization, selector, creditAssignment, injectionRate, lagWindow);
                    InstrumentedAlgorithm instAlg = runSearch(hemoea, properties, path);
                } catch (IOException ex) {
                    Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
//            case 5://Update DSMs
//                AE.init(numAE);
//                //AE.recomputeAllDSM();
//                AE.recomputeNDSM(2);
//                AE.clearResults();
//                AE.recomputeNDSM(3);
//                AE.clearResults();
//                AE.recomputeNDSM(Params.nInstruments);
//                try {
//                    SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss" );
//                    String stamp = dateFormat.format( new Date() );
//                    FileOutputStream file = new FileOutputStream( Params.path_save_results + "\\all_dsms" + stamp + ".dat");
//                    ObjectOutputStream os = new ObjectOutputStream( file );
//                    os.writeObject( AE.getDsm_map() );
//                    os.close();
//                    file.close();
//                } catch (Exception e) {
//                    System.out.println( e.getMessage() );
//                }
//                System.out.println("DONE");
//                break;
//             case 7://Update scores file
//                AE.init(numAE);
//                AE.recomputeScores(1);
//                AE.clearResults();
//                AE.recomputeScores(2);
//                AE.clearResults();
//                AE.recomputeScores(3);
//                AE.clearResults();
//                AE.recomputeScores(Params.nInstruments);
//                AE.clearResults();
//                AE.recomputeScores(Params.nInstruments-1);
//                try{
//                    SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss" );
//                    String stamp = dateFormat.format( new Date() );
//                    FileOutputStream fos = new FileOutputStream(Params.path_save_results + "\\scores" + stamp + ".dat");
//                    ObjectOutputStream oos = new ObjectOutputStream(fos);
//                    oos.writeObject(AE.getScores());
//                    oos.writeObject(AE.getSubobj_scores());
//                    oos.close();
//                    fos.close();
//                } catch(Exception e) {
//                    System.out.println(e.getMessage());
//                }
//                System.out.println("DONE");
//                break;
            default:
                System.out.println("Choose a mode between 1 and 9");
        }

    }

    public static Problem initEOSSProblem(String path, String fuzzyMode, String testMode, String normalMode, boolean explanation, int numCPU) {
        EOSSDatabase.getInstance(); //to initiate database
        Params params = new Params(path, fuzzyMode, testMode, normalMode);//FUZZY or CRISP;
        return new EOSSProblem(Params.altnertivesForNumberOfSatellites, EOSSDatabase.getInstruments(), EOSSDatabase.getOrbits(), null, explanation, true);
    }

    public static InstrumentedAlgorithm runSearch(Algorithm alg, TypedProperties properties, String savePath) {
        int populationSize = (int) properties.getDouble("populationSize", 600);
        int maxEvaluations = (int) properties.getDouble("maxEvaluations", 10000);

        Instrumenter instrumenter = new Instrumenter().withFrequency(populationSize)
                .attachHypervolumeJmetalCollector(new Solution(new double[]{}))
                .attachElapsedTimeCollector();

        InstrumentedAlgorithm instAlgorithm = instrumenter.instrument(alg);

        // run the executor using the listener to collect results
        System.out.println("Starting " + alg.getClass().getSimpleName() + " on " + alg.getProblem().getName() + " with pop size: " + populationSize);
        long startTime = System.currentTimeMillis();
        while (!instAlgorithm.isTerminated() && (instAlgorithm.getNumberOfEvaluations() < maxEvaluations)) {
            instAlgorithm.step();
        }

        alg.terminate();
        long finishTime = System.currentTimeMillis();
        System.out.println("Done with optimization. Execution time: " + ((finishTime - startTime) / 1000) + "s");

        ResultIO resio = new ResultIO();
        String filename = savePath + File.separator + "result";
        resio.saveMetrics(instAlgorithm, filename);
        resio.savePopulation(instAlgorithm.getResult(), filename);
        return instAlgorithm;
    }
}
