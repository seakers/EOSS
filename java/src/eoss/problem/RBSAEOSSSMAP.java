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
import hh.IO.IOCreditHistory;
import hh.IO.IOSelectionHistory;
import hh.hyperheuristics.HHFactory;
import hh.hyperheuristics.HeMOEA;
import hh.nextheuristic.INextHeuristic;
import hh.rewarddefinition.IRewardDefinition;
import hh.rewarddefinition.RewardDefFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.algorithm.EpsilonMOEA;
import org.moeaframework.core.EpsilonBoxDominanceArchive;
import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;
import org.moeaframework.core.comparator.DominanceComparator;
import org.moeaframework.core.operator.CompoundVariation;

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
//        args[0] = "/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric";
//          args[0] = "C:\\Users\\SEAK1\\Dropbox\\EOSS\\problems\\climateCentric";
        if (args.length==0) {
            args = new String[3];
            args[0] = "C:\\Users\\SEAK2\\Nozomi\\EOSS\\problems\\climateCentric";
            args[1] = "3";
            args[2] = "3";
        }

        System.out.println("Path set to " + args[0]);
        System.out.println("Running mode " + args[1]);
        System.out.println("Will get " + args[2] + " resources");

        String path = args[0];

        int MODE = Integer.parseInt(args[1]);
        int numCPU = Integer.parseInt(args[2]);

        Problem problem = initEOSSProblem(path, "FUZZY-ATTRIBUTES", "test", "normal", false, numCPU);

        //parameters and operators for search
        TypedProperties properties = new TypedProperties();
        //search paramaters set here
        int popSize = 100;
        properties.setInt("maxEvaluations", 5025);
        properties.setInt("populationSize", popSize);
        double crossoverProbability = 1.0;
        double mutationProbability = 0.01;
        Variation singlecross = new OnePointCrossover(crossoverProbability);
        Variation BitFlip = new BitFlip(mutationProbability);
        Variation GAVariation = new GAVariation(singlecross, BitFlip);
        Initialization initialization = new ArchitectureGenerator(problem, popSize, "random");

        //setup for epsilon MOEA
        
        DominanceComparator comparator = new ParetoDominanceComparator();
        double[] epsilonDouble = new double[]{0.001, 0.001};
        final TournamentSelection selection = new TournamentSelection(2, comparator);

        String time = String.valueOf(System.currentTimeMillis());
        switch (MODE) {
            case 1: //MOEA/D

                //setup MOEAD
                NondominatedSortingPopulation ndsPopulation = new NondominatedSortingPopulation();

                TournamentSelection tSelection = new TournamentSelection(2,
                        new ChainedComparator(
                                new ParetoDominanceComparator(),
                                new CrowdingComparator()));

                //NSGA crossover probability should be less than 1
                double crossoverProbability08 = 0.8;
                Variation singlecross08 = new OnePointCrossover(crossoverProbability08);
                Variation NSGAVariation = new GAVariation(singlecross08, BitFlip);

                Algorithm nsga2 = new NSGAII(problem, ndsPopulation, null, tSelection, NSGAVariation,
                        initialization);

                runSearch(nsga2, properties, path + File.separator + "result", time);

                break;
            case 2: //Use epsilonMOEA
                for(int i = 0; i < 30; i++) {
                    Population population = new Population();
                    EpsilonBoxDominanceArchive archive = new EpsilonBoxDominanceArchive(epsilonDouble);
                    Algorithm eMOEA = new EpsilonMOEA(problem, population, archive, selection, GAVariation, initialization);
                    time = String.valueOf(System.currentTimeMillis());
                    runSearch(eMOEA, properties, path + File.separator + "result", time);
                }
                break;

            case 3://Hyperheuristic search

                for (int i = 0; i < 30; i++) {
                    IRewardDefinition creditAssignment;
                    time = String.valueOf(System.currentTimeMillis());
//                String[] creditDefs = new String[]{"ODP", "OPopPF", "OPopEA", "CPF", "CEA"};
//                String[] creditDefs = new String[]{"OPIR2", "OPopIPFR2", "OPopIEAR2", "CR2PF", "CR2EA"};
                String[] creditDefs = new String[]{"OPopEA"};
                for (String credDef : creditDefs) {

                    try {
                        creditAssignment = RewardDefFactory.getInstance().getCreditDef(credDef, properties, problem);

                        int injectionRate = (int) properties.getDouble("injectionRate", 0.25);
                        //for injection
                        int lagWindow = (int) properties.getDouble("lagWindow", 50);

                        ArrayList<Variation> heuristics = new ArrayList();
                        //add domain-specific heuristics
                        heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability),new AddRandomToSmallSatellite(500), new BitFlip(mutationProbability)));
                        heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability),new RemoveRandomFromLoadedSatellite(1500), new BitFlip(mutationProbability)));
                        heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability),new RemoveSuperfluous(5), new BitFlip(mutationProbability)));
                        heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability),new ImproveOrbit(2), new BitFlip(mutationProbability)));
//                        heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability),new RemoveInterference(5), new BitFlip(mutationProbability)));
                        heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability),new AddSynergy(5), new BitFlip(mutationProbability)));
                        //add domain-independent heuristics
//                        heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new BitFlip(mutationProbability)));

                        properties.setDouble("pmin", 0.03);

                        //all other properties use default parameters
                        INextHeuristic selector = HHFactory.getInstance().getHeuristicSelector("AP", properties, heuristics);

                        Population population = new Population();
                        EpsilonBoxDominanceArchive archive = new EpsilonBoxDominanceArchive(epsilonDouble);
                        HeMOEA hemoea = new HeMOEA(problem, population, archive, selection,
                                initialization, selector, creditAssignment, injectionRate, lagWindow);
                        String fileName = hemoea.getNextHeuristicSupplier() + "_" + hemoea.getCreditDefinition() + "_" + "moreCrossNoInterNoSingle10" + time;
                        String name = path + File.separator + "result" + File.separator;
                        runSearch(hemoea, properties, path + File.separator + "result", fileName);

                        IOCreditHistory ioch = new IOCreditHistory();
                        ioch.saveHistory(hemoea.getCreditHistory(), name + fileName + ".credit", ",");
                        IOSelectionHistory iosh = new IOSelectionHistory();
                        iosh.saveHistory(hemoea.getSelectionHistory(), name + fileName + ".hist", ",");
                    } catch (IOException ex) {
                        Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
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
//            case 10: //use when you have data files you can load (maybe useful if computer restarts mid way through a run)
//                POP_SIZE = 500;
//                MAX_SEARCH_ITS = 2;
//                
//                params = new Params( path, "CRISP-ATTRIBUTES", "test","normal","search_heuristic_rules_smap_2");//FUZZY or CRISP
//                ResultCollection loadedResCol = RM.loadResultCollectionFromFile(Params.path_save_results + "\\2015-01-07_06-14-26_test.rs");
//                ArrayList<EOSSArchitecture> init_popul = loadedResCol.getPopulation();    //ArrayList<Architecture> init_pop = ResultIO.getInstance().loadResultCollectionFromFile(params.initial_pop).getPopulation();
//                for (int i = 0;i<9;i++) {
//                    params = new Params( path, "CRISP-ATTRIBUTES", "test","normal","search_heuristic_rules_smap_2");//FUZZY or CRISP
//                    AE.init(numAE);
//                    AE.evalMinMax();
//                    ATE.setTerm_crit(new SearchOptions(POP_SIZE,MAX_SEARCH_ITS,0.5,0.1,0.5));
//                    ATE.search_NSGA2();
//                    System.out.println("PERF: " + ATE.getSp().toString());
//                    ResultCollection c = new ResultCollection(AE.getResults());//
//                    init_popul = c.getPopulation();
//                    RM.saveResultCollection(c);
//                    ATE.clear();
//                    AE.clear();
//                }
            default:
                System.out.println("Choose a mode between 1 and 9");
        }
    }

    public static Problem initEOSSProblem(String path, String fuzzyMode, String testMode, String normalMode, boolean explanation, int numCPU) {
        EOSSDatabase.getInstance(); //to initiate database
        Params params = new Params(path, fuzzyMode, testMode, normalMode);//FUZZY or CRISP;
        return new EOSSProblem(Params.altnertivesForNumberOfSatellites, EOSSDatabase.getInstruments(), EOSSDatabase.getOrbits(), null, explanation, true);
    }

    public static InstrumentedAlgorithm runSearch(Algorithm alg, TypedProperties properties, String savePath, String name) {
        int populationSize = (int) properties.getDouble("populationSize", 600);
        int maxEvaluations = (int) properties.getDouble("maxEvaluations", 10000);

        Instrumenter instrumenter = new Instrumenter().withFrequency(20)
                .withReferenceSet(new File(savePath + File.separator + "ref.obj"))
                .attachHypervolumeJmetalCollector(new Solution(new double[]{1.0, 2.0}))
                .attachElapsedTimeCollector();

        InstrumentedAlgorithm instAlgorithm = instrumenter.instrument(alg);

        // run the executor using the listener to collect results
        System.out.println("Starting " + alg.getClass().getSimpleName() + " on " + alg.getProblem().getName() + " with pop size: " + populationSize);
        long startTime = System.currentTimeMillis();
        while (!instAlgorithm.isTerminated() && (instAlgorithm.getNumberOfEvaluations() < maxEvaluations)) {
            if (instAlgorithm.getNumberOfEvaluations() % 500 == 0) {
                ((EOSSProblem) instAlgorithm.getProblem()).renewJess();
                System.out.println("NFE: " + instAlgorithm.getNumberOfEvaluations());
                System.out.print("Popsize: " + ((AbstractEvolutionaryAlgorithm) alg).getPopulation().size());
                System.out.println("  Archivesize: " + ((AbstractEvolutionaryAlgorithm) alg).getArchive().size());
            }
            instAlgorithm.step();
        }

        alg.terminate();
        long finishTime = System.currentTimeMillis();
        System.out.println("Done with optimization. Execution time: " + ((finishTime - startTime) / 1000) + "s");

        ResultIO resio = new ResultIO();
        String filename = savePath + File.separator + alg.getClass().getSimpleName() + "_" + name;
        resio.saveSearchMetrics(instAlgorithm, filename);
        resio.savePopulation(((AbstractEvolutionaryAlgorithm) alg).getPopulation(), filename);
        resio.saveObjectives(instAlgorithm.getResult(), filename);
        return instAlgorithm;
    }
}
