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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author Bang
 */
public class DrivingFeaturesGenerator {

    private double supp_threshold;
    private double confidence_threshold;
    private double lift_threshold;
    private String[] instrument_list;
    private String[] orbit_list;
    private int ninstr;
    private int norb;

    private ArrayList<int[][]> behavioral;
    private ArrayList<int[][]> non_behavioral;

    private int max_num_of_instruments;
    private int[][] dataMat;

    private ArrayList<DrivingFeature> drivingFeatures;

    public DrivingFeaturesGenerator() {

        this.behavioral = new ArrayList<>();
        this.non_behavioral = new ArrayList<>();
        this.orbit_list = DrivingFeaturesParams.orbit_list;
        this.instrument_list = DrivingFeaturesParams.instrument_list;
        this.norb = orbit_list.length;
        this.ninstr = instrument_list.length;
        this.supp_threshold = DrivingFeaturesParams.support_threshold;
        this.confidence_threshold = DrivingFeaturesParams.confidence_threshold;
        this.lift_threshold = DrivingFeaturesParams.lift_threshold;
        this.drivingFeatures = new ArrayList<>();
        this.max_num_of_instruments = DrivingFeaturesParams.max_number_of_instruments;
    }

    private double[] computeMetrics(Scheme s) {

        double cnt_all = (double) non_behavioral.size() + behavioral.size();
        double cnt_F = 0;
        double cnt_S = (double) behavioral.size();
        double cnt_SF = 0;

        for (int[][] e : behavioral) {
            if (s.compare(e) == 1) {
                cnt_SF = cnt_SF + 1.0;
            }
        }
        for (int[][] e : non_behavioral) {
            if (s.compare(e) == 1) {
                cnt_F = cnt_F + 1.0;
            }
        }

        double cnt_NS = cnt_all - cnt_S;
        double cnt_NF = cnt_all - cnt_F;
        double cnt_S_NF = cnt_S - cnt_SF;
        double cnt_F_NS = cnt_F - cnt_SF;

        double[] metrics = new double[4];

        double support = cnt_SF / cnt_all;
        double support_F = cnt_F / cnt_all;
        double support_S = cnt_S / cnt_all;
        double lift = (cnt_SF / cnt_S) / (cnt_F / cnt_all);
        double conf_given_F = (cnt_SF) / (cnt_F);   // confidence (feature -> selection)
        double conf_given_S = (cnt_SF) / (cnt_S);   // confidence (selection -> feature)

        metrics[0] = support;
        metrics[1] = lift;
        metrics[2] = conf_given_F;
        metrics[3] = conf_given_S;
        return metrics;
    }

    public ArrayList<DrivingFeature> getDrivingFeatures(String labeledDataFile) {

        parseCSV(labeledDataFile);
        Scheme scheme = new Scheme();
        int ind=0;

        scheme.setName("present");
        for (int i = 0; i < ninstr; ++i) {
            scheme.setInstrument (i);
            double[] metrics = computeMetrics(scheme);
            if (checkThreshold(metrics)) {
                int[] param = new int[1];
                param[0] = i;
                String featureName = "present[" + instrument_list[i] + "]";
                drivingFeatures.add(new DrivingFeature(ind,featureName,"present", param, metrics));
                ind++;
            }
        }
        scheme.resetArg();
        scheme.setName("absent");
        for (int i = 0; i < ninstr; ++i) {
            scheme.setInstrument (i);
            double[] metrics = computeMetrics(scheme);
            if (checkThreshold(metrics)) {
                int [] param = new int[1];
                param[0] = i;
                String featureName = "absent[" + instrument_list[i] + "]";
                drivingFeatures.add(new DrivingFeature(ind,featureName,"absent", param, metrics));
                ind++;
            }
        }
        scheme.resetArg();
        scheme.setName("inOrbit");
        for (int i = 0; i < norb; ++i) {
            for (int j = 0; j < ninstr; ++j) {
                scheme.setInstrument (j);
                scheme.setOrbit(i);
                double[] metrics = computeMetrics(scheme);
                if (checkThreshold(metrics)) {
                    int[] param = new int[2];
                    param[0] = i;
                    param[1] = j;
                    String featureName = "inOrbit[" + orbit_list[i] + ", " + instrument_list[j] + "]";
                    drivingFeatures.add(new DrivingFeature(ind,featureName,"inOrbit", param, metrics));
                    ind++;
                }
            }
        }
        scheme.resetArg();
        scheme.setName("notInOrbit");
        for (int i = 0; i < norb; ++i) {
            for (int j = 0; j < ninstr; ++j) {
                scheme.setInstrument (j);
                scheme.setOrbit(i);
                double[] metrics = computeMetrics(scheme);
                if (checkThreshold(metrics)) {
                    int[] param = new int[2];
                    param[0] = i;
                    param[1] = j;
                    String featureName = "notInOrbit[" + orbit_list[i] + ", " + instrument_list[j] + "]";
                    drivingFeatures.add(new DrivingFeature(ind,featureName,"notInOrbit", param, metrics));
                    ind++;
                } 
            }
        }
        scheme.resetArg();
        scheme.setName("together2");
        for (int i = 0; i < ninstr; ++i) {
            for (int j = 0; j < i; ++j) {
                scheme.setInstrument(i);
                scheme.setInstrument2(j);
                double[] metrics = computeMetrics(scheme);
                if (checkThreshold(metrics)) {
                    int[] param = new int[2];
                    param[0] = i;
                    param[1] = j;
                    String featureName = "together2[" + instrument_list[i] + ", " + instrument_list[j] + "]";
                    drivingFeatures.add(new DrivingFeature(ind,featureName,"together2", param, metrics));
                    ind++;
                }
            }
        }    
        scheme.resetArg();
        scheme.setName("togetherInOrbit2");
        for (int i = 0; i < norb; ++i) {
            for (int j = 0; j < ninstr; ++j) {
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
                        String featureName = "togetherInOrbit2[" + orbit_list[i] + ", " + instrument_list[j] + 
                                ", " + instrument_list[k] + "]"; 
                        drivingFeatures.add(new DrivingFeature(ind,featureName,"togetherInOrbit2", param,metrics));
                        ind++;
                    }
                }
            }
        }
        scheme.resetArg();
        scheme.setName("separate2");
        for (int i = 0; i < ninstr; ++i) {
            for (int j = 0; j < i; ++j) {
                scheme.setInstrument(i);
                scheme.setInstrument2(j);
                double[] metrics = computeMetrics(scheme);
                if (checkThreshold(metrics)) {
                        int[] param = new int[2];
                        param[0] = i;
                        param[1] = j;
                        String featureName = "separate2[" + instrument_list[i] + ", " + instrument_list[j] + "]";
                        drivingFeatures.add(new DrivingFeature(ind,featureName,"separate2", param, metrics));
                        ind++;
                    }
            }            
        }
        scheme.resetArg();
        scheme.setName("together3");
        for (int i = 0; i < ninstr; ++i) {
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
                        String featureName = "together3[" + instrument_list[i] + ", " + 
                        		instrument_list[j] + ", " + instrument_list[k] + "]";
                        drivingFeatures.add(new DrivingFeature(ind,featureName,"together3", param, metrics));
                        ind++;
                    }
                }
            }            
        }
        scheme.resetArg();
        scheme.setName("togetherInOrbit3");
        for (int i = 0; i < norb; ++i) {
            for (int j = 0; j < ninstr; ++j) {
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
                            String featureName = "togetherInOrbit3[" + orbit_list[i] + ", " + 
                            		instrument_list[j] + ", " + instrument_list[k] + "," + instrument_list[l] + "]";
                            drivingFeatures.add(new DrivingFeature(ind,featureName,"togetherInOrbit3", param, metrics));
                            ind++;
                        }
                    }
                }
            }
        }
        scheme.resetArg();
        scheme.setName("separate3");
        for (int i = 0; i < ninstr; ++i) {
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
                        String featureName = "separate3[" + instrument_list[i] + ", " + 
                        		instrument_list[j] + ", " + instrument_list[k] + "]";
                        drivingFeatures.add(new DrivingFeature(ind,featureName,"separate3", param, metrics));
                        ind++;
                    }
                }
            }
        }
        scheme.resetArg();
        scheme.setName("emptyOrbit");
        for (int i = 0; i < norb; ++i) {
            scheme.setOrbit(i);
            double[] metrics = computeMetrics(scheme);
            if (checkThreshold(metrics)) {
                int[] param = new int[1];
                param[0] = i;
                String featureName = "emptyOrbit[" + orbit_list[i] + "]";
                drivingFeatures.add(new DrivingFeature(ind,featureName,"emptyOrbit", param, metrics));
                ind++;
            }
        }
        scheme.resetArg();
        scheme.setName("numOrbitUsed");
        for (int i = 1; i < norb+1; i++) {
            scheme.setCount(i);
            double[] metrics = computeMetrics(scheme);
            if (checkThreshold(metrics)) {
                int[] param = new int[1];
                param[0] = i;
                String featureName = "numOrbitUsed[" + param[0] + "]";
                drivingFeatures.add(new DrivingFeature(ind,featureName,"numOrbitUsed", param, metrics));
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
                String featureName = "numInstruments[" + param[0] + "]";
                drivingFeatures.add(new DrivingFeature(ind,featureName,"numInstruments", param, metrics));
                ind++;
            }
        }
        scheme.resetArg();
        scheme.setName("numInstruments"); 
        // Number of each instrument
        for (int i=0;i<ninstr;i++){
        	for (int j=0;j<norb;j++){
        		scheme.setInstrument(i);
	            scheme.setCount(j);
	            double[] metrics = computeMetrics(scheme);
	            if (checkThreshold(metrics)) {
	                int[] param = new int[2];
	                param[0] = i;
	                param[1] = j;
	                String featureName = "numInstruments[" + param[0] + ","+ param[1] +"]";
	                drivingFeatures.add(new DrivingFeature(ind,featureName,"numInstruments", param, metrics));
	                ind++;
	            }
        	}
        }

        return drivingFeatures;
    }

    /**
     * Saves all of the driving features in an ordered list based on (0:
     * support, 1: lift, 2: confidence)
     *
     * @param index the metric to sort features by
     * @param filename path and filename to save features
     */
    public boolean exportDrivingFeatures(int index, String filename) {
        return exportDrivingFeatures(index, filename, drivingFeatures.size());
    }

    /**
     * Saves the topN driving features in an ordered list based on (0: support,
     * 1: lift, 2: confidence)
     *
     * @param index the metric to sort features by
     * @param filename path and filename to save features
     * @param topN only save the top N features
     */
    public boolean exportDrivingFeatures(int index, String filename, int topN) {
        try {

            PrintWriter w = new PrintWriter(filename, "UTF-8");
            w.println("// (mode,arg,orb,inst)/support/lift");

            int count = 0;

            for (DrivingFeature df : sortDrivingFeatures(index)) {
                if (count >= topN) {
                    break;
                }
                
                String name = df.getName();
                double[] metrics = df.getMetrics();
                String type = df.getType();
                int[] param = df.getParam();
                int i = param[0];
                int j = -1;
                int k = -1;
                int l = -1;
                if (param.length > 1) {
                    j = param[1];
                }
                if (param.length > 2) {
                    k = param[2];
                }
                if (param.length > 3) {
                    l = param[3];
                }

                if (type.equalsIgnoreCase("present")) {
                    w.println("(0,1,*," + i + ")/" + metrics[0] + "/" + metrics[1] + " //" + name);
                } else if (type.equalsIgnoreCase("absent")) {
                    w.println("(0,0,*," + i + ")/" + metrics[0] + "/" + metrics[1] + "  //" + name);
                } else if (type.equalsIgnoreCase("inOrbit")) {
                    w.println("(0,1," + i + "," + j + ")/" + metrics[0] + "/" + metrics[1] + "  //" + name);
                } else if (type.equalsIgnoreCase("notInOrbit")) {
                    w.println("(0,0," + i + "," + j + ")/" + metrics[0] + "/" + metrics[1] + "  //" + name);
                } else if (type.equalsIgnoreCase("together2")) {
                    w.println("(0,1,*," + i + "," + j + ")/" + metrics[0] + "/" + metrics[1] + "  //" + name);
                } else if (type.equalsIgnoreCase("togetherInOrbit2")) {
                    w.println("(0,1," + i + "," + j + "," + k + ")/" + metrics[0] + "/" + metrics[1] + "  //" + name);
                } else if (type.equalsIgnoreCase("separate2")) {
                    w.println("(0,0,*," + i + "," + j + ")/" + metrics[0] + "/" + metrics[1] + "  //" + name);
                } else if (type.equalsIgnoreCase("together3")) {
                    w.println("(0,1,*," + i + "," + j + "," + k + ")/" + metrics[0] + "/" + metrics[1] + "  //" + name);
                } else if (type.equalsIgnoreCase("togetherInOrbit3")) {
                    w.println("(0,1," + i + "," + j + "," + k + "," + l + ")/" + metrics[0] + "/" + metrics[1] + "  //" + name);
                } else if (type.equalsIgnoreCase("separate3")) {
                    w.println("(0,0,*," + i + "," + j + "," + k + ")/" + metrics[0] + "/" + metrics[1] + "  //" + name);
                } else if (type.equalsIgnoreCase("emptyOrbit")) {
                    w.println("(0,0," + i + ",*)/" + metrics[0] + "/" + metrics[1] + "  //" + name);
                } else if (type.equalsIgnoreCase("numOrbitUsed")) {
                    w.println("(1," + i + ",*,*)/" + metrics[0] + "/" + metrics[1] + "  //" + name);
                } else if (type.equalsIgnoreCase("numInstruments")) {
                    if (j == -1) {
                        w.println("(2," + i + ",*,*)/" + metrics[0] + "/" + metrics[1] + "  //" + name);
                    } else {
                        w.println("(2," + j + ",*," + i + ")/" + metrics[0] + "/" + metrics[1] + "  //" + name);
                    }
                }
                count++;
            }

            w.flush();
            w.close();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public ArrayList<DrivingFeature> sortDrivingFeatures(int index) {

        ArrayList<DrivingFeature> sortedDF = new ArrayList<>();
        for (DrivingFeature df : drivingFeatures) {
            double val = df.getMetrics()[index];
            if (sortedDF.isEmpty()) {
                sortedDF.add(df);
            } else if (val <= sortedDF.get(0).getMetrics()[index]) {
                // If val is smaller than the first element, insert it at as the first element
                sortedDF.add(0, df);
            } else if (val > sortedDF.get(sortedDF.size() - 1).getMetrics()[index]) {
                // If val is larger than the last element, add it to the last
                sortedDF.add(df);
            } else {
                // Insert df based on the metric value
                for (int i = 0; i < sortedDF.size(); i++) {
                    if (val > sortedDF.get(i).getMetrics()[index]
                            && val <= sortedDF.get(i + 1).getMetrics()[index]) {
                        sortedDF.add(i + 1, df);
                    }
                }
            }
        }
        drivingFeatures = sortedDF;
        return sortedDF;
    }


    public int[][] getDataFeatureMat(){
        
        int numData = behavioral.size() + non_behavioral.size();
        int numFeature = drivingFeatures.size() + 1; // add class label as a last feature
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
                DrivingFeature f = drivingFeatures.get(j);
                int id = f.getID();
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
    
    
    
    public boolean checkThreshold(double[] metrics) {
        if (metrics[0] >= supp_threshold
                && metrics[1] >= lift_threshold
                && metrics[2] >= confidence_threshold
                && metrics[3] >= confidence_threshold) {
            return true;
        } else {
            return false;
        }
    }

    public void parseCSV(String path) {
        String csvFile = path;
        String line = "";
        String splitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            //skip header
            line = br.readLine();

            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] tmp = line.split(splitBy);
                int label = Integer.parseInt(tmp[0]);
                String bitString = tmp[2];
                if (label == 1) {
                    this.behavioral.add(bitString2intArr(bitString));
                } else {
                    this.non_behavioral.add(bitString2intArr(bitString));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int[][] bitString2intArr(String input) {
        int[][] output = new int[norb][ninstr];
        int cnt = 0;
        for (int i = 0; i < ninstr; i++) {
            for (int o = 0; o < norb; o++) {
                int thisBit;
                if (input.length() == 1) {
                    thisBit = Integer.parseInt(input);
                } else {
                    thisBit = Integer.parseInt(input.substring(cnt, cnt + 1));
                }
                output[o][i] = thisBit;
            }
        }
        return output;
    }

    public int[][] booleanToInt(boolean[][] b) {
        int[][] intVector = new int[b.length][b[0].length];
        for (int i = 0; i < b.length; i++) {
            for (int j = 0; j < b[0].length; ++j) {
                intVector[i][j] = b[i][j] ? 1 : 0;
            }
        }
        return intVector;
    }

}
