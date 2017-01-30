/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import eoss.problem.evaluation.ArchitectureEvaluatorParams;
import aos.aos.AOSEpsilonMOEA;
import aos.aos.AOSFactory;
import aos.creditassigment.CreditDefFactory;
import aos.creditassigment.ICreditAssignment;
import aos.nextoperator.INextOperator;
import aos.operatorselectors.replacement.EpochTrigger;
import aos.operatorselectors.replacement.OperatorReplacementStrategy;
import aos.operatorselectors.replacement.RemoveNLowest;
import eoss.problem.assignment.InstrumentAssignment;
import eoss.problem.assignment.InstrumentAssignment2;
import eoss.problem.assignment.KnowledgeConstraintComparator;
import eoss.problem.evaluation.RequirementMode;
import eoss.problem.scheduling.MissionScheduling;
import java.io.File;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Variation;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.operator.GAVariation;
import org.moeaframework.core.operator.OnePointCrossover;
import org.moeaframework.core.operator.TournamentSelection;
import org.moeaframework.core.operator.binary.BitFlip;
import org.moeaframework.util.TypedProperties;
import eoss.search.InnovizationSearch;
import eoss.search.InstrumentedSearch;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import jess.JessException;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import knowledge.operator.EOSSOperatorCreator;
import knowledge.operator.RepairDataDutyCycle;
import knowledge.operator.RepairInstrumentOrbit;
import knowledge.operator.RepairMass;
import mining.label.AbstractPopulationLabeler;
import mining.label.NondominatedSortingLabeler;
import orekit.util.OrekitConfig;
import org.moeaframework.algorithm.EpsilonMOEA;
import org.moeaframework.core.EpsilonBoxDominanceArchive;
import org.moeaframework.core.Population;
import org.moeaframework.core.comparator.ChainedComparator;
import org.moeaframework.core.comparator.DominanceComparator;
import org.moeaframework.core.comparator.ParetoObjectiveComparator;
import org.moeaframework.core.operator.CompoundVariation;
import org.moeaframework.core.operator.RandomInitialization;
import org.moeaframework.core.operator.integer.IntegerUM;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.xml.sax.SAXException;

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
     * Executor completion services helps remove completed tasks
     */
    private static CompletionService<Algorithm> ecs;

    /**
     * First argument is the path to the project folder. Second argument is the
     * mode. Third argument is the number of ArchitecturalEvaluators to
     * initialize.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        //PATH
        if (args.length == 0) {
            args = new String[4];
//            args[0] = "C:\\Users\\SEAK2\\Nozomi\\EOSS\\problems\\climateCentric";
//            args[0] = "C:\\Users\\SEAK1\\Nozomi\\EOSS\\problems\\climateCentric";
            args[0] = "/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric";
//            args[0] = "/Users/nozomihitomi/Dropbox/EOSS/problems/decadalScheduling";
            args[1] = "1"; //Mode
            args[2] = "1"; //numCPU
            args[3] = "30"; //numRuns
        }

        System.out.println("Path set to " + args[0]);
        System.out.println("Running mode " + args[1]);
        System.out.println("Will get " + args[2] + " resources");
        System.out.println("Will do " + args[3] + " runs");

        String path = args[0];

        int mode = Integer.parseInt(args[1]);
        int numCPU = Integer.parseInt(args[2]);
        int numRuns = Integer.parseInt(args[3]);

        pool = Executors.newFixedThreadPool(numCPU);
        ecs = new ExecutorCompletionService<>(pool);

        //setup for using orekit
        OrekitConfig.init();

        //parameters and operators for search
        TypedProperties properties = new TypedProperties();
        //search paramaters set here
        int popSize = 100;
        int maxEvals = 5000;
        properties.setInt("maxEvaluations", maxEvals);
        properties.setInt("populationSize", popSize);
        double crossoverProbability = 1.0;
        double mutationProbability = 1. / 65.;
        Variation singlecross;
        Variation bitFlip;
        Variation intergerMutation;
        Initialization initialization;

        //setup for epsilon MOEA
        DominanceComparator comparator = new ParetoDominanceComparator();
        double[] epsilonDouble = new double[]{0.001, 0.001};
        final TournamentSelection selection = new TournamentSelection(2, comparator);

        //setup for innovization
        int epochLength = 1000; //for learning rate
        properties.setInt("nOpsToAdd", 4);
        properties.setInt("nOpsToRemove", 4);

        //setup for saving results
        properties.setBoolean("saveQuality", true);
        properties.setBoolean("saveCredits", true);
        properties.setBoolean("saveSelection", true);

        //initialize EOSS database
        EOSSDatabase.getInstance();
        EOSSDatabase.loadBuses(new File(path + File.separator + "config" + File.separator + "candidateBuses.xml"));
        EOSSDatabase.loadInstruments(new File(path + File.separator + "xls" + File.separator + "Instrument Capability Definition.xls"));
        EOSSDatabase.loadOrbits(new File(path + File.separator + "config" + File.separator + "candidateOrbits.xml"));
        EOSSDatabase.loadLaunchVehicles(new File(path + File.separator + "config" + File.separator + "candidateLaunchVehicles.xml"));

        //initialize problem
        Problem problem = null; 

        switch (mode) {
            case 1: //Use epsilonMOEA Assignment
                for (int i = 0; i < numRuns; i++) {
                    singlecross = new OnePointCrossover(crossoverProbability);
                    bitFlip = new BitFlip(mutationProbability);
                    intergerMutation = new IntegerUM(mutationProbability);
                    CompoundVariation var = new CompoundVariation(singlecross, bitFlip, intergerMutation);
//                    CompoundVariation var = new CompoundVariation(singlecross, new RepairMass(3000., 1, 1), bitFlip, intergerMutation);
                    Population population = new Population();
                    EpsilonBoxDominanceArchive archive = new EpsilonBoxDominanceArchive(epsilonDouble);
                    
                    problem = getAssignmentProblem2(path, 5, RequirementMode.FUZZYATTRIBUTE, false);
                    initialization = new RandomInitialization(problem, popSize);
                    ChainedComparator comp = new ChainedComparator(new KnowledgeConstraintComparator(), new ParetoObjectiveComparator());
                    Algorithm eMOEA = new EpsilonMOEA(problem, population, archive, selection, var, initialization,comp);
                    ecs.submit(new InstrumentedSearch(eMOEA, properties, path + File.separator + "result", "emoea" + String.valueOf(i)));
                }
                for (int i = 0; i < numRuns; i++) {
                    try {
                        Algorithm alg = ecs.take().get();
                        ((InstrumentAssignment2) alg.getProblem()).saveSolutionDB(new File(path + File.separator + "database" + File.separator + "emoea" + System.currentTimeMillis() + "solutions.dat"));
                        ((InstrumentAssignment2) alg.getProblem()).clearDB();
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;

            case 2://AOS search Assignment
                try {
                    for (int i = 0; i < numRuns; i++) {
                        problem = getAssignmentProblem2(path, 5, RequirementMode.FUZZYATTRIBUTE, false);
                        ICreditAssignment creditAssignment = CreditDefFactory.getInstance().getCreditDef("SIDo", properties, problem);
                        ArrayList<Variation> heuristics = new ArrayList();

                        //add domain-independent heuristics
                        heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability, 2), new BitFlip(mutationProbability)));
                        heuristics.add(new CompoundVariation(new OnePointCrossover(crossoverProbability, 2), new RepairMass(5000., 1, 1), new BitFlip(mutationProbability)));
                        properties.setDouble("pmin", 0.03);

                        //all other properties use default parameters
                        INextOperator selector = AOSFactory.getInstance().getHeuristicSelector("AP", properties, heuristics);

                        Population population = new Population();
                        EpsilonBoxDominanceArchive archive = new EpsilonBoxDominanceArchive(epsilonDouble);

                        initialization = new RandomInitialization(problem, popSize);

                        AOSEpsilonMOEA hemoea = new AOSEpsilonMOEA(problem, population, archive, selection,
                                initialization, selector, creditAssignment);

                        ecs.submit(new InstrumentedSearch(hemoea, properties, path + File.separator + "result", hemoea.getName() + String.valueOf(i)));
                    }
                } catch (IOException ex) {
                    Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
                }

                for (int i = 0; i < numRuns; i++) {
                    try {
                        AOSEpsilonMOEA hemoea = (AOSEpsilonMOEA) ecs.take().get();
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;
            case 3://innovization search Assignment
                problem = getAssignmentProblem2(path, 5, RequirementMode.FUZZYATTRIBUTE, false);
                String innovizeAssignment = "AIAA_innovize_" + System.nanoTime();
                for (int i = 0; i < numRuns; i++) {
                    try {
                        ICreditAssignment creditAssignment = CreditDefFactory.getInstance().getCreditDef("SIDo", properties, problem);

                        ArrayList<Variation> operators = new ArrayList();

                        //add domain-independent heuristics
                        Variation SingleCross = new CompoundVariation(new OnePointCrossover(crossoverProbability, 2), new BitFlip(mutationProbability));
                        operators.add(SingleCross);

                        //set up OperatorReplacementStrategy
                        EpochTrigger epochTrigger = new EpochTrigger(epochLength);
                        EOSSOperatorCreator eossOpCreator = new EOSSOperatorCreator(crossoverProbability, mutationProbability);
                        ArrayList<Variation> permanentOps = new ArrayList();
                        permanentOps.add(SingleCross);
                        RemoveNLowest operatorRemover = new RemoveNLowest(permanentOps, properties.getInt("nOpsToRemove", 2));
                        OperatorReplacementStrategy ops = new OperatorReplacementStrategy(epochTrigger, operatorRemover, eossOpCreator);

                        properties.setDouble("pmin", 0.03);

                        //all other properties use default parameters
                        INextOperator selector = AOSFactory.getInstance().getHeuristicSelector("AP", properties, operators);

                        Population population = new Population();
                        EpsilonBoxDominanceArchive archive = new EpsilonBoxDominanceArchive(epsilonDouble);

                        initialization = new RandomInitialization(problem, popSize);

                        AOSEpsilonMOEA hemoea = new AOSEpsilonMOEA(problem, population, archive, selection,
                                initialization, selector, creditAssignment);

                        AbstractPopulationLabeler labeler = new NondominatedSortingLabeler(.25);
                        ecs.submit(new InnovizationSearch(hemoea, properties, labeler, ops, path + File.separator + "result", innovizeAssignment + i));
                    } catch (IOException ex) {
                        Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                for (int i = 0; i < numRuns; i++) {
                    try {
                        AOSEpsilonMOEA hemoea = (AOSEpsilonMOEA) ecs.take().get();
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;

            case 4: { //Use epsilonMOEA Scheduling
                problem = getSchedulingProblem(path, RequirementMode.FUZZYATTRIBUTE);
                for (int i = 0; i < numRuns; i++) {
                    singlecross = new OnePointCrossover(crossoverProbability);
                    bitFlip = new BitFlip(mutationProbability);
                    GAVariation gaVariation = new GAVariation(singlecross, bitFlip);
                    Population population = new Population();
                    EpsilonBoxDominanceArchive archive = new EpsilonBoxDominanceArchive(epsilonDouble);

                    initialization = new RandomInitialization(problem, popSize);
                    Algorithm eMOEA = new EpsilonMOEA(problem, population, archive, selection, gaVariation, initialization);
                    ecs.submit(new InstrumentedSearch(eMOEA, properties, path + File.separator + "sched_result", String.valueOf(i)));
                }
                 for (int i = 0; i < numRuns; i++) {
                    try {
                        ecs.take().get();
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;
            }

            default:
                throw new IllegalArgumentException(String.format("%d is an invalid option", mode));
        }
        pool.shutdown();
    }

    public static InstrumentAssignment getAssignmentProblem(String path, RequirementMode mode, boolean explanation) {
        return new InstrumentAssignment(path, mode, ArchitectureEvaluatorParams.altnertivesForNumberOfSatellites, explanation, true, new File(path + File.separator + "database" + File.separator + "solutions.dat"));
    }

    public static InstrumentAssignment2 getAssignmentProblem2(String path, int nSpacecraft, RequirementMode mode, boolean explanation) {
        return new InstrumentAssignment2(path, nSpacecraft, mode, explanation, true, new File(path + File.separator + "database" + File.separator + "solutions.dat"));
    }

    public static MissionScheduling getSchedulingProblem(String path, RequirementMode mode) {
        try {
            TimeScale utc = TimeScalesFactory.getUTC();
            return new MissionScheduling(path, mode,
                    new AbsoluteDate(2010, 1, 1, utc),
                    new AbsoluteDate(2050, 1, 1, utc), 1.);
        } catch (OrekitException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BiffException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JessException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static void convertXlsToMap() {
        HashMap<HashMap<Orbit, Double>, HashMap<String, Double>> newDb = new HashMap();
        try {
            Workbook xls = Workbook.getWorkbook(new File("/Users/nozomihitomi/Dropbox/EOSS/problems/decadalScheduling/xls/Mission Analysis Database.xls"));
            Sheet sheet = xls.getSheet("Walker");
            int nlines = sheet.getRows();
            for (int i = 1; i < nlines; i++) {
                HashMap<Orbit, Double> orbMap = new HashMap();
                Cell[] row = sheet.getRow(i);
                if (Integer.parseInt(row[0].getContents()) == 1) {
                    double sa = Double.parseDouble(row[2].getContents()) * 1000. + 6378100.0;
                    String inclination = row[2].getContents();
                    Orbit.OrbitType type;
                    if (inclination.equals("12345")) {
                        inclination = "SSO";
                        type = Orbit.OrbitType.SSO;
                    } else {
                        type = Orbit.OrbitType.LEO;
                    }
                    Orbit orb = new Orbit(String.valueOf(i), type, sa, inclination, "N/A", 0, 0, 0);
                    double fov = Double.parseDouble(row[4].getContents());
                    orbMap.put(orb, fov);
                }
                HashMap<String, Double> revTimes = new HashMap();
                revTimes.put("avg_revisit_time", Double.parseDouble(row[5].getContents()));
                revTimes.put("avg_revisit_time_tropics", Double.parseDouble(row[6].getContents()));
                revTimes.put("avg_revisit_time_NH", Double.parseDouble(row[7].getContents()));
                revTimes.put("avg_revisit_time_SH", Double.parseDouble(row[8].getContents()));
                revTimes.put("avg_revisit_time_cold regions", Double.parseDouble(row[9].getContents()));
                revTimes.put("avg_revisit_time_US", Double.parseDouble(row[10].getContents()));
                newDb.put(orbMap, revTimes);
            }

            try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("newRevtimes"));) {
                os.writeObject(newDb);
                os.close();
            } catch (IOException ex) {
                Logger.getLogger(ArchitectureEvaluatorParams.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BiffException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
