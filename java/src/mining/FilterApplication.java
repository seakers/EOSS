/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mining;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.BitSet;

/**
 *
 * @author bang
 */
public class FilterApplication {
    
    public static int norb = 5;
    public static int ninstr = 12;
    public static boolean tallMatrix = true;
    
    public static List<String> instrument_list = Arrays.asList(new String[]{"ACE_ORCA","ACE_POL","ACE_LID","CLAR_ERB","ACE_CPR","DESD_SAR","DESD_LID","GACM_VIS","GACM_SWIR",
                                                     "HYSP_TIR","POSTEPS_IRS","CNES_KaRIN"});
    public static List<String> orbit_list = Arrays.asList(new String[]{"LEO-600-polar-NA", "SSO-600-SSO-AM", "SSO-600-SSO-DD","SSO-800-SSO-DD", "SSO-800-SSO-PM"});
    
    
    /**
     * Reads in input files, applies features, and outputs a csv file.
    */
    public static void run(String labledDataFile, String featureDataFile, String outputFile){
        
        // Read in files
        ArrayList<int[][]> architectures = registerData(labledDataFile);
                
        ArrayList<String> features = registerFeature(featureDataFile);

        // Apply features
        ArrayList<BitSet> output = applyFilter(architectures, features);
        
        
        // Writes an output file
        exportFilterOutput(outputFile, output, features, architectures.size());
    
    }
    
    
    
    /**
     * Reads in the feature expressions from a file.
     * @param featureDataFile: Path to the file containing the list of features to apply
     * @return combinedFeatures: list of strings containing the feature expressions
     */
    private static ArrayList<String> registerFeature(String featureDataFile){
        
        String line = "";
        String splitBy = ",";

        ArrayList<String> combinedFeatures = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(featureDataFile))) {
            
            //skip header
            line = br.readLine();

            while ((line = br.readLine()) != null) {
                
                String feature = line.split("//")[1].trim();
                feature = feature.replaceAll("],", "]&&");
                combinedFeatures.add(feature);
                System.out.println(feature);
                
            }
        } catch (IOException e) {
            System.out.println("Exception in parsing labeled data file");
            e.printStackTrace();
        }
        return combinedFeatures;
        
    }
    
    /**
     * Reads in bit strings from a file.
     * @param labledDataFile: Path to the file containing the bitStrings
     * @return architectures: ArrayList of integer arrays
     */
    private static ArrayList<int[][]> registerData(String labledDataFile){
    
        String line = "";
        String splitBy = ",";

        ArrayList<int[][]> architectures = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(labledDataFile))) {
            
            //skip header
            line = br.readLine();

            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] rowSplit = line.split(splitBy);
                // The first column is the label
                boolean label = rowSplit[0].equals("1");
                StringBuilder sb = new StringBuilder();
                
                //skip first variables ince it is the number of satellites per plane
                for (int i = 1; i < 61; i++) {
                    sb.append(rowSplit[i + 1]);
                }
                sb.toString();
                architectures.add(bitString2intArr(sb.toString()));
            }
        } catch (IOException e) {
            System.out.println("Exception in parsing labeled data file");
            e.printStackTrace();
        }
        return architectures;
    }
    
    /**
     * Writes a csv file with binary arrays.
     * @param filename: Path to the file to write
     * @param out: ArrayList of integer arrays. Each integer array contains the result of applying one combined feature
     * @return
     */
    private static boolean exportFilterOutput(String filename, ArrayList<BitSet> out, ArrayList<String> features, int size) {
        try {

            PrintWriter w = new PrintWriter(filename, "UTF-8");
            
            StringBuilder sb = new StringBuilder();
            
            for(String feat : features){
                sb.append(feat).append("||");
            }
            sb.delete(sb.length()-2, sb.length());
            
            for (int i=0;i<out.size();i++){
                
                sb.append("\n");
                
                BitSet bs = out.get(i);
                
                for(int j=0;j<size;j++){
                    boolean b = bs.get(j);
                    if(j>0){
                        sb.append(",");
                    }
                    if(b){
                        sb.append("1");
                    }else{
                        sb.append("0");
                    }
                }
            }
            
            w.print(sb.toString());
            w.flush();
            w.close();

        } catch (Exception e) {
            System.out.println("Exception in exporting output");
            e.printStackTrace();
            return false;
        }
        return true;
    }
    

    /**
     * Applies features to all architecture inputs.
     * @param architectures: ArrayList of 2D integer array. The integer array is of size [number of orbits X number of instruments]
     * @param features: ArrayList of string with feature expressions.
     * @return ArrayList of BitSets. Each BitSet is of the same size as the number of input architectures. 
     */
    private static ArrayList<BitSet> applyFilter(ArrayList<int[][]> architectures, ArrayList<String> features){

        BitSet population = new BitSet(architectures.size());

        population.set(0, architectures.size()-1);
        
        ArrayList<BitSet> out = new ArrayList<>();
        

        for(String feat:features){            
            BitSet matched = processFilterExpression(feat, population, architectures);
            out.add(matched);
        }
        return out;
    }

    

    /**
     * Takes in a combined feature expression and returns a list of indices of the solutions that pass the filter
     * @param filterExpression: Expression of a combined feature (only conjunctions can be handled).
     * @param prevMatched: A BitSet indicating which of the solutions passed the filter in the previous recursive step.
     * @param architectures: A list of architectures each represented by a 2D integer array.
     * @return A BitSet indicating which solutions passed the filter
     */
    private static BitSet processFilterExpression(String filterExpression, BitSet prevMatched, ArrayList<int[][]> architectures){

        String expression = filterExpression;

        BitSet currMatched = new BitSet(architectures.size());
        boolean first = true;
        

        if(!expression.contains("&&")){
            // Single feature expression
            currMatched = FilterApplication.processSingleFilterExpression(expression, architectures);
            return compareMatchedIDSets("&&", currMatched, prevMatched);
        }


        while(true){
            
            if(first){
                // The first filter in a series to be applied
                currMatched = prevMatched;
                first = false;
            }else{
                expression = expression.substring(2);
            }
            
            boolean next = false;
            if(expression.contains("&&")){
                next = true;
            } 
            
            if(next){
                String singleFeature = expression.split("&&",2)[0];
                expression = expression.substring(singleFeature.length());
                currMatched = processFilterExpression(singleFeature,currMatched, architectures);
                               
            }else{
                currMatched = processFilterExpression(expression,currMatched, architectures);             	
                break;
            }
        }
        return currMatched;
    }
    
    
    /**
     * Takes in a single feature expression and returns a list of indices of the solutions that pass the filter
     * @param inputExpression: Expression of a single feature
     * @param architectures: A list of architectures each represented by a 2D integer array.
     * @return BitSet of indicating which solutions passed the filter
     */
    private static BitSet processSingleFilterExpression(String inputExpression, ArrayList<int[][]> architectures){
        // Examples of a single feature expression: presetName[orbits;instruments;numbers]
        
        BitSet matched = new BitSet(architectures.size());
        String exp;
        if(inputExpression.startsWith("{") && inputExpression.endsWith("}")){
            exp = inputExpression.substring(1,inputExpression.length()-1);
        }else{
            exp = inputExpression;
        }
        
        String presetName = exp.split("\\[")[0];
        String arguments = exp.substring(0,exp.length()-1).split("\\[")[1];
        
        String[] argSplit = arguments.split(";");
        
        int[] orbits =  new int[0];
        int[] instruments =  new int[0];
        int[] numbers =  new int[0];

        if(argSplit.length>0){
            String[] orbitStrings = argSplit[0].split(",");
            if(!orbitStrings[0].isEmpty()){
                orbits = new int[orbitStrings.length];
                for(int i=0;i<orbitStrings.length;i++){
                    orbits[i] = orbit_list.indexOf(orbitStrings[i]);
                }
            }
        }
        if(argSplit.length>1){
            String[] instrStrings = argSplit[1].split(",");
            if(!instrStrings[0].isEmpty()){
                instruments = new int[instrStrings.length];
                for(int i=0;i<instrStrings.length;i++){
                    instruments[i] = instrument_list.indexOf(instrStrings[i]);
                }
            }
        }
        
        if(argSplit.length>2){
            String[] numberStrings = argSplit[2].split(",");
            if(!numberStrings[0].isEmpty()){
                numbers = new int[numberStrings.length];
                for(int i=0;i<numberStrings.length;i++){
                    numbers[i] = Integer.parseInt(numberStrings[i]);
                }
            }
        }
        
        for(int i=0;i<architectures.size();i++){
            int[][] arch = architectures.get(i);
            if(FilterApplication.comparePresetFilter(arch, presetName,orbits,instruments,numbers)){
                matched.set(i);
            }
        }
        
        return matched;
    }    
    
    
    /**
     * Combines two sets of integers using specified logical connective
     * @param logic: String indicating the logical connective. Can be either "&&" or "||"
     * @param set1: List of indices of solutions
     * @param set2: List of indices of solutions
     * @return List of integers
     */
    private static BitSet compareMatchedIDSets(String logic, BitSet set1, BitSet set2){
        BitSet bs = (BitSet) set1.clone();
        if(logic.equals("&&")){
            bs.and(set2);
        }else{
            bs.or(set2);
        }
        return bs;
    }
    
    
    
    private static int[][] bitString2intArr(String input) {
        int[][] output = new int[norb][ninstr];

        int cnt = 0;
        if (tallMatrix) {
            for (int i = 0; i < ninstr; i++) {
                for (int o = 0; o < norb; o++) {
                    int thisBit;
                    if (cnt == input.length() - 1) {
                        thisBit = Integer.parseInt(input.substring(cnt));
                    } else {
                        thisBit = Integer.parseInt(input.substring(cnt, cnt + 1));
                    }
                    output[o][i] = thisBit;
                    cnt++;
                }
            }
        } else {
            for (int i = 0; i < norb; i++) {
                for (int j = 0; j < ninstr; j++) {
                    int thisBit;
                    if (cnt == input.length() - 1) {
                        thisBit = Integer.parseInt(input.substring(cnt));
                    } else {
                        thisBit = Integer.parseInt(input.substring(cnt, cnt + 1));
                    }
                    output[i][j] = thisBit;
                    cnt++;
                }
            }
            cnt++;
        }
        return output;
    }
    
    
    
    private static boolean comparePresetFilter(int[][] mat, String type, int[] orbits, int[] instruments, int[] numbers){
        
        if(type.equalsIgnoreCase("present")){
            int instrument = instruments[0];
            for (int i=0;i<norb;i++) {
                if (mat[i][instrument]==1) return true;
            }
            return false;
        } else if(type.equalsIgnoreCase("absent")){
            
            int instrument = instruments[0];
            for (int i = 0; i < norb; ++i) {
                if (mat[i][instrument]==1) return false;
            }
            return true;  
        } else if(type.equalsIgnoreCase("inOrbit")){
            int orbit = orbits[0];
            boolean together = true;
            for(int j=0;j<instruments.length;j++){
                int instrument = instruments[j];
                if(mat[orbit][instrument]==0){together=false;}
            }
            if(together){return true;}            
            return false;
        } else if(type.equalsIgnoreCase("notInOrbit")){
            
            int orbit = orbits[0];
            int instrument = instruments[0];
            return mat[orbit][instrument] == 0;
            
        } else if(type.equalsIgnoreCase("together")){
            
            for(int i=0;i<norb;i++){
                boolean together = true;
                for(int j=0;j<instruments.length;j++){
                    int instrument = instruments[j];
                    if(mat[i][instrument]==0){together=false;}
                }
                if(together){return true;}
            }
            return false;
            
        } else if(type.equalsIgnoreCase("separate")){
            
            for(int i=0;i<norb;i++){
                boolean together = true;
                for(int j=0;j<instruments.length;j++){
                    int instrument = instruments[j];
                    if(mat[i][instrument]==0){together=false;}
                }
                if(together){return false;}
            }
            return true;
            
        } else if(type.equalsIgnoreCase("emptyOrbit")){
            
            int orbit = orbits[0];
            for(int i=0;i<ninstr;i++){
                if(mat[orbit][i]==1){return false;}
            }
            return true;
           
        } else if(type.equalsIgnoreCase("numOrbits")){
            
            int num = numbers[0];
            int count = 0;
            for (int i = 0; i < norb; ++i) {
               boolean empty= true;
               for (int j=0; j< ninstr; j++){
                   if(mat[i][j]==1){
                       empty= false;
                   }
               }
               if(empty==false) count++;
            }
            return count==num;     
            
        } else if(type.equalsIgnoreCase("numOfInstruments")){
            // Three cases
            //numOfInstruments[;i;j]
            //numOfInstruments[i;;j]
            //numOfInstruments[;;i]
            
            int num = numbers[0];
            int count = 0;

            if(orbits.length!=0){
                // Number of instruments in a specified orbit
                int orbit = orbits[0];
                for(int i=0;i<ninstr;i++){
                    if(mat[orbit][i]==1){count++;}
                }
            }else if(instruments.length!=0){
                // Number of a specified instrument
                int instrument = instruments[0];
                for(int i=0;i<norb;i++){
                    if(mat[i][instrument]==1){count++;}
                }
            }else{
                // Number of instruments in all orbits
                for(int i=0;i<norb;i++){
                    for(int j=0;j<ninstr;j++){
                        if(mat[i][j]==1){count++;}
                    }
                }
            }
            if(count==num){return true;}
            return false;
            
        }
        
        return false;
    }
        
}