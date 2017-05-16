/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mining;

import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Orbit;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;

/**
 *
 * @author bang
 */
public class FilterApplication {

    public static boolean tallMatrix = true;

    /**
     * Reads in input files, applies features, and outputs a csv file.
     */
    public static void run(String labledDataFile, String featureDataFile, String outputFile) {

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
     *
     * @param featureDataFile: Path to the file containing the list of features
     * to apply
     * @return combinedFeatures: list of strings containing the feature
     * expressions
     */
    private static ArrayList<String> registerFeature(String featureDataFile) {

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
            }
        } catch (IOException e) {
            System.out.println("Exception in parsing labeled data file");
            e.printStackTrace();
        }
        return combinedFeatures;

    }

    /**
     * Reads in bit strings from a file.
     *
     * @param labledDataFile: Path to the file containing the bitStrings
     * @return architectures: ArrayList of integer arrays
     */
    private static ArrayList<int[][]> registerData(String labledDataFile) {

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
                int[] bitString = new int[EOSSDatabase.getNumberOfOrbits() * EOSSDatabase.getNumberOfInstruments()];
                for (int i = 0; i < 60; i++) {
                    bitString[i] = Integer.parseInt(rowSplit[i + 2]);
                }

                architectures.add(bitString2intArr(bitString));
            }
        } catch (IOException e) {
            System.out.println("Exception in parsing labeled data file");
            e.printStackTrace();
        }
        return architectures;
    }

    /**
     * Writes a csv file with binary arrays.
     *
     * @param filename: Path to the file to write
     * @param out: ArrayList of integer arrays. Each integer array contains the
     * result of applying one combined feature
     * @return
     */
    public static boolean exportFilterOutput(String filename, ArrayList<BitSet> out, ArrayList<String> features, int size) {
        try {

            PrintWriter w = new PrintWriter(filename, "UTF-8");

            StringBuilder sb = new StringBuilder();

            for (String feat : features) {
                sb.append(feat).append("||");
            }
            sb.delete(sb.length() - 2, sb.length());

            for (int i = 0; i < out.size(); i++) {

                sb.append("\n");

                BitSet bs = out.get(i);

                for (int j = 0; j < size; j++) {
                    boolean b = bs.get(j);
                    if (j > 0) {
                        sb.append(",");
                    }
                    if (b) {
                        sb.append("1");
                    } else {
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
     *
     * @param architectures: ArrayList of 2D integer array. The integer array is
     * of size [number of orbits X number of instruments]
     * @param features: ArrayList of string with feature expressions.
     * @return ArrayList of BitSets. Each BitSet is of the same size as the
     * number of input architectures.
     */
    private static ArrayList<BitSet> applyFilter(ArrayList<int[][]> architectures, ArrayList<String> features) {
        ArrayList<BitSet> out = new ArrayList<>();
        for (String feat : features) {
            BitSet match = new BitSet(architectures.size());
            for (int i = 0; i < architectures.size(); i++) {
                match.set(i, checkFeature(feat, architectures.get(i)));
            }
            out.add(match);
        }
        return out;
    }

    private static boolean checkFeature(String feature, int[][] architecture) {
        //first split the feature into its base parts
        for (String baseFeature : feature.split("&&")) {
            if (!processSingleFilterExpression(baseFeature, architecture)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes in a single feature expression and returns a list of indices of the
     * solutions that pass the filter
     *
     * @param inputExpression: Expression of a single feature
     * @param architecture: A list of architectures each represented by a 2D
     * integer array.
     * @return BitSet of indicating which solutions passed the filter
     */
    public static boolean processSingleFilterExpression(String inputExpression, int[][] architecture) {
        // Examples of a single feature expression: presetName[orbits;instruments;numbers]

        String exp;
        if (inputExpression.startsWith("{") && inputExpression.endsWith("}")) {
            exp = inputExpression.substring(1, inputExpression.length() - 1);
        } else {
            exp = inputExpression;
        }

        String presetName = exp.split("\\[")[0];
        String arguments = exp.split("\\[")[1].split("\\]")[0];

        String[] argSplit = arguments.split(";");

        int[] orbits = new int[0];
        int[] instruments = new int[0];
        int[] numbers = new int[0];

        if (argSplit.length > 0) {
            String[] orbitStrings = argSplit[0].split(",");
            if (!orbitStrings[0].isEmpty()) {
                orbits = new int[orbitStrings.length];
                for (int i = 0; i < orbitStrings.length; i++) {
                    Orbit orb = EOSSDatabase.getOrbit(orbitStrings[i]);
                    if(orb == null){
                        orb = EOSSDatabase.getOrbit(Integer.parseInt(orbitStrings[i]));
                    }
                    orbits[i] = EOSSDatabase.findOrbitIndex(orb);
                }
            }
        }
        if (argSplit.length > 1) {
            String[] instrStrings = argSplit[1].split(",");
            if (!instrStrings[0].isEmpty()) {
                instruments = new int[instrStrings.length];
                for (int i = 0; i < instrStrings.length; i++) {
                    Instrument inst = EOSSDatabase.getInstrument(instrStrings[i]);
                    if(inst == null){
                        inst = EOSSDatabase.getInstrument(Integer.parseInt(instrStrings[i]));
                    }
                    instruments[i] = EOSSDatabase.findInstrumentIndex(inst);
                }
            }
        }

        if (argSplit.length > 2) {
            String[] numberStrings = argSplit[2].split(",");
            if (!numberStrings[0].isEmpty()) {
                numbers = new int[numberStrings.length];
                for (int i = 0; i < numberStrings.length; i++) {
                    numbers[i] = Integer.parseInt(numberStrings[i]);
                }
            }
        }

        return FilterApplication.comparePresetFilter(architecture, presetName, orbits, instruments, numbers);
    }

    private static int[][] bitString2intArr(int[] input) {
        int[][] output = new int[EOSSDatabase.getNumberOfOrbits()][EOSSDatabase.getNumberOfInstruments()];

        int cnt = 0;
        if (tallMatrix) {
            for (int i = 0; i < EOSSDatabase.getNumberOfInstruments(); i++) {
                for (int o = 0; o < EOSSDatabase.getNumberOfOrbits(); o++) {
                    output[o][i] = input[cnt];
                    cnt++;
                }
            }
        } else {
            for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); i++) {
                for (int j = 0; j < EOSSDatabase.getNumberOfInstruments(); j++) {
                    output[i][j] = input[cnt];
                    cnt++;
                }
            }
            cnt++;
        }
        return output;
    }

    private static boolean comparePresetFilter(int[][] mat, String type, int[] orbits, int[] instruments, int[] numbers) {
        switch (type) {
            case "present":
                for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); i++) {
                    if (mat[i][instruments[0]] == 1) {
                        return true;
                    }
                }
                return false;

            case "absent":
                for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); ++i) {
                    if (mat[i][instruments[0]] == 1) {
                        return false;
                    }
                }
                return true;
                
            case "inOrbit":
                for (int instrument : instruments) {
                    if (mat[orbits[0]][instrument] == 0) {
                        return false;
                    }
                }
                return true;
                
            case "notInOrbit":
                for (int instrument : instruments) {
                    if (mat[orbits[0]][instrument] == 1) {
                        return false;
                    }
                }
                return true;
                
            case "together":
                for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); i++) {
                    boolean together = true;
                    for (int j = 0; j < instruments.length; j++) {
                        int instrument = instruments[j];
                        if (mat[i][instrument] == 0) {
                            together = false;
                        }
                    }
                    if (together) {
                        return true;
                    }
                }
                return false;
            case "separate":
                for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); i++) {
                    boolean together = true;
                    for (int j = 0; j < instruments.length; j++) {
                        int instrument = instruments[j];
                        if (mat[i][instrument] == 0) {
                            together = false;
                        }
                    }
                    if (together) {
                        return false;
                    }
                }
                return true;
            case "emptyOrbit":
                int orbit = orbits[0];
                for (int i = 0; i < EOSSDatabase.getNumberOfInstruments(); i++) {
                    if (mat[orbit][i] == 1) {
                        return false;
                    }
                }
                return true;
            case "numOrbits":
                int orbitCount = 0;
                for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); ++i) {
                    boolean empty = true;
                    for (int j = 0; j < EOSSDatabase.getNumberOfInstruments(); j++) {
                        if (mat[i][j] == 1) {
                            empty = false;
                        }
                    }
                    if (empty == false) {
                        orbitCount++;
                    }
                }
                return orbitCount == numbers[0];
            case "numOfInstruments":
                // Three cases
                //numOfInstruments[;i;j]
                //numOfInstruments[i;;j]
                //numOfInstruments[;;i]
                int instCount = 0;

                if (orbits.length != 0) {
                    // Number of instruments in a specified orbit
                    for (int i = 0; i < EOSSDatabase.getNumberOfInstruments(); i++) {
                        if (mat[orbits[0]][i] == 1) {
                            instCount++;
                        }
                    }
                } else if (instruments.length != 0) {
                    // Number of a specified instrument
                    int instrument = instruments[0];
                    for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); i++) {
                        if (mat[i][instrument] == 1) {
                            instCount++;
                        }
                    }
                } else {
                    // Number of instruments in all orbits
                    for (int i = 0; i < EOSSDatabase.getNumberOfOrbits(); i++) {
                        for (int j = 0; j < EOSSDatabase.getNumberOfInstruments(); j++) {
                            if (mat[i][j] == 1) {
                                instCount++;
                            }
                        }
                    }
                }
                if (instCount == numbers[0]) {
                    return true;
                }
                return false;
            default:
                throw new UnsupportedOperationException(String.format("Unexpected feature type %s", type));
        }
    }

}
