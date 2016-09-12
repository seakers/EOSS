/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import java.io.File;
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
import eoss.problem.operators.Knowledge1;
import eoss.problem.operators.Knowledge2;
import eoss.problem.operators.Knowledge3;
import eoss.problem.operators.Knowledge4;
import eoss.problem.operators.KnowledgeARMGood;
import eoss.problem.operators.KnowledgeARMPoor;
import eoss.problem.operators.KnowledgeHumanGood;
import eoss.problem.operators.KnowledgeHumanPoor;
import eoss.problem.operators.RemoveRandomFromLoadedSatellite;
import eoss.problem.operators.RemoveSuperfluous;
import hh.IO.IOCreditHistory;
import hh.IO.IOSelectionHistory;
import hh.hyperheuristics.HHFactory;
import hh.hyperheuristics.HeMOEA;
import hh.hyperheuristics.IHyperHeuristic;
import hh.nextheuristic.INextHeuristic;
import hh.rewarddefinition.IRewardDefinition;
import hh.rewarddefinition.RewardDefFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.moeaframework.algorithm.EpsilonMOEA;
import org.moeaframework.core.EpsilonBoxDominanceArchive;
import org.moeaframework.core.Population;
import org.moeaframework.core.comparator.DominanceComparator;
import org.moeaframework.core.operator.CompoundVariation;

/**
 *
 * @author dani
 */
public class RBSAEOSSSMAP {

    /**
     * pool of resources
     */
    private static ExecutorService pool;

    /**
     * List of future tasks to perform
     */
    private static ArrayList<Future<Algorithm>> futures;

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
        if (args.length == 0) {
            args = new String[4];
//            args[0] = "C:\\Users\\SEAK2\\Nozomi\\EOSS\\problems\\climateCentric";
            args[0] = "C:\\Users\\SEAK1\\Dropbox\\EOSS\\problems\\climateCentric";
//            args[0] = "/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric";
            args[1] = "3"; //Mode
            args[2] = "1"; //numCPU
            args[3] = "1"; //numRuns
        }

        System.out.println("Path set to " + args[0]);
        System.out.println("Running mode " + args[1]);
        System.out.println("Will get " + args[2] + " resources");
        System.out.println("Will do " + args[3] + " runs");

        String path = args[0];

        int MODE = Integer.parseInt(args[1]);
        int numCPU = Integer.parseInt(args[2]);
        int numRuns = Integer.parseInt(args[3]);

        pool = Executors.newFixedThreadPool(numCPU);
        futures = new ArrayList<>(numRuns);

        //parameters and operators for search
        TypedProperties properties = new TypedProperties();
        //search paramaters set here
        int popSize = 100;
        properties.setInt("maxEvaluations", 2500);
        properties.setInt("populationSize", popSize);
        double crossoverProbability = 1.0;
        double mutationProbability = 0.01;
        Variation singlecross;
        Variation BitFlip;
        Variation GAVariation;
        Initialization initialization;
        Problem problem;

        //setup for epsilon MOEA
        DominanceComparator comparator = new ParetoDominanceComparator();
        double[] epsilonDouble = new double[]{0.001, 0.001};
        final TournamentSelection selection = new TournamentSelection(2, comparator);
        
        initEOSSProblem(path, "FUZZY-ATTRIBUTES", "test", "normal");

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

                singlecross = new OnePointCrossover(crossoverProbability);
                BitFlip = new BitFlip(mutationProbability);
                GAVariation = new GAVariation(singlecross, BitFlip);
                Variation NSGAVariation = new GAVariation(singlecross08, BitFlip);
                

                problem = getEOSSProblem(false);

                initialization = new ArchitectureGenerator(problem, popSize, "random");
                Algorithm nsga2 = new NSGAII(problem, ndsPopulation, null, tSelection, NSGAVariation,
                        initialization);

                break;
            case 2: //Use epsilonMOEA
                for (int i = 0; i < numRuns; i++) {

                    singlecross = new OnePointCrossover(crossoverProbability);
                    BitFlip = new BitFlip(mutationProbability);
                    GAVariation = new GAVariation(singlecross, BitFlip);
                    Population population = new Population();
                    EpsilonBoxDominanceArchive archive = new EpsilonBoxDominanceArchive(epsilonDouble);
                    
                    problem = getEOSSProblem(false);
                    initialization = new ArchitectureGenerator(problem, popSize, "random");
                    Algorithm eMOEA = new EpsilonMOEA(problem, population, archive, selection, GAVariation, initialization);
                    time = String.valueOf(System.currentTimeMillis() + (long) i);
                    InstrumentedSearch run = new InstrumentedSearch(eMOEA, properties, path + File.separator + "result", time);
                    futures.add(pool.submit(run));
                }
                for (Future<Algorithm> run : futures) {
                    try {
                        run.get();
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;

            case 3://Hyperheuristic search
                String origname = "";
                for (int i = 0; i < numRuns; i++) {
                    IRewardDefinition creditAssignment;
                    time = String.valueOf(System.currentTimeMillis() + (long) i);
//                String[] creditDefs = new String[]{"ODP", "OPopPF", "OPopEA", "CPF", "CEA"};
//                String[] creditDefs = new String[]{"OPIR2", "OPopIPFR2", "OPopIEAR2", "CR2PF", "CR2EA"};
                    String[] creditDefs = new String[]{"OPopEA"};
                    for (String credDef : creditDefs) {

                        try {
                            problem = getEOSSProblem(false);
                            
                            creditAssignment = RewardDefFactory.getInstance().getCreditDef(credDef, properties, problem);

                            int injectionRate = (int) properties.getDouble("injectionRate", 0.25);
                            //for injection
                            int lagWindow = (int) properties.getDouble("lagWindow", 50);

                            ArrayList<Variation> heuristics = new ArrayList();
                            //add domain-specific heuristics
//                            heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new AddRandomToSmallSatellite(500), new BitFlip(mutationProbability)));
//                            heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new RemoveRandomFromLoadedSatellite(1500), new BitFlip(mutationProbability)));
//                            heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new RemoveSuperfluous(5), new BitFlip(mutationProbability)));
//                            heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new ImproveOrbit(2), new BitFlip(mutationProbability)));
//                        heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability),new RemoveInterference(5), new BitFlip(mutationProbability)));
//                            heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new AddSynergy(5), new BitFlip(mutationProbability)));
                            
//                            heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new Knowledge1(), new BitFlip(mutationProbability)) );
//                            heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new Knowledge2(), new BitFlip(mutationProbability)) );
//                            heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new Knowledge3(), new BitFlip(mutationProbability)) );
//                            heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new KnowledgeARMGood(), new BitFlip(mutationProbability)) );
//                            heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new KnowledgeARMPoor(), new BitFlip(mutationProbability)) );
                            
                            heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new KnowledgeHumanGood(), new BitFlip(mutationProbability)) );
                            heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new KnowledgeHumanPoor(), new BitFlip(mutationProbability)) );
                            
                            //add domain-independent heuristics
                            heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new BitFlip(mutationProbability)));

                            properties.setDouble("pmin", 0.03);

                            //all other properties use default parameters
                            INextHeuristic selector = HHFactory.getInstance().getHeuristicSelector("AP", properties, heuristics);

                            Population population = new Population();
                            EpsilonBoxDominanceArchive archive = new EpsilonBoxDominanceArchive(epsilonDouble);
                            
//                            initialization = new ArchitectureGenerator(problem, popSize, "random");
                            ResultIO resio = new ResultIO();
                            origname = "HeMOEA_AdaptivePursuit_SI-A_1ptC+BitM1464548365178";
                            population = resio.loadPopulation(path+"/result/CESUN/"+ origname + ".pop");
                            initialization = new ArchitectureGenerator(problem, 0, "random");
                            HeMOEA hemoea = new HeMOEA(problem, population, archive, selection,
                                    initialization, selector, creditAssignment, injectionRate, lagWindow);
                            String fileName = hemoea.getNextHeuristicSupplier() + "_" + hemoea.getCreditDefinition() + "_" + "noKnow" + time;

                            InstrumentedSearch run = new InstrumentedSearch(hemoea, properties, path + File.separator + "result", origname + "_ARC");
                            futures.add(pool.submit(run));
                        } catch (IOException ex) {
                            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                for (Future<Algorithm> run : futures) {
                    try {
                        HeMOEA hemoea = (HeMOEA)run.get();
                        IOCreditHistory ioch = new IOCreditHistory();
                        ioch.saveHistory(hemoea.getCreditHistory(), path + File.separator+ origname+ "ARC.credit", ",");
//                        IOSelectionHistory iosh = new IOSelectionHistory();
//                        iosh.saveHistory(hemoea.getSelectionHistory(), name + fileName + ".hist", ",");
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
                pool.shutdown();
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

    public static void initEOSSProblem(String path, String fuzzyMode, String testMode, String normalMode) {
        EOSSDatabase.getInstance(); //to initiate database
        new Params(path, fuzzyMode, testMode, normalMode);//FUZZY or CRISP;
    }
    
     public static Problem getEOSSProblem(boolean explanation) {
        return new EOSSProblem(Params.altnertivesForNumberOfSatellites, EOSSDatabase.getInstruments(), EOSSDatabase.getOrbits(), null, explanation, true);
    }

}
