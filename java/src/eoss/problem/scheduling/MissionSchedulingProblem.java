/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.scheduling;

import eoss.problem.Panel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;

import org.moeaframework.core.Solution;
import org.moeaframework.problem.AbstractProblem;
import org.orekit.time.AbsoluteDate;

/**
 *
 * @author nozomihitomi
 */
public class MissionSchedulingProblem extends AbstractProblem {

    /**
     * The budget of a series of dates
     */
    private final ArrayList<Budget> budgets;

    /**
     * The missions to schedule
     */
    private final HashMap<Integer, Mission> missions;

    private final ArrayList<Panel> panels;

    private final AbsoluteDate startDate;

    private final AbsoluteDate endDate;
    
    private final double stepSize;

    private final HashMap<Panel, Double> panelDiscountRate;
    
    private final HashMap<Mission, RealMatrix> missionDataContinuity;
    
    private final HashMap<Integer, HashMap<AbsoluteDate, ArrayList<String>>> precursorsDataContinuityMatrix;

    public MissionSchedulingProblem(Path path) {
        super(1, 2, 0); //1 permutation variable, 2 objectives, 0 constraints

    }

    /**
     * Computes the dates of the launches from the sequence, the available
     * budget, and the cost profile for each mission
     *
     * @param perm
     * @return
     */
    private Collection<AbsoluteDate> computeLaunchDates(MissionSchedulingArchitecture schedule) {
        final ArrayList<AbsoluteDate> launchDates = new ArrayList<>(missions.size());

        double currentBudget = 0;
        int profileCounter = 0;
        int missionCounter = 0;
        double[] currentCostProfile = missions.get(schedule.getSequence()[missionCounter]).getCostProfile();
        for (Budget b : budgets) {
            currentBudget += b.getBudget();

            //check what can be funded
            while (missionCounter < missions.size()) {
                if (currentBudget >= currentCostProfile[profileCounter]) {
                    currentBudget -= currentCostProfile[profileCounter];
                    currentCostProfile[profileCounter] = 0;
                    profileCounter++;
                    if (profileCounter == currentCostProfile.length) {
                        //launch mission
                        launchDates.add(b.getDate());

                        //move to next mission
                        missionCounter++;
                        currentCostProfile = missions.get(schedule.getSequence()[missionCounter]).getCostProfile();
                        profileCounter = 0;
                    }
                } else {
                    //spend all the budget on the available part of the mission
                    currentCostProfile[profileCounter] -= currentBudget;
                    currentBudget = 0;
                    break;
                }
            }
        }

        return launchDates;
    }

    /**
     * Computes the discounted value. The original value is how much each panel
     * benefits from a specific mission operating at a given date. These values
     * are discounted as a function of a discount rate r and time dt
     * (d = d_0*exp(-r*dt))
     *
     * @param dates the launch dates computed for the 
     * @param schedule a schedule of missions
     * @return
     */
    private double computeDiscountedValue(MissionSchedulingArchitecture schedule) {
        RealMatrix discountedMatrix = new Array2DRowRealMatrix(missions.size(), panels.size());
        ArrayList<AbsoluteDate> sortedDates = new ArrayList<>(schedule.getLaunchDates().values());
        Collections.sort(sortedDates);
        for (int i = 0; i < missions.size(); i++) {
            double dt = (sortedDates.get(i).durationFrom(startDate)/(365*24*3600.)); //convert sec to years
            for (int j = 0; j < panels.size(); j++) {
                double r = panelDiscountRate.get(panels.get(j));
                discountedMatrix.setEntry(i, j, Math.exp(-r * dt));
            }
        }
        double discountedValue = 0;
        for (int i = 0; i < missions.size(); i++) {
            discountedValue += discountedMatrix.getRowVector(i)
                    .dotProduct(schedule.getPanelScores().getRowVector(i));
        }
        return discountedValue;
    }
    
    private double computeDataContinuityScore(MissionSchedulingArchitecture schedule){
        overall_matrix = params.precursors_data_continuity_matrix;
        int[] permutation = schedule.getSequence();
        
        for(int i=0; i<missions.size(); i++){
            // Offset corresponding params.MissionMatrices according to launch date and lifetime
            Mission currentMission = missions.get(permutation[i]);
            RealMatrix matrix0 = missionDataContinuity.get(currentMission);
            double lifetime = currentMission.getLifetime();
            AbsoluteDate launchdate2 = schedule.getLaunchDates().get(currentMission);
            RealMatrix matrix1 = OffsetContinuityMatrix(matrix0,lifetime,launchdate2);
    
            // Superimpose all matrices
            RealMatrix overall_matrix = SuperimposeContinuityMatrix(overall_matrix,matrix1);
            
            //initialize
            overall_matrix = cell(size(matrix0));
            for i = 1:size(matrix0,1)
                for j = 1:size(matrix0,2)
                    overall_matrix(i,j) = java.util.ArrayList;
                end
            end

            % create new matrix
            for i = 1:size(matrix0,1)// for each measurement
                for j = 1:size(matrix0,2)
                    overall_matrix{i,j} = matrix0{i,j}.clone();
                    if matrix1{i,j}.size>0
                        array = matrix1{i,j}.iterator;
                        while array.hasNext
                            overall_matrix{i,j}.add(array.next());
                        end
                    end
                end
            end
        }

// Compute data continuity score from new matrix
data_continuity_matrix_int = cellfun(@size,overall_matrix);
data_continuity_matrix_diff = data_continuity_matrix_int - params.precursors_data_continuity_integer_matrix;
data_continuity_matrix_diff(data_continuity_matrix_diff < 0) = 0;%only improvements are considered
data_continuity_score = params.measurement_weights_for_data_continuity*data_continuity_matrix_diff*params.data_continuity_weighting_scheme;
    }


    @Override
    public void evaluate(Solution sltn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Solution newSolution() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
