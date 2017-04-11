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



import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Math;

import org.jblas.DoubleMatrix;

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

    private double supp_threshold;
    private double conf_threshold;
    private double lift_threshold;
    private String[] instrument_list;
    private String[] orbit_list;
    private int ninstr;
    private int norb;
    
    private ArrayList<Integer> behavioral;
    private ArrayList<Integer> non_behavioral;
    private ArrayList<Integer> population;
    
    private ArrayList<Architecture> architectures;
    private ArrayList<DrivingFeature> presetDrivingFeatures;
    private ArrayList<int[]> presetDrivingFeatures_satList;
    private ArrayList<DrivingFeature> drivingFeatures;

    double [][] dataFeatureMat;
    double[] labels;
    
    double [] thresholds;
    
    private int maxIter;
    private int minRuleNum;
    private int maxRuleNum;
    private double adaptSupp;
    

    public boolean tallMatrix;
    public int maxLength;
    public boolean run_mRMR;
    
    private FilterExpressionHandler feh;
    
    
    
    
    public DrivingFeaturesGenerator(){
        
    	this.orbit_list = DrivingFeaturesParams.orbit_list;
    	this.instrument_list = DrivingFeaturesParams.instrument_list;
    	this.norb = orbit_list.length;
    	this.ninstr = instrument_list.length;
        this.supp_threshold=DrivingFeaturesParams.support_threshold;
        this.conf_threshold=DrivingFeaturesParams.confidence_threshold;
        this.lift_threshold=DrivingFeaturesParams.lift_threshold;
        
    	this.thresholds = new double[3];
    	thresholds[0] = supp_threshold;
    	thresholds[1] = lift_threshold;
    	thresholds[2] = conf_threshold;
        
        
        this.feh = new FilterExpressionHandler();
        this.architectures = new ArrayList<>();
        
        this.behavioral = new ArrayList<>();
        this.non_behavioral = new ArrayList<>();
        this.presetDrivingFeatures = new ArrayList<>();
        
        
        this.maxIter = DrivingFeaturesParams.maxIter;
        this.minRuleNum = DrivingFeaturesParams.minRuleNum;
        this.maxRuleNum = DrivingFeaturesParams.maxRuleNum;

        this.tallMatrix=DrivingFeaturesParams.tallMatrix;
        this.maxLength = DrivingFeaturesParams.maxLength;
        this.run_mRMR = DrivingFeaturesParams.run_mRMR;

    }
    

    public void getDrivingFeatures(String labeledDataFile, String saveDataFile, int sort_index, int topN){
        
        // Read-in a csv file with labeled data
    	parseCSV(labeledDataFile);
        
//    	System.out.println("...Extracting level 1 driving features and sort by support values");
    	getPresetDrivingFeatures();

//    	System.out.println("...Starting Apriori");
        getDrivingFeatures();

        // Sort driving features
        Collections.sort(this.drivingFeatures, DrivingFeature.DrivingFeatureComparator);
        
        MRMR mRMR = new MRMR();
        this.drivingFeatures = mRMR.minRedundancyMaxRelevance(getDataMat(this.drivingFeatures), new DoubleMatrix(this.labels), this.drivingFeatures, topN);
        
        // Printout result
        exportDrivingFeatures(saveDataFile,topN);
        
    }
    



    
    
    
    
    
    
    
    
    public ArrayList<DrivingFeature> getPresetDrivingFeatures(){

        this.presetDrivingFeatures = new ArrayList<>();
        this.presetDrivingFeatures_satList = new ArrayList<>();        

        ArrayList<String> candidate_features = new ArrayList<>();
    	
        // Types
        // present, absent, inOrbit, notInOrbit, together2, 
        // separate2, separate3, together3, emptyOrbit
        // numOrbits, numOfInstruments, subsetOfInstruments
        
        // Preset filter expression example:
        // {presetName[orbits;instruments;numbers]}    
                    
        for(int i=0;i<ninstr;i++){
            // present, absent
            candidate_features.add("{present[;"+i+";]}");
            candidate_features.add("{absent[;"+i+";]}");
            
            for(int j=0;j<norb+1;j++){
                // numOfInstruments (number of specified instruments across all orbits)
                candidate_features.add("{numOfInstruments[;"+i+";"+j+"]}");
            }                
            
            for(int j=0;j<i;j++){
                // together2, separate2
                candidate_features.add("{together[;"+i+","+j+";]}");
                candidate_features.add("{separate[;"+i+","+j+";]}");
                for(int k=0;k<j;k++){
                    // together3, separate3
                    candidate_features.add("{together[;"+i+","+j+","+k+";]}");
                    candidate_features.add("{separate[;"+i+","+j+","+k+";]}");
                }
            }
        }
        for(int i=0;i<norb;i++){
            for(int j=1;j<9;j++){
                // numOfInstruments (number of instruments in a given orbit)
                candidate_features.add("{numOfInstruments["+i+";;"+j+"]}");
            }
            // emptyOrbit
            candidate_features.add("{emptyOrbit["+i+";;]}");
            // numOrbits
            int numOrbitsTemp = i+1;
            candidate_features.add("{numOrbits[;;"+numOrbitsTemp+"]}");
            for(int j=0;j<ninstr;j++){
                // inOrbit, notInOrbit
                candidate_features.add("{inOrbit["+i+";"+j+";]}");
                candidate_features.add("{notInOrbit["+i+";"+j+";]}");
                for(int k=0;k<j;k++){
                    // togetherInOrbit2
                    candidate_features.add("{inOrbit["+i+";"+j+","+k+";]}");
                    for(int l=0;l<k;l++){
                        // togetherInOrbit3
                        candidate_features.add("{inOrbit["+i+";"+j+","+k+","+l+";]}");
                    }
                }
            }
        }
        for(int i=0;i<16;i++){
            // numOfInstruments (across all orbits)
            candidate_features.add("{numOfInstruments[;;"+i+"]}");
        }
        
        
        
        try{
        
            ArrayList<String> featureData_name = new ArrayList<>();
            ArrayList<String> featureData_exp = new ArrayList<>();
            ArrayList<double[]> featureData_metrics = new ArrayList<>();
            ArrayList<int[]> featureData_satList = new ArrayList<>();

            for(String feature:candidate_features){ 
                String feature_expression_inside = feature.substring(1,feature.length()-1);
                String name = feature_expression_inside.split("\\[")[0];
                double[] metrics = feh.processSingleFilterExpression_computeMetrics(feature_expression_inside);
                featureData_satList.add(feh.getSatisfactionArray());
                featureData_name.add(name);
                featureData_exp.add(feature);
                featureData_metrics.add(metrics);
            }

            int iter=0;
            ArrayList<Integer> addedFeatureIndices = new ArrayList<>();
            double[] bounds = new double[2];
            bounds[0] = 0;
            bounds[1] = (double) behavioral.size() / population.size();
            
            boolean apriori = true;
            if(apriori){
                while(addedFeatureIndices.size() < minRuleNum || addedFeatureIndices.size() > maxRuleNum){

                    iter++;
                    if(iter > maxIter){
                        break;
                    }else if(iter > 1){
                        // max supp threshold is support_S
                        // min supp threshold is 0
                        double a;
                                if(addedFeatureIndices.size() > maxRuleNum){ // Too many rules -> increase threshold
                                        bounds[0] = this.adaptSupp;
                                        a = bounds[1];
                                }else{ // too few rules -> decrease threshold
                                        bounds[1] = this.adaptSupp;
                                        a = bounds[0];
                                }
                        // Bisection
                        this.adaptSupp = (double) (this.adaptSupp + a) * 0.5;	
                    }
                    addedFeatureIndices = new ArrayList<>();
                        for(int i=0;i<featureData_name.size();i++){
                        double[] metrics = featureData_metrics.get(i);
                        if(metrics[0]>adaptSupp){
                                addedFeatureIndices.add(i);
                            if(addedFeatureIndices.size() > this.maxRuleNum && iter < maxIter){
                                break;
                            }else if(( candidate_features.size() - (i+1) ) + addedFeatureIndices.size() < this.minRuleNum){
                                break;
                            }
                        }
                    }        	
                    System.out.println("RuleSetSize: " + addedFeatureIndices.size() +" Treshold: "+ this.adaptSupp);
                }		
                System.out.println("Driving features extracted in "+ iter +" steps with size: " + addedFeatureIndices.size());
            }else{
                for(int i=0;i<featureData_name.size();i++){
                    double[] metrics = featureData_metrics.get(i);
                    if(metrics[0]>thresholds[0]&&metrics[1]>thresholds[1]&&metrics[2]>thresholds[2]&&metrics[3]>thresholds[2]){
                        addedFeatureIndices.add(i);
                    }
                }
            }

            int id=0;
            for(int i:addedFeatureIndices){
                this.presetDrivingFeatures.add(new DrivingFeature(id,featureData_name.get(i), featureData_exp.get(i), featureData_metrics.get(i)));
                presetDrivingFeatures_satList.add(featureData_satList.get(i));
                id++;
            }


            //if(apriori) return getDrivingFeatures();
            return this.presetDrivingFeatures;


        }catch(Exception e){
        	e.printStackTrace();
        	return new ArrayList<>();
        }
        
        
    }
    

   public void setDrivingFeatureSatisfactionData(){
	   
       // Get feature satisfaction matrix
       this.dataFeatureMat = new double[population.size()][presetDrivingFeatures.size()];
       this.labels = new double[population.size()];
       
       for(int i=0;i<population.size();i++){
            for(int j=0;j<presetDrivingFeatures.size();j++){

                DrivingFeature df = presetDrivingFeatures.get(j);
                int index = df.getID();
                this.dataFeatureMat[i][j] = (double) presetDrivingFeatures_satList.get(index)[i];
            }

            if(behavioral.contains(population.get(i))){
                    labels[i]=1;
            }else{
                    labels[i]=0;
            }
       	
       }         
   }
   

    
    public ArrayList<DrivingFeature> getDrivingFeatures(){

    	this.setDrivingFeatureSatisfactionData();
    	
    	//System.out.println("higher level feature extraced");
    	ArrayList<DrivingFeature> dfs=new ArrayList<>();

        // Create a new instance of Apriori
        Apriori ap = new Apriori(this.presetDrivingFeatures, this.dataFeatureMat, labels, thresholds);
        
        // Run Apriori algorithm
        ArrayList<Apriori.Feature> new_features = ap.runApriori(this.maxLength,this.run_mRMR,100);

        // Create a new list of driving features (assign new IDs)
        int id=0;
        for(int f=0;f<new_features.size();f++){
            
            Apriori.Feature feat = new_features.get(f);
            String expression="";
            String name="";
            ArrayList<Integer> featureIndices = feat.getElements();

            boolean first = true;
            for(int index:featureIndices){
                if(first){
                    first = false;
                }
                else{
                    expression = expression + "&&";
                    name = name + "&&";
                }
                DrivingFeature thisDF = this.presetDrivingFeatures.get(index);
                expression = expression + thisDF.getExpression();
                name = name + thisDF.getName();
            }
            double[] metrics = feat.getMetrics();
            DrivingFeature df = new DrivingFeature(id,name,expression, metrics);
            df.setDatArray(feat.getDatArray());
            id++;
            dfs.add(df);
        }
        

        this.drivingFeatures = dfs;
    	
    	return this.drivingFeatures;
    }
    

    
    
    public void RecordSingleFeature(PrintWriter w, DrivingFeature df){
        
        String expression = df.getExpression();
        
        //{present[orb;instr;num]}&&{absent[orb;instr;num]}
        
        String[] individual_features = expression.split("&&");
        
        for(int t=0;t<individual_features.length;t++){

            String exp = individual_features[t];
            if(exp.startsWith("{") && exp.endsWith("}")){
                exp = exp.substring(1,exp.length()-1);
            }
            
            
            String type = exp.split("\\[")[0];
            String params = exp.split("\\[")[1];
            params = params.substring(0,params.length()-1);
            String[] paramsSplit = params.split(";");
            
            String orb, instr, num;
            
            switch (paramsSplit.length) {
                case 1:
                    orb=paramsSplit[0];
                    instr = "";
                    num = "";
                    break;
                case 2:
                    orb=paramsSplit[0];
                    instr =paramsSplit[1];
                    num = "";
                    break;
                case 3:
                    orb=paramsSplit[0];
                    instr =paramsSplit[1];
                    num =paramsSplit[2];
                    break;
                default:
                    instr="";
                    orb="";
                    num="";
                    break;
            }
            
            int i,j,k,l;
            String[] instr_split;
            
                
            switch(type) {
                case "present":
                        i = Integer.parseInt(instr);
                        w.print("(0,1,*,"+i+")"); break;
                case "absent":
                        i = Integer.parseInt(instr);
                        w.print("(0,0,A,"+i+")");break;
                case "inOrbit":
                        i = Integer.parseInt(orb);
                        instr_split = instr.split(",");
                        if(instr_split.length==1){
                            j = Integer.parseInt(instr_split[0]);
                            w.print("(0,1,"+i+","+j+")");break;
                        }else if(instr_split.length==2){
                            j = Integer.parseInt(instr_split[0]);
                            k = Integer.parseInt(instr_split[1]);
                            w.print("(0,1,"+i+","+j+","+k+")"); break;
                        }else if(instr_split.length==3){
                            j = Integer.parseInt(instr_split[0]);
                            k = Integer.parseInt(instr_split[1]);
                            l = Integer.parseInt(instr_split[2]);
                            w.print("(0,1,"+i+","+j+","+k+","+l+")"); break;
                        }
                        
                case "notInOrbit":
                        i = Integer.parseInt(orb);
                        j = Integer.parseInt(instr);
                        w.print("(0,0,"+i+","+j+")");break;
                case "together":
                        instr_split = instr.split(",");
                        if(instr_split.length==2){
                            i = Integer.parseInt(instr_split[0]);
                            j = Integer.parseInt(instr_split[1]);
                            w.print("(0,1,*,"+i+","+j+")");break;
                        }else if(instr_split.length==3){
                            i = Integer.parseInt(instr_split[0]);
                            j = Integer.parseInt(instr_split[1]);
                            k = Integer.parseInt(instr_split[2]);
                            w.print("(0,1,*,"+i+","+j+","+k+")");break;
                        }
                case "separate":
                        instr_split = instr.split(",");
                        if(instr_split.length==2){
                            i = Integer.parseInt(instr_split[0]);
                            j = Integer.parseInt(instr_split[1]);
                            w.print("(0,0,A,"+i+","+j+")");break;
                        }else if(instr_split.length==3){
                            i = Integer.parseInt(instr_split[0]);
                            j = Integer.parseInt(instr_split[1]);
                            k = Integer.parseInt(instr_split[2]);
                            w.print("(0,0,A,"+i+","+j+","+k+")");break;
                        }
                case "emptyOrbit":
                        i = Integer.parseInt(orb);
                        w.print("(0,0,"+i+",A)");break;
                case "numOrbits":
                        i = Integer.parseInt(num);
                        w.print("(1,"+i+",*,*)");break;
                case "numOfInstruments":
                        if(instr.length()==0){
                            i = Integer.parseInt(num);
                            w.print("(2,"+i+",*,*)");break;
                        }else{
                            i = Integer.parseInt(num);
                            j = Integer.parseInt(instr);
                            w.print("(2,"+i+",*,"+j+")");break;
                        }                     
                        
            }    
        }
    }
    
    public void recordMetaInfo(PrintWriter w, DrivingFeature feature){
    	
    	double[] metrics;

        metrics = feature.getMetrics();
        String expression = feature.getExpression();
        String[] individual_features = expression.split("&&");
        
        String name = "";
        try{
            for(String expr:individual_features){
                if(expr.startsWith("{") && expr.endsWith("}")){
                    expr = expr.substring(1,expr.length()-1);
                }


                String type = expr.split("\\[")[0];
                String params = expr.split("\\[")[1];
                params = params.substring(0,params.length()-1);
                String[] paramsSplit = params.split(";");
                String orb="";
                String instr="";
                String num="";

                if(!paramsSplit[0].isEmpty()){
                    int o = Integer.parseInt(paramsSplit[0]);
                    orb = DrivingFeaturesParams.orbit_list[o];
                }
                if(paramsSplit.length>1){
                    if(!paramsSplit[1].contains(",")){
                        if(paramsSplit[1].isEmpty()){
                            instr = "";
                        }else{
                            int i = Integer.parseInt(paramsSplit[1]);
                            instr = DrivingFeaturesParams.instrument_list[i];
                        }
                    }else{
                        String[] instrSplit = paramsSplit[1].split(",");
                        instr = "";
                        for(String temp:instrSplit){
                            if(!temp.isEmpty()){
                                int i = Integer.parseInt(temp);
                                instr  = instr + "," + DrivingFeaturesParams.instrument_list[i];
                            }
                        }
                        if(instr.startsWith(",")){
                            instr = instr.substring(1);
                        }
                    }
                }
                if(paramsSplit.length>2){
                    num = paramsSplit[2];
                }

                name = name + "," + type + "[" + orb +";"+ instr + ";" + num + "]";
            }
            if(name.startsWith(",")){
                name = name.substring(1);
            }

            w.print("/" + metrics[0] + "/" + metrics[1] + "// " + name + "\n");
            
        }catch(Exception e){
            System.out.println("Exception in printing feature names:" + expression);
            e.printStackTrace();
        }        
    }
    
    

    /**
     * Saves the topN driving features in an ordered list based on (0: support,
     * 1: lift, 2: confidence)
     *
     * @param features the features to be exported
     * @param filename path and filename to save features
     * @param topN only save the top N features
     */
    public boolean exportDrivingFeatures(String filename, int topN){
        try{

            PrintWriter w = new PrintWriter(filename,"UTF-8");
            w.println("// (mode,arg,orb,inst)/support/lift");

            int count = 1;

            for(DrivingFeature feature:this.drivingFeatures){
                    if (count > topN) {
                        break;
                    }

                    this.RecordSingleFeature(w, feature);
                    this.recordMetaInfo(w, feature);
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

    
    
    
    


    public void parseCSV(String path){
        String line = "";
        String splitBy = ",";
        
        architectures = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            //skip header
            line = br.readLine();
        	
            int id = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] tmp = line.split(splitBy);
                // The first column is the label
                boolean label = tmp[0].equals("1");
                String bitString = tmp[2];

                architectures.add(new Architecture(id,label,bitString2intArr(bitString)));
                if (label){
                	this.behavioral.add(id);
                }else{
                	this.non_behavioral.add(id);
                }                
                
                id++;
               
            }
        } catch (IOException e) {
            System.out.println("Exception in parsing labeled data file");
            e.printStackTrace();
        }
        
        this.population = new ArrayList<>();
        this.population.addAll(behavioral);
        this.population.addAll(non_behavioral);
        this.feh.setArchs(this.architectures, this.behavioral, this.non_behavioral, this.population);
        // Adaptive support threshold
        this.adaptSupp= (double) behavioral.size() / population.size() * 0.5 ;        
    }
    
    


    private int[][] bitString2intArr(String input){
    	int[][] output = new int[norb][ninstr];
    	
    	int cnt=0;
    	if (DrivingFeaturesParams.tallMatrix){
        	for(int i=0;i<ninstr;i++){
        		for (int o=0;o<norb;o++){
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
        	for(int i=0;i<norb;i++){
        		for (int j=0;j<ninstr;j++){
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
    

    public DoubleMatrix getDataMat(ArrayList<DrivingFeature> dfs){

        int ncols = dfs.size();
        int nrows = dfs.get(0).getDatArray().getRows();
        DoubleMatrix mat = DoubleMatrix.zeros(nrows,ncols);
        for(int i=0;i<ncols;i++){
            mat.putColumn(i, dfs.get(i).getDatArray());
        }
        return mat;
    }
    
    
    
    
    
    
    public class Architecture{
        
    	int id;
        boolean label;
    	double[] objectives;
    	int[][] booleanMatrix;
    	
    	public Architecture(int id, boolean label, int[][] mat, double[] objectives){
    		this.id=id;
    		this.label =label;
    		this.booleanMatrix = mat;
    		this.objectives = objectives;
    	}
    	public Architecture(int id, boolean label, int[][] mat){
    		this.id=id;
    		this.label = label;
    		this.booleanMatrix = mat;
    	}

    	public int getID(){
    		return id;
    	}
    	public boolean getLabel(){
    		return label;
    	}
    	public int[][] getBooleanMatrix(){
    		return booleanMatrix;
    	}
    	public double[] getObjectives(){
    		return objectives;
    	}

        
    }
    

    
}
