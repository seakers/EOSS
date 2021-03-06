///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package rbsa.eoss;
//
//
//import rbsa.problem.ArchitectureGenerator;
//import rbsa.problem.EOSSArchitecture;
//import rbsa.eoss.local.Params;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Future;
//import java.util.Stack;
//import org.paukov.combinatorics.*;
//import java.util.Iterator;
//import jess.*;
//import java.util.Arrays;
//import java.util.Collections;
//
///**
// *
// * @author Marc
// */
//public class ArchitectureEvaluator {
//    
//    private static ArchitectureEvaluator instance = null;
//    private ArrayList<EOSSArchitecture> population;
//    private ResourcePool rp;
//    private Resource searchRes;
//    //private ThreadPoolExecutor tpe;
//    private ExecutorService tpe;
//
//    private int numCPU;
//    //private int population_size;
//    private Stack<Result> results;
//    private ArrayList<Future<Result>> futures;
//    private HashMap<String,ArrayList<Fact>> capabilities;
//    private HashMap<String,NDSM> dsm_map;
//    private HashMap<ArrayList<String>, HashMap<String,Double>> scores;
//    private HashMap<ArrayList<String>, HashMap<String,ArrayList<Double>>> subobj_scores;
//    
//    private ArchitectureEvaluator () {
//        results = new Stack<Result>();
//        searchRes = null;
//        futures = new ArrayList<Future<Result>>();
//        population = null;
//        rp = null;
//        tpe = null;
//        numCPU = 0;
//        capabilities = null;
//       // population_size = 0;
//        dsm_map = new HashMap<String,NDSM>();
//        scores = new HashMap<ArrayList<String>, HashMap<String,Double>>();
//        subobj_scores = new  HashMap<ArrayList<String>, HashMap<String,ArrayList<Double>>>();
//    }
//    public ExecutorService getTpe() {
//        return tpe;
//    
//    }
//    public NDSM getDSM(String desc, String orb) {
//        String key = desc + "@" + orb;
//        return dsm_map.get(key);
//    }
//    public HashMap<String,Double> getAllOrbitScores(ArrayList<String> instruments) {
//        Collections.sort(instruments);
//        return scores.get(instruments);
//    }
//    public HashMap<String,ArrayList<Double>> getAllOrbitSubobjScores(ArrayList<String> instruments) {
//        Collections.sort(instruments);
//        HashMap<String,ArrayList<Double>> ret = subobj_scores.get(instruments);
//        if(ret == null) {
//            System.out.println("instruments " + instruments);
//        }
//        return ret;
//    }
//    public HashMap<ArrayList<String>, HashMap<String,Double>> getScores() {
//        return scores;
//    }
//    public Double getScore(ArrayList<String> instruments,String orbit) {
//        return getAllOrbitScores(instruments).get(orbit);
//    }
//    public void setScore(ArrayList<String> instruments,String orbit,Double score) {
//        Collections.sort(instruments);
//        HashMap<String,Double> hmap =  getAllOrbitScores(instruments);
//        if(hmap == null) {
//            hmap = new HashMap<String,Double>();
//            scores.put(instruments, hmap);
//        }
//        
//        if(hmap.get(orbit) == null)
//            hmap.put(orbit,score);
//    }
//    public ArrayList<Double> getSubobjScores(ArrayList<String> instruments,String orbit) {
//        Collections.sort(instruments);
//        ArrayList<Double> ret;
//        ret = getAllOrbitSubobjScores(instruments).get(orbit);
//        if(ret == null) {
//            System.out.println(instruments.toString() + " " + orbit);
//        }
//        return ret;
//    }
//    public void setSubobjScores(ArrayList<String> instruments,String orbit,ArrayList<Double> scores) {
//        Collections.sort(instruments);
//        HashMap<String,ArrayList<Double>> hmap =  getAllOrbitSubobjScores(instruments);
//        if(hmap == null) {
//            hmap = new HashMap<String,ArrayList<Double>>();
//            subobj_scores.put(instruments, hmap);
//        }
//    
//        if(hmap.get(orbit) == null)
//            hmap.put(orbit,scores);
//    }
//    public void recomputeScores(int dim) {
//            
//        ICombinatoricsVector<String> originalVector = Factory.createVector(Params.instrument_list);
//        Generator<String> gen = Factory.createSimpleCombinationGenerator(originalVector, dim);
//        Iterator it = gen.iterator();
//        for (int i = 0;i<gen.getNumberOfGeneratedObjects();i++) {
//            ICombinatoricsVector<String> combination = ( ICombinatoricsVector<String>)it.next();
//            for (int o = 0;o<Params.norb;o++) {
//                String orbit = Params.orbit_list[o];
//                GenericTask t = new GenericTask( new Architecture(combination, orbit),"Slow");
//                futures.add(tpe.submit(t));
//            }
//        }
//        
//        for (Future<Result> future:futures){
//            try {
//                Result resu = future.get(); //Do something with the results..
//                ArchitectureEvaluator.getInstance().pushResult(resu);
//                // Add DSM
//                Architecture arch = resu.getArch();
//                String[] payloads = arch.getPayloads();
//                String orbit = arch.getOrbit();
//                
//                Double score = resu.getScience();
//                ArrayList<Double> the_subobj_scores = resu.getSubobjective_scores();
//                
//                ArrayList<String> als = new ArrayList<String>();
//                als.addAll(Arrays.asList(payloads));
//                setScore(als,orbit,score);
//                setSubobjScores(als,orbit,the_subobj_scores);
//                
//            }catch(Exception e) {
//                System.out.println(e.getMessage());
//            }
//        }
//    }
//    private ArrayList<String> combin2ArrayList(ICombinatoricsVector<String> combin) {
//        int n = combin.getSize();
//        ArrayList<String> thepayloads = new ArrayList<String>();
//       for (int i = 0;i<n;i++) {
//            thepayloads.add(combin.getValue(i));
//        }
//       return thepayloads;
//    }
//            
//    public void recomputeNDSM(int dim) {
//        HashMap<String,Double[]> no_syn_scores = new HashMap<String,Double[]>();
//        HashMap<String,Double[]> syn_scores = new HashMap<String,Double[]>();
//        ICombinatoricsVector<String> originalVector = Factory.createVector(Params.instrument_list);
//        Generator<String> gen = Factory.createSimpleCombinationGenerator(originalVector, dim-1);
//        
//        /*for (ICombinatoricsVector<String> combination : gen) {
//            System.out.println(combination);
//         }*/
//        for (int o = 0;o<Params.nOrbits;o++) {
//            String orbit = Params.orbit_list[o];
//            String key_r = "RDSM" + dim + "@" + orbit;
//            Iterator it = gen.iterator();
//            for (int i = 0;i<gen.getNumberOfGeneratedObjects();i++) {
//                
//                ICombinatoricsVector<String> combination = ( ICombinatoricsVector<String>)it.next();
//                for (int j = 0;j<Params.nInstruments;j++) {
//                    //compute score with synergies
//                    
//                    if(combination.contains(Params.instrument_list[j]))  {
//                        continue;
//                    }
//                    //CombinatoricsVector<String> comb = new CombinatoricsVector<String>(combination);
//                    //comb.addValue(Params.instrument_list[j]);
//                    Nto1pair nto10 = new Nto1pair(combination,Params.instrument_list[j]);
//                    GenericTask t = new GenericTask( new EOSSArchitecture(nto10, orbit),"Fast");
//                    futures.add(tpe.submit(t));
//
//                    //compute score without synergies
//                    GenericTask t2 = new GenericTask( new EOSSArchitecture(nto10, orbit) , "NoSynergies");
//                    futures.add(tpe.submit(t2));
//                    ArrayList<String> als = new ArrayList<String>();
//                    als.addAll(Arrays.asList(nto10.getBase()));
//                    Collections.sort(als);
//                    ArrayList<String> als2 = new ArrayList<String>();
//                    als2.addAll(nto10.toArrayList());
//                    Collections.sort(als2);
//                    ArrayList<Double> subobj_scores0 = getSubobjScores(als,orbit);
//                    ArrayList<Double> subobj_scores1 = getSubobjScores(als2,orbit);
//                    double red = 0.0;
//                    for (int k = 0;k<subobj_scores0.size();k++)
//                        red -= (subobj_scores1.get(k) - subobj_scores0.get(k));
//                    NDSM rdsm = dsm_map.get(key_r);
//                    if (rdsm == null) {
//                        rdsm = new NDSM(Params.instrument_list,key_r);
//                        dsm_map.put(key_r,rdsm);
//                    }
//                    rdsm.setInteraction(nto10.getBase(),nto10.getAdded(), red);
//                }
//            }
//        }
//        for (Future<Result> future:futures){
//            try {
//                Result resu = future.get(); //Do something with the results..
//                ArchitectureEvaluator.getInstance().pushResult(resu);
//                // Add DSM
//                EOSSArchitecture arch = resu.getArch();
//                String payload = arch.getPayload();
//                String[] payloads = arch.getPayloads();
//                String[] base = arch.getNto1pair().getBase();
//                String add = arch.getNto1pair().getAdded();
//                String orbit = arch.getOrbit();
//                String key = payload + "@"  + orbit;
//                String type = resu.getTaskType();
//                if(type.equalsIgnoreCase("Fast")) {
//                    Double[] dd = new Double[2];
//                    dd[0] = resu.getScience();
//                    dd[1] = resu.getCost();
//                    syn_scores.put(key, dd);
//                } else if (type.equalsIgnoreCase("NoSynergies")) {
//                    Double[] dd = new Double[2];
//                    dd[0] = resu.getScience();
//                    dd[1] = resu.getCost();
//                    no_syn_scores.put(key, dd);
//                }
//                if (syn_scores.get(key)!=null & no_syn_scores.get(key)!=null) {
//                    String key_s = "SDSM" + dim + "@" + orbit;
//                    String key_e = "EDSM" + dim + "@" + orbit;
//                    NDSM sdsm = dsm_map.get(key_s);
//                    if (sdsm == null) {
//                        sdsm = new NDSM(Params.instrument_list,key_s);
//                        dsm_map.put(key_s,sdsm);
//                    }
//
//                    NDSM edsm = dsm_map.get(key_e);
//                    if (edsm == null) {
//                        edsm = new NDSM(Params.instrument_list,key_e);
//                        dsm_map.put(key_e,edsm);
//                    }
//                    double score_syn = syn_scores.get(key)[0];
//                    double cost_syn = syn_scores.get(key)[1]; 
//                    double score_nosyn = no_syn_scores.get(key)[0];
//                    double cost_nosyn = no_syn_scores.get(key)[1];
//                    Double synergy = new Double(score_syn-score_nosyn);
//                    Double interference = new Double(cost_syn-cost_nosyn);
//                    System.out.println(payload + ": synergy = " + synergy + "; interf" + interference);
//                    sdsm.setInteraction(base,add, synergy);
//                    edsm.setInteraction(base,add, interference);
//                }
//                
//            }catch(Exception e) {
//                System.out.println(e.getMessage());
//            }
//        }
//    } 
//    public HashMap<String, NDSM> getDsm_map() {
//        return dsm_map;
//    }
//
//    public void setDsm_map(HashMap<String, NDSM> dsm_map) {
//        this.dsm_map = dsm_map;
//    }
//    
//    public HashMap<String, ArrayList<Fact>> getCapabilities() {
//        return capabilities;
//    }
//
//    public void setCapabilities(HashMap<String, ArrayList<Fact>> capabilities) {
//        this.capabilities = capabilities;
//    }
//    
//    public static ArchitectureEvaluator getInstance()
//    {
//        if( instance == null ) 
//        {
//            instance = new ArchitectureEvaluator();
//        }
//        return instance;
//    }
//    
//    public void init(int n) {
//        numCPU = n;
//        rp = new ResourcePool( numCPU );
//        searchRes = new Resource();
//        tpe = Executors.newFixedThreadPool(numCPU);
//        if (!Params.run_mode.equalsIgnoreCase("update_dsms")) {
//            setDsm_map(Params.all_dsms);
//        }
//        if (!Params.run_mode.equalsIgnoreCase("update_capabilities")) {
//            setCapabilities(Params.capabilities);
//        }
//        if (!Params.run_mode.equalsIgnoreCase("update_scores")) {
//            setScores(Params.scores);
//            setSubobj_scores(Params.subobj_scores);
//        }
//        results.clear();
//        futures.clear();
//    }
//    
//    public void setScores (HashMap<ArrayList<String>, HashMap<String,Double>> scores) {
//        this.scores = scores;
//    }
//    public void clearResults()
//    {
//        //rp = new ResourcePool( numCPU );
//        //ArrayBlockingQueue<Runnable> taskList = new ArrayBlockingQueue( population_size );
//        //tpe = new ThreadPoolExecutor( numCPU, numCPU, 0, TimeUnit.MILLISECONDS, taskList );
//        //tpe = Executors.newFixedThreadPool(numCPU);
//        results.clear();
//        futures.clear();
//    }
//    
//    public void precomputeCapabilities() {
//        capabilities = new HashMap<String,ArrayList<Fact>>();
//        population = ArchitectureGenerator.getInstance().generatePrecomputedPopulation();
//       for( int i = 0; i < population.size(); i++ ) {
//            GenericTask t = new GenericTask( population.get(i),"Capabilities" );
//            //tpe.execute(t);
//            futures.add(tpe.submit(t));
//            
//        }
//       int it = 0;
//        for (Future<Result> future:futures){
//            try {
//                Result resu = future.get(); //Do something with the results..
//                String key = resu.getArch().getKey();
//                ArrayList<Fact> capas = resu.getCapabilities();
//                capabilities.put(key, capas);
//                ArchitectureEvaluator.getInstance().pushResult(resu);
//                System.out.println(it++);
//                // Add a quality check to see if science < 1 and arch is not empty. Push only if it passes quality control
//            }catch(Exception e) {
//                System.out.println(e.getMessage());
//            }
//        }
//        
//    }
//            
//    public void clear() {
//        results = new Stack<Result>();
//        searchRes = null;
//        futures = new ArrayList<Future<Result>>();
//        population = null;
//        rp = null;
//        tpe = null;
//        numCPU = 0;
//        capabilities = null;
//       // population_size = 0;
//        dsm_map = new HashMap<String,NDSM>();
//        scores = new HashMap<ArrayList<String>, HashMap<String,Double>>();
//    }        
//     
//        
//    public ResourcePool getResourcePool()
//    {
//        return rp;
//    }
//
//    public Stack<Result> getResults() {
//        return results;
//    }
//
//    public void setResults(Stack<Result> results) {
//        this.results = results;
//    }
//    public synchronized void pushResult(Result result) {
//        this.results.push(result);
//    }
//
//    
//    public Resource getSearchResource() {
//        return searchRes;
//    }
//    
//    public void freeSearchResource() {
//        try {
//            searchRes.getRete().eval("(reset)");
//        }catch(Exception e) {System.out.println("HOLA");}
//    }
//}
