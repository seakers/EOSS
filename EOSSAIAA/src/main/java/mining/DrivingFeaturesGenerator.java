package mining;


/*

This class reads in result file in csv format, and minds driving features

Member functions
    computeMetrics: computes metrics used to evaluate ARM (e.g. support, lift, confidence)
    getDrivingFeatures: mines driving features and returns an ArrayList<DrivingFeature>
    exportDrivingFeatures: writes a csv file with compact representation of driving features
    sortDrivingFeatures: sorts driving features based on different ARM measures
    checkThreshold: checks if the ARM measures are above threshold
    parseCSV: reads in a result file in csv format
    bitString2intArr: Modifies bitString to integer array
    booleanToInt: Modifies boolean array to integer array


 */



import eoss.problem.EOSSDatabase;
import java.util.ArrayList;

import mining.label.Clustering;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

/**
 *
 * @author Bang
 */
public class DrivingFeaturesGenerator {
    
    private final int numberOfVariables;

    private double supp_threshold;
    private double confidence_threshold;
    private double lift_threshold;
    
    private ArrayList<DrivingFeature> presetDrivingFeatures;
    
    private ArrayList<int[][]> behavioral;
    private ArrayList<int[][]> non_behavioral;
    
    private ArrayList<Arch> architectures;
    
    private int max_num_of_instruments;
    public boolean tallMatrix;
    public int maxLength;
    public boolean run_mRMR;


    public DrivingFeaturesGenerator(int numberOfVariables){
        this.numberOfVariables = numberOfVariables;
        
        this.supp_threshold=DrivingFeaturesParams.support_threshold;
        this.confidence_threshold=DrivingFeaturesParams.confidence_threshold;
        this.lift_threshold=DrivingFeaturesParams.lift_threshold;
        this.presetDrivingFeatures = new ArrayList<>();
        
        this.tallMatrix=DrivingFeaturesParams.tallMatrix;
        this.maxLength = DrivingFeaturesParams.maxLength;
        this.run_mRMR = DrivingFeaturesParams.run_mRMR;
        
        this.behavioral = new ArrayList<>();
        this.non_behavioral = new ArrayList<>();
        
        this.architectures = new ArrayList<>();

    }

    public ArrayList<SetOfFeatures> getDrivingFeatures(String labeledDataFile, String saveDataFile, FeatureMetric metric, int topN){
        
    	parseCSV(labeledDataFile);
        
//    	System.out.println("...Extracting level 1 driving features and sort by support values");
    	ArrayList<DrivingFeature> preset = sort_preset(0,getDrivingFeatures_preset());

//    	System.out.println("...Starting Apriori");
    	double[][] dataFeatureMat = this.getDataFeatureMat_double();
    	double[] thresholds = {this.supp_threshold,this.lift_threshold,this.confidence_threshold};

    	Apriori ap = new Apriori(preset, dataFeatureMat, thresholds);
    	ArrayList<SetOfFeatures> features = ap.runApriori(maxLength,run_mRMR, topN);

        Collections.sort(features, new FeatureComparator(metric));
    	exportDrivingFeatures(features, saveDataFile, topN);
    	return features;
    }
    
    public void getDrivingFeatures_clustering(String labeledDataFile, String saveDataFile, FeatureMetric metric, int numCluster){
    	
    	parseCSV_clustering(labeledDataFile);
    	clusterData(numCluster);
    	
    	try{
	        PrintWriter w = new PrintWriter(saveDataFile,"UTF-8");
	        w.println("// (mode,arg,orb,inst)/support/lift");
	    	
	    	for(int c=0;c<numCluster;c++){
	    		
	    		behavioral = new ArrayList<>();
	    		non_behavioral = new ArrayList<>();

	    		for(Arch arch:architectures){
	    			if(arch.getClassID()==c){
	    				behavioral.add(arch.getBitString());
	    			}else{
	    				non_behavioral.add(arch.getBitString());
	    			}
	    		}	
//	        	System.out.println("...Extracting level 1 driving features for cluster " + c);
//	        	System.out.println("...behavioral: " + behavioral.size() +" non_behavioral: " + non_behavioral.size());

	    		this.supp_threshold = (double) DrivingFeaturesParams.support_threshold_clustering * behavioral.size() / (behavioral.size()+non_behavioral.size());
	    		this.confidence_threshold = DrivingFeaturesParams.confidence_threshold_clustering;

	    		ArrayList<DrivingFeature> preset;
                        int cnt=0;
	    		while(true){
	    			preset=getDrivingFeatures_preset();
	    			if(preset.size() > 170){
	    				this.confidence_threshold = this.confidence_threshold + 0.05;
	    			}else if(preset.size() < 30){
	    				this.confidence_threshold = this.confidence_threshold - 0.03;
	    			}else{
	    				break;
	    			}
                                if(cnt > 50){
                                    break;
                                }
                                cnt++;
	    		}
                        
                        // Sort by support in decreasing order
	        	preset = sort_preset(0,preset);
	        	double[][] dataFeatureMat = this.getDataFeatureMat_double();
	        	double[] thresholds = {this.supp_threshold,this.lift_threshold,this.confidence_threshold};
	
	        	Apriori ap = new Apriori(preset, dataFeatureMat, thresholds);
	        	ArrayList<SetOfFeatures> features = ap.runApriori(maxLength,run_mRMR, 1);
                        Collections.sort(features, new FeatureComparator(metric));
	        	exportDrivingFeatures_clustering(w, features);
	    	}
	    	
	        w.flush();
	        w.close();
        
    	}catch(Exception e){
    		e.printStackTrace();
    	}

    }

    private double[] computeMetrics(Scheme s){
    	
    	double cnt_all= (double) non_behavioral.size() + behavioral.size();
        double cnt_F=0.0;
        double cnt_S= (double) behavioral.size();
        double cnt_SF=0.0;
        
        for (int[][] e : behavioral) {
            if (s.compare(e) == 1) {
            	cnt_SF = cnt_SF+1.0;
            	cnt_F = cnt_F + 1.0;
            }
        }
        for (int[][] e : non_behavioral) {
            if (s.compare(e) == 1) cnt_F = cnt_F+1.0;
        }

        
        double cnt_NS = cnt_all-cnt_S;
        double cnt_NF = cnt_all-cnt_F;
        double cnt_S_NF = cnt_S-cnt_SF;
        double cnt_F_NS = cnt_F-cnt_SF;
        
    	double[] metrics = new double[4];
    	
        double support = cnt_SF/cnt_all;
        double support_F = cnt_F/cnt_all;
        double support_S = cnt_S/cnt_all;
        double lift = (cnt_SF/cnt_S) / (cnt_F/cnt_all);
        double conf_given_F = (cnt_SF)/(cnt_F);   // confidence (feature -> selection)
        double conf_given_S = (cnt_SF)/(cnt_S);   // confidence (selection -> feature)


    	metrics[0] = support;
    	metrics[1] = lift;
    	metrics[2] = conf_given_F;
    	metrics[3] = conf_given_S;
    	
    	return metrics;
    }
    
    
    public ArrayList<DrivingFeature> getDrivingFeatures_preset(){

        Scheme scheme = new Scheme();
        int ind=0;
        ArrayList<DrivingFeature> presetFeatures = new ArrayList<>();

        scheme.setName("present");
        for (int i = 0; i < EOSSDatabase.getNumberOfInstruments(); ++i) {
            scheme.setInstrument (i);
            double[] metrics = computeMetrics(scheme);
            if (checkThreshold(metrics)) {
                int[] param = new int[1];
                param[0] = i;
                String featureName = "present[" + EOSSDatabase.getInstrument(i) + "]";
                presetFeatures.add(new DrivingFeature(ind,featureName,"present", param, metrics));
                ind++;
            }
        }
        scheme.resetArg();
        scheme.setName("absent");
        for (int i = 0; i < EOSSDatabase.getNumberOfInstruments(); ++i) {
            scheme.setInstrument (i);
            double[] metrics = computeMetrics(scheme);
            if (checkThreshold(metrics)) {
                int [] param = new int[1];
                param[0] = i;
                String featureName = "absent[" + EOSSDatabase.getInstrument(i) + "]";
                presetFeatures.add(new DrivingFeature(ind,featureName,"absent", param, metrics));
                ind++;
            }
        }
        scheme.resetArg();
        scheme.setName("inOrbit");
        for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); ++i) {
            for (int j = 0; j < EOSSDatabase.getNumberOfInstruments(); ++j) {
                scheme.setInstrument (j);
                scheme.setOrbit(i);
                double[] metrics = computeMetrics(scheme);
                if (checkThreshold(metrics)) {
                    int[] param = new int[2];
                    param[0] = i;
                    param[1] = j;
                    String featureName = "inOrbit[" + EOSSDatabase.getOrbit(i) + ", " + EOSSDatabase.getInstrument(j) + "]";
                    presetFeatures.add(new DrivingFeature(ind,featureName,"inOrbit", param, metrics));
                    ind++;
                }
            }
        }
        scheme.resetArg();
        scheme.setName("notInOrbit");
        for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); ++i) {
            for (int j = 0; j < EOSSDatabase.getNumberOfInstruments(); ++j) {
                scheme.setInstrument (j);
                scheme.setOrbit(i);
                double[] metrics = computeMetrics(scheme);
                if (checkThreshold(metrics)) {
                    int[] param = new int[2];
                    param[0] = i;
                    param[1] = j;
                    String featureName = "notInOrbit[" + EOSSDatabase.getOrbit(i) + ", " + EOSSDatabase.getInstrument(j) + "]";
                    presetFeatures.add(new DrivingFeature(ind,featureName,"notInOrbit", param, metrics));
                    ind++;
                } 
            }
        }
        scheme.resetArg();
        scheme.setName("together2");
        for (int i = 0; i < EOSSDatabase.getNumberOfInstruments(); ++i) {
            for (int j = 0; j < i; ++j) {
                scheme.setInstrument(i);
                scheme.setInstrument2(j);
                double[] metrics = computeMetrics(scheme);
                if (checkThreshold(metrics)) {
                    int[] param = new int[2];
                    param[0] = i;
                    param[1] = j;
                    String featureName = "together2[" + EOSSDatabase.getInstrument(i) + ", " + EOSSDatabase.getInstrument(j) + "]";
                    presetFeatures.add(new DrivingFeature(ind,featureName,"together2", param, metrics));
                    ind++;
                }
            }
        }    
        scheme.resetArg();
        scheme.setName("togetherInOrbit2");
        for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); ++i) {
            for (int j = 0; j < EOSSDatabase.getNumberOfInstruments(); ++j) {
                for (int k = 0; k < j; ++k) {
                    scheme.setInstrument(j);
                    scheme.setInstrument2(k);
                    scheme.setOrbit(i);
                    double[] metrics = computeMetrics(scheme);
                    if (checkThreshold(metrics)) {
                        int[] param = new int[3];
                        param[0] = i;
                        param[1] = j;
                        param[2] = k;
                        String featureName = "togetherInOrbit2[" + EOSSDatabase.getOrbit(i) + ", " + EOSSDatabase.getInstrument(j) + 
                                ", " + EOSSDatabase.getInstrument(k) + "]"; 
                        presetFeatures.add(new DrivingFeature(ind,featureName,"togetherInOrbit2", param,metrics));
                        ind++;
                    }
                }
            }
        }
        scheme.resetArg();
        scheme.setName("separate2");
        for (int i = 0; i < EOSSDatabase.getNumberOfInstruments(); ++i) {
            for (int j = 0; j < i; ++j) {
                scheme.setInstrument(i);
                scheme.setInstrument2(j);
                double[] metrics = computeMetrics(scheme);
                if (checkThreshold(metrics)) {
                        int[] param = new int[2];
                        param[0] = i;
                        param[1] = j;
                        String featureName = "separate2[" + EOSSDatabase.getInstrument(i) + ", " + EOSSDatabase.getInstrument(j) + "]";
                        presetFeatures.add(new DrivingFeature(ind,featureName,"separate2", param, metrics));
                        ind++;
                    }
            }            
        }
        scheme.resetArg();
        scheme.setName("together3");
        for (int i = 0; i < EOSSDatabase.getNumberOfInstruments(); ++i) {
            for (int j = 0; j < i; ++j) {
                for (int k = 0; k < j; ++k) {
                    scheme.setInstrument(i);
                    scheme.setInstrument2(j);
                    scheme.setInstrument3(k);
                    double[] metrics = computeMetrics(scheme);
                    if (checkThreshold(metrics)) {
                        int[] param = new int[3];
                        param[0] = i;
                        param[1] = j;
                        param[2] = k;
                        String featureName = "together3[" + EOSSDatabase.getInstrument(i) + ", " + 
                        		EOSSDatabase.getInstrument(j) + ", " + EOSSDatabase.getInstrument(k) + "]";
                        presetFeatures.add(new DrivingFeature(ind,featureName,"together3", param, metrics));
                        ind++;
                    }
                }
            }            
        }
        scheme.resetArg();
        scheme.setName("togetherInOrbit3");
        for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); ++i) {
            for (int j = 0; j < EOSSDatabase.getNumberOfInstruments(); ++j) {
                for (int k = 0; k < j; ++k) {
                    for (int l = 0; l < k; ++l) {
                        scheme.setName("togetherInOrbit3");
                        scheme.setInstrument(j);
                        scheme.setInstrument2(k);
                        scheme.setInstrument3(l);
                        scheme.setOrbit(i);
                        double[] metrics = computeMetrics(scheme);
                        if (checkThreshold(metrics)) {
                            int[] param = new int[4];
                            param[0] = i;
                            param[1] = j;
                            param[2] = k;
                            param[3] = l;
                            String featureName = "togetherInOrbit3[" + EOSSDatabase.getOrbit(i) + ", " + 
                            		EOSSDatabase.getInstrument(j) + ", " + EOSSDatabase.getInstrument(k) + "," + EOSSDatabase.getInstrument(l) + "]";
                            presetFeatures.add(new DrivingFeature(ind,featureName,"togetherInOrbit3", param, metrics));
                            ind++;
                        }
                    }
                }
            }
        }
        scheme.resetArg();
        scheme.setName("separate3");
        for (int i = 0; i < EOSSDatabase.getNumberOfInstruments(); ++i) {
            for (int j = 0; j < i; ++j) {
                for (int k = 0; k < j; ++k) {
                    scheme.setInstrument(i);
                    scheme.setInstrument2(j);
                    scheme.setInstrument3(k);
                    double[] metrics = computeMetrics(scheme);
                    if (checkThreshold(metrics)) {
                        int[] param = new int[3];
                        param[0] = i;
                        param[1] = j;
                        param[2] = k;
                        String featureName = "separate3[" + EOSSDatabase.getInstrument(i) + ", " + 
                        		EOSSDatabase.getInstrument(j) + ", " + EOSSDatabase.getInstrument(k) + "]";
                        presetFeatures.add(new DrivingFeature(ind,featureName,"separate3", param, metrics));
                        ind++;
                    }
                }
            }
        }
        scheme.resetArg();
        scheme.setName("emptyOrbit");
        for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); ++i) {
            scheme.setOrbit(i);
            double[] metrics = computeMetrics(scheme);
            if (checkThreshold(metrics)) {
                int[] param = new int[1];
                param[0] = i;
                String featureName = "emptyOrbit[" + EOSSDatabase.getOrbit(i) + "]";
                presetFeatures.add(new DrivingFeature(ind,featureName,"emptyOrbit", param, metrics));
                ind++;
            }
        }
        scheme.resetArg();
        scheme.setName("numOrbitUsed");
        for (int i = 1; i < EOSSDatabase.getNumberOfOrbits()+1; i++) {
            scheme.setCount(i);
            double[] metrics = computeMetrics(scheme);
            if (checkThreshold(metrics)) {
                int[] param = new int[1];
                param[0] = i;
                String featureName = "numOrbitUsed[" + param[0] + "]";
                presetFeatures.add(new DrivingFeature(ind,featureName,"numOrbitUsed", param, metrics));
                ind++;
            }
        }
        scheme.resetArg();
        scheme.setName("numInstruments"); 
        // Total number of instruments
        for (int i = 1; i < max_num_of_instruments; i++) {
            scheme.setCount(i);
            double[] metrics = computeMetrics(scheme);
            if (checkThreshold(metrics)) {
                int[] param = new int[1];
                param[0] = i;
                String featureName = "numInstruments[" + i + "]";
                presetFeatures.add(new DrivingFeature(ind,featureName,"numInstruments", param, metrics));
                ind++;
            }
        }
        scheme.resetArg();
        scheme.setName("numInstruments"); 
        // Number of each instrument
        for (int i=0;i<EOSSDatabase.getNumberOfInstruments();i++){
        	for (int j=1;j<max_num_of_instruments;j++){
        		scheme.setInstrument(i);
	            scheme.setCount(j);
	            double[] metrics = computeMetrics(scheme);
	            if (checkThreshold(metrics)) {
	                int[] param = new int[2];
	                param[0] = i;
	                param[1] = j;
	                String featureName = "numInstruments[" + EOSSDatabase.getInstrument(i) + ","+ j +"]";
	                presetFeatures.add(new DrivingFeature(ind,featureName,"numInstruments", param, metrics));
	                ind++;
	            }
        	}
        }

        presetDrivingFeatures = presetFeatures;
        return presetFeatures;
    }
    

    
    public void RecordSingleFeature(PrintWriter w, DrivingFeature df){
        String type = df.getType();
        int[] param = df.getParam();
        int i = param[0];
        int j = -1; int k=-1; int l=-1;
        if(param.length>1){
                j=param[1];
        }
        if(param.length>2){
                k=param[2];
        }
        if(param.length>3){
                l=param[3];
        }

        switch(type) {
        case "present":
                w.print("(0,1,*,"+i+")"); break;
        case "absent":
                w.print("(0,0,A,"+i+")");break;
        case "inOrbit":
                w.print("(0,1,"+i+","+j+")");break;
        case "notInOrbit":
                w.print("(0,0,"+i+","+j+")");break;
        case "together2":
                w.print("(0,1,*,"+i+","+j+")");break;
        case "togetherInOrbit2":
                w.print("(0,1,"+i+","+j+","+k+")"); break;
        case "separate2":
                w.print("(0,0,A,"+i+","+j+")");break;
        case "together3":
                w.print("(0,1,*,"+i+","+j+","+k+")");break;
        case "togetherInOrbit3":
                w.print("(0,1,"+i+","+j+","+k+","+l+")"); break;
        case "separate3":
                w.print("(0,0,A,"+i+","+j+","+k+")");break;
        case "emptyOrbit":
                w.print("(0,0,"+i+",A)");break;
        case "numOrbitUsed":
                w.print("(1,"+i+",*,*)");break;
        case "numInstruments":
                if(j==-1){
                        w.print("(2,"+i+",*,*)");break;
                }else{
                        w.print("(2,"+j+",*,"+i+")");break;
                }
        }

    }
    
    public void recordMetaInfo(PrintWriter w, SetOfFeatures branch){
    	
    	double[] metrics;
    	String name="";

        metrics = branch.getMetrics();
   
    	for(int dfIndex:branch.getIndices()){
    		DrivingFeature df = presetDrivingFeatures.get(dfIndex);
    		if(name.isEmpty()){
    			name = df.getName();
    		}else{
    			name = name + " , " + df.getName();
    		}
    	}
    	w.print("/" + metrics[0] + "/" + metrics[1] + "// " + name + "\n");
    }
    /**
     * Saves all of the driving features in an ordered list based on (0:
     * support, 1: lift, 2: confidence)
     *
     * @param features the features to be exported
     * @param filename path and filename to save features
     */
    public boolean exportDrivingFeatures(ArrayList<SetOfFeatures> features, String filename) {
        return exportDrivingFeatures(features,filename, presetDrivingFeatures.size());
    }

    /**
     * Saves the topN driving features in an ordered list based on (0: support,
     * 1: lift, 2: confidence)
     *
     * @param features the features to be exported
     * @param filename path and filename to save features
     * @param topN only save the top N features
     */
    public boolean exportDrivingFeatures(ArrayList<SetOfFeatures> features,String filename, int topN){
        try{

            PrintWriter w = new PrintWriter(filename,"UTF-8");
            w.println("// (mode,arg,orb,inst)/support/lift");

            int count = 0;

            for(SetOfFeatures branch:features){
                    if (count > topN) {
                        break;
                    }

                    for(int dfIndex:branch.getIndices()){
                            DrivingFeature df = presetDrivingFeatures.get(dfIndex);
                            this.RecordSingleFeature(w, df);
                    }
                    this.recordMetaInfo(w, branch);
                    count++;
            }

            w.flush();
            w.close();

        }catch(Exception e){
                e.printStackTrace();
                return false;
        }
        return true;
    }
    
    public boolean exportDrivingFeatures_clustering(PrintWriter w, ArrayList<SetOfFeatures> features){
        try{
            for(SetOfFeatures branch:features){
                for(int dfIndex:branch.getIndices()){
                    DrivingFeature df = this.presetDrivingFeatures.get(dfIndex);
                    this.RecordSingleFeature(w, df);
                }
                this.recordMetaInfo(w, branch);
            }
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
    

    public ArrayList<DrivingFeature> sort_preset(int metric, ArrayList<DrivingFeature> inputList){
    	
    	ArrayList<DrivingFeature> sortedList = new ArrayList<>();
    	int length = inputList.size();
        for(int i=0;i<length;i++){
                DrivingFeature df = inputList.get(i);
                double val = df.getMetrics()[metric];
                if (sortedList.isEmpty()){
                        sortedList.add(df);
                }else if (val >= sortedList.get(0).getMetrics()[metric]){
                        // If val is smaller than the first element, insert it at as the first element
                        sortedList.add(0, df);
                }else if(val < sortedList.get(sortedList.size()-1).getMetrics()[metric]){
                        // If val is larger than the last element, add it to the last
                        sortedList.add(df);
                }else{
                        // Insert df based on the metric value
                        for(int j=0;j<sortedList.size();j++){
                                if(val < sortedList.get(j).getMetrics()[metric] && 
                                val >= sortedList.get(j+1).getMetrics()[metric] ){
                                        sortedList.add(j+1, df);
                                }
                        }
                }
        }
        for(int i=0;i<length;i++){
                sortedList.get(i).setIndex(i);
        }
        this.presetDrivingFeatures = sortedList;
        return sortedList;
    }
    
    public double[][] getDataFeatureMat_double(){
    	int[][] dataMat = getDataFeatureMat();
    	int nrows = dataMat.length;
    	int ncols = dataMat[0].length;
    	double[][] newDataMat = new double[nrows][ncols];
    	for(int i=0;i<nrows;i++){
    		for(int j=0;j<ncols;j++){
    			newDataMat[i][j] = (double) dataMat[i][j];
    		}
    	}
    	return newDataMat;
    }

    public int[][] getDataFeatureMat(){
        
        int numData = behavioral.size() + non_behavioral.size();
        int numFeature = presetDrivingFeatures.size() + 1; // add class label as a last feature
        int[][] dataMat = new int[numData][numFeature];
        
        for(int i=0;i<numData;i++){
        	
        	int[][] d;
        	boolean classLabel;
        	
        	if(i < behavioral.size()){
        		d = behavioral.get(i);
        		classLabel = true;
        	}else{
        		d = non_behavioral.get(i-behavioral.size());
        		classLabel = false;
        	}

            Scheme s = new Scheme();

            for(int j=0;j<numFeature-1;j++){
                DrivingFeature f = presetDrivingFeatures.get(j);
                String type = f.getType();
                int[] param_ = f.getParam();
                ArrayList<String> param = new ArrayList<>();
                for(int par:param_){
                	param.add(""+par);
                }
//                param.addAll(Arrays.asList(param_));
                if(s.presetFilter(type, d, param)){
                    dataMat[i][j]=1;
                } else{
                    dataMat[i][j]=0;
                }
            }

            if(classLabel){
                dataMat[i][numFeature-1]=1;
            } else{
                dataMat[i][numFeature-1]=0;
            }
        }

        return dataMat;
    }


    public boolean checkThreshold(double[] metrics){
    	if (metrics[0] >= supp_threshold && 
//    			metrics[1]>= lift_threshold && 
    			metrics[2] >= confidence_threshold){
    		return true;
    	}
    	else{
    		return false;
    	}
    }

    public void parseCSV(String path){
        String line = "";
        String splitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
        	//skip header
        	line = br.readLine();
        	
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] tmp = line.split(splitBy);
                int label = Integer.parseInt(tmp[0]);
                StringBuilder sb = new StringBuilder();
                //skiip the first variable since it is the number of sats per orbit
                for(int i=1; i < numberOfVariables; i++){
                    sb.append(tmp[i+1]);
                }
                if (label==1){
                	this.behavioral.add(bitString2intArr(sb.toString()));
                }else{
                	this.non_behavioral.add(bitString2intArr(sb.toString()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    public void parseCSV_clustering(String path){
        String csvFile = path;
        String line = "";
        String splitBy = ",";
        
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            //skip header
            line = br.readLine();
        	
            int id=0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] tmp = line.split(splitBy);
                int label = Integer.parseInt(tmp[0]);
                String bitString = tmp[2];
                double[] obj = new double[2];
                obj[0] = Double.parseDouble(tmp[3]);
                obj[1] = Double.parseDouble(tmp[4]);
                
                this.architectures.add(new Arch(id,label,bitString,obj));
                id++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int[][] bitString2intArr(String input){
    	int[][] output = new int[EOSSDatabase.getNumberOfOrbits()][EOSSDatabase.getNumberOfInstruments()];
    	
    	int cnt=0;
    	if (DrivingFeaturesParams.tallMatrix){
        	for(int i=0;i<EOSSDatabase.getNumberOfInstruments();i++){
        		for (int o=0;o<EOSSDatabase.getNumberOfOrbits();o++){
        			int thisBit;
        			if(cnt==input.length()-1){
        				thisBit = Integer.parseInt(input.substring(cnt));
        			}else{
        				thisBit = Integer.parseInt(input.substring(cnt,cnt+1));
        			}
        			output[o][i] = thisBit;
        			cnt++;
        		}
        	}
    	}else{
        	for(int i=0;i<EOSSDatabase.getNumberOfOrbits();i++){
        		for (int j=0;j<EOSSDatabase.getNumberOfInstruments();j++){
        			int thisBit;
        			if(cnt==input.length()-1){
        				thisBit = Integer.parseInt(input.substring(cnt));
        			}else{
        				thisBit = Integer.parseInt(input.substring(cnt,cnt+1));
        			}
        			output[i][j] = thisBit;
        			cnt++;
        		}
        	}
        	cnt++;
    	}
    	return output;
    }

    public int[][] booleanToInt(boolean[][] b) {
        int[][] intVector = new int[b.length][b[0].length]; 
        for(int i = 0; i < b.length; i++){
            for(int j = 0; j < b[0].length; ++j) intVector[i][j] = b[i][j] ? 1 : 0;
        }
        return intVector;
    }
    
    
    public void clusterData(int numCluster){
    	
    	Clustering clustering = new Clustering(2);
    	for(Arch a:this.architectures){
    		// If the architecture is behavioral
    		if(a.getLabel()){
    			clustering.addInstance(a.getID(), a.getObjectives());
    		}
    	}
    	// Perform K-means clustering with 200 iterations
    	ArrayList<int[]> cl_result = clustering.parseClusterData(clustering.KMeansClustering(numCluster, 200));
    	
    	numCluster = cl_result.size();
    	
    	for(Arch ar:this.architectures){
    		boolean contains = false;
    		for(int i=0;i<numCluster;i++){
    			
    			int[] indices_thisCl = cl_result.get(i);
    			
    			for(int ind:indices_thisCl){
    				if(ind==ar.getID()){
    					contains=true;
    					break;
    				}
    			}
    			
    			if(contains){
    				ar.setClassID(i);
    				break;
    			}
    		}
    	}
    	
    }

    
    public class Arch{
    	
    	int id;
    	int classID;
    	double[] objectives;
    	int[][] bitString;
    	boolean label;
    	
    	public Arch(int id, int label, String bitString, double[] objectives){
    		this.id=id;
    		this.label = label==1;
    		this.bitString = bitString2intArr(bitString);
    		this.objectives = objectives;
    		this.classID=-1;
    	}

    	public void setClassID(int classID){
    		this.classID = classID;
    	}
    	public int getID(){
    		return id;
    	}
    	public boolean getLabel(){
    		return label;
    	}
    	public int[][] getBitString(){
    		return bitString;
    	}
    	public double[] getObjectives(){
    		return objectives;
    	}
    	public int getClassID(){
    		return classID;
    	}
    	
    }
    
}
