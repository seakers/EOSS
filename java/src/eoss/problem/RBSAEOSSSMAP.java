/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import eoss.problem.Params;
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
//import rbsa.eoss.ArchitectureEvaluator;
import architecture.ArchitectureGenerator;
import eoss.problem.EOSSDatabase;
import eoss.problem.EOSSProblem;
import architecture.ResultIO;
/**
 *
 * @author dani
 */
public class RBSAEOSSSMAP {

    /**
     * First argument is the path to the project folder. Second argument is the mode. Third argument is the number of ArchitecturalEvaluators to initialize.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        //PATH
        args = new String[3];
        args[0] = "/Users/nozomihitomi/Dropbox/EOSS";
//          args[0] = "C:\\Users\\SEAK1\\Dropbox\\EOSS";
        args[1] = "3";
        args[2] = "3";
        
        System.out.println("Path set to " + args[0]);
        System.out.println("Running mode " + args[1]);
        System.out.println("Will get " + args[2] + " resources");
        
        String path = args[0];
        
        int MODE = Integer.parseInt(args[1]);
        int numCPU = Integer.parseInt(args[2]);        
//        ArchitectureEvaluator AE = ArchitectureEvaluator.getInstance();
        ResultIO resio = new ResultIO();
        Problem prob = initEOSSProblem(path, "CRISP-ATTRIBUTES", "test","normal","fast",true, numCPU);
        switch(MODE) {

            case 3://Search
                TypedProperties prop = new TypedProperties();
                //search paramaters set here
                int maxEvaluations = 50000;
                int popSize = 2;
                double crossoverProbability = 0.8;
                double mutationProbability = 0.01;

                //setup NSGAII
		Initialization initialization = new ArchitectureGenerator(prob, popSize, "random");

		NondominatedSortingPopulation population = new NondominatedSortingPopulation();

		TournamentSelection selection = new TournamentSelection(2,
                        new ChainedComparator(
						new ParetoDominanceComparator(),
						new CrowdingComparator()));

		Variation singlecross = new OnePointCrossover(crossoverProbability);
                Variation BitFlip = new BitFlip(mutationProbability);
                Variation GAVariation =  new GAVariation(singlecross, BitFlip);
                

		Algorithm alg = new NSGAII(prob, population, null, selection, GAVariation,
				initialization);
                
                Instrumenter instrumenter = new Instrumenter().withFrequency(popSize)
                        .attachHypervolumeJmetalCollector()
                        .attachElapsedTimeCollector();

                InstrumentedAlgorithm instAlgorithm = instrumenter.instrument(alg);

                // run the executor using the listener to collect results
                System.out.println("Starting " + alg.getClass().getSimpleName() + " on " + prob.getName() + " with pop size: " + popSize);
                long startTime = System.currentTimeMillis();
                while (!instAlgorithm.isTerminated() && (instAlgorithm.getNumberOfEvaluations() < maxEvaluations)) {
                    instAlgorithm.step();
                }

                alg.terminate();
                long finishTime = System.currentTimeMillis();
                System.out.println("Done with optimization. Execution time: " + ((finishTime - startTime) / 1000) + "s");
                
                String filename = path + File.separator + "result";
                resio.saveMetrics(instAlgorithm, filename);
                resio.savePopulation(instAlgorithm.getResult(), filename);
                
                System.out.println("DONE");
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
//            case 6://Update capabilities file
//                AE.init(numAE);
//                AE.precomputeCapabilities();                
//                try{
//                    SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss" );
//                    String stamp = dateFormat.format( new Date() );
//                    FileOutputStream fos = new FileOutputStream(Params.path_save_results + "\\capabilities" + stamp + ".dat");
//                    ObjectOutputStream oos = new ObjectOutputStream(fos);
//                    oos.writeObject(AE.getCapabilities());
//                    oos.close();
//                    fos.close();
//                } catch(Exception e) {
//                    System.out.println(e.getMessage());
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
    
    public static Problem initEOSSProblem(String path,String fuzzyMode, String testMode, String normalMode, String evalMode, boolean explanation, int numCPU){
        EOSSDatabase.getInstance(); //to initiate database
        Params params = new Params(path, fuzzyMode, testMode,normalMode);//FUZZY or CRISP;
        return new EOSSProblem(Params.altnertivesForNumberOfSatellites, EOSSDatabase.getInstruments(), EOSSDatabase.getOrbits(), null, evalMode, explanation);
    }
}
