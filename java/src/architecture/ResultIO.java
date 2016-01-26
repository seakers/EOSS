/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package architecture;

/**
 *
 * @author Marc
 */
import hh.credittest.HHCreditTest;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.moeaframework.analysis.collector.Accumulator;
import org.moeaframework.analysis.collector.InstrumentedAlgorithm;
import org.moeaframework.core.Population;
import org.moeaframework.core.PopulationIO;

public class ResultIO implements Serializable {

    private static final long serialVersionUID = -2048768868854760056L;

    public ResultIO() {
    }

    /**
     * Saves the measured metrics from the instrumenter (e.g. hypervolume,
     * elapsed time)
     *
     * @param instAlgorithm
     * @param filename filename including the path
     */
    public void saveMetrics(InstrumentedAlgorithm instAlgorithm, String filename) {
        Accumulator accum = instAlgorithm.getAccumulator();

        File results = new File(filename + ".res");
        System.out.println("Saving metrics");

        try (FileWriter writer = new FileWriter(results)) {
            Set<String> keys = accum.keySet();
            Iterator<String> keyIter = keys.iterator();
            while (keyIter.hasNext()) {
                String key = keyIter.next();
                int dataSize = accum.size(key);
                writer.append(key).append(",");
                for (int i = 0; i < dataSize; i++) {
                    writer.append(accum.get(key, i).toString());
                    if (i + 1 < dataSize) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }
            writer.flush();

        } catch (IOException ex) {
            Logger.getLogger(HHCreditTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Saves only the objective values of the solutions in the population
     *
     * @param pop
     * @param filename
     */
    public void saveObjectives(Population pop, String filename) {
        System.out.println("Saving objectives");

        try {
            PopulationIO.writeObjectives(new File(filename + ".obj"), pop);
        } catch (IOException ex) {
            Logger.getLogger(ResultIO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Reads a set of objective vectors from the specified file. Files read
     * using this method should only have been created using the
     * {@code saveObjectives} method.
     *
     * @param file the file containing the objective vectors
     * @return a population containing all objective vectors in the specified
     * file
     * @throws IOException if an I/O exception occurred
     */
    public Population readObjectives(File file) throws IOException {
        return PopulationIO.readObjectives(file);
    }

    /**
     * Writes a collection of solutions to the specified file. This saves all
     * the explanations as well as any computed objectives Files written using
     * this method should only be read using the {@link #read} method. This
     * method relies on serialization.
     *
     * @param pop the solutions to be written in the specified file
     * @param filename the filename including the path to which the solutions
     * are written
     */
    public void savePopulation(Population pop, String filename) {
        System.out.println("Saving population");

        try {
            PopulationIO.write(new File(filename + ".obj"), pop);
        } catch (IOException ex) {
            Logger.getLogger(ResultIO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Reads a population from the specified file. Files read using this method
     * should only have been created using the {@link #write} method. This
     * method relies on serialization.
     *
     * @param filename the filename including the path to which the solutions
     * are written
     * @return a population containing all solutions in the specified file
     * @throws IOException if an I/O exception occurred
     */
    public Population loadPopulation(String filename) throws IOException {
        return PopulationIO.read(new File(filename));
    }

}