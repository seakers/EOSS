/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mining.label;

import aos.IO.IOQualityHistory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;

/**
 * This class will export the labels to a dlm file.
 *
 * @author nozomihitomi
 */
public class LabelIO implements Serializable {

    private static final long serialVersionUID = -7850252430671191170L;

    public LabelIO() {
        super();
    }

    /**
     * This method will save the label of each individual stored in the
     * population to a dlm file with a user specified separator. Only
     * individuals with a label attribute will be saved. In addition to the
     * label, the decision values and objective values will be saved in the file
     * as well. If the population is empty, this method does not attempt to save
     * to any file and returns false
     *
     * @param population
     * @param filename
     * @param separator
     * @return True if a file is successfully saved. Else false.
     */
    public boolean saveLabels(Population population, String filename, String separator) {
        if (population.isEmpty()) {
            return false;
        }
        //Only try saving populations that are not empty
        try (FileWriter fw = new FileWriter(new File(filename))) {
            //write the header
            fw.append("Label" + separator);
            for (int i = 0; i < population.get(0).getNumberOfVariables(); i++) {
                    fw.append(String.format("dec%d%s", i, separator));
            }
            for (int i = 0; i < population.get(0).getNumberOfObjectives(); i++) {
                if (i == population.get(0).getNumberOfObjectives() - 1) {
                    fw.append(String.format("obj%d\n", i));
                } else {
                    fw.append(String.format("obj%d%s", i, separator));
                }
            }

            //Write information of each individual
            int numDec = population.get(0).getNumberOfVariables();
            int numObj = population.get(0).getNumberOfVariables();
            for (Solution individual : population) {
                if (individual.hasAttribute(AbstractPopulationLabeler.LABELATTRIB)) {
                    fw.append(individual.getAttribute(AbstractPopulationLabeler.LABELATTRIB) + separator);
                    for (int i = 0; i < numDec; i++) {
                        fw.append(String.format("%s%s", individual.getVariable(i), separator));
                    }
                    for (int i = 0; i < numObj; i++) {
                        if (i == numObj - 1) {
                            fw.append(String.format("%f\n", individual.getObjective(i)));
                        } else {
                            fw.append(String.format("%f%s", individual.getObjective(i), separator));
                        }
                    }
                }
            }
            fw.flush();
        } catch (IOException ex) {
            Logger.getLogger(IOQualityHistory.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }
}
