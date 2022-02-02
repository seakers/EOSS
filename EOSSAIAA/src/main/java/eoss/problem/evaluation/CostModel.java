/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.evaluation;

import eoss.problem.Instrument;
import eoss.spacecraft.Spacecraft;
import java.util.Collection;

/**
 *
 * @author nozomihitomi
 */
public class CostModel {

    /**
     * Computes the cost of a spacecraft. cost model accounts for learning
     * factor if more than one is required
     *
     * @param s the spacecraft design
     * @param lifetime the lifetime of the spacecraft: affects recurrant costs
     * @param launchCost the launch cost for the spacecraft and its copies
     * @param nsat the number of copies to make of this spacecraft
     * @return
     */
    public static double lifeCycleCost(Spacecraft s, double lifetime, double launchCost, int nsat) {
        double lifeCycleCost = 0.0;

        //payload costs (NRC = non-recurring cost) (RC = recurring cost)
        double payloadCost = payloadCost(s);
        double payloadNRC = 0.8 * payloadCost;
        double payloadRC = 0.2 * payloadCost;

        //bus costs
        double busNRC = busNonRecurringCost(s);
        double busRC = busRecurringCost(s);

        //This rule estimates s/c non recurring cost adding up bus and payload 
        //n/r cost spacecraft recurring and non-recurring s/c cost       
        double spacecraftNRC = busNRC + payloadCost * 0.6;
        double spacecraftRC = busRC + payloadCost * 0.4;
        double spacecraftCost = spacecraftNRC + spacecraftRC;

        //Integration, assembly and testing (IA&T) cost
        //This rule estimates Integration, assembly and testing non recurring and cost using SMAD CERs
        double iatNRC = 989. + spacecraftNRC * 0.215;
        double iatRC = 10.4 * s.getDryMass();
        double iatCost = iatNRC + iatRC;

        //Program overhead cost
        //This rule estimates program overhead non recurring and cost using SMAD CERs
        double progNRC = 1.963 * Math.pow(spacecraftNRC, 0.841);
        double progRC = 0.341 * spacecraftRC;
        double progCost = progRC + progNRC;

        //Operation Cost
        //This rule estimates operations cost using NASAs MOCM"
        double totalCost = (spacecraftCost + progCost + iatCost);
        totalCost *= 0.001097; //correct for inflation and transform to $M
        double penalty; //ground station penalty 
        if (Double.parseDouble(s.getProperty("sat-data-rate-per-orbit#"))
                > 5. * 60. * 700. * (1. / 8192.)) {
            penalty = 10.0;
        } else {
            penalty = 1.0;
        }
        double opsCost = (0.035308 * Math.pow(totalCost, 0.928) * lifetime); //NASA MOCM in FY04$M
        opsCost *= (1. / 0.001097) ; //back to FY00$k
        opsCost *= penalty;

        //Cost overruns
        //This rule estimates total mission cost adding an overrun which is proportional to 
        //the expected schedule slippage, which in turn is a function of the TRL of the less 
        //mature instrument in the payload
        double missionCost = (spacecraftCost + progCost + iatCost + opsCost) / 1000. + launchCost;
        missionCost *= 1 + costOverrunTRL(s.getPayload());
        
        //Mission recurring and nonrecurring costs
        double missionNRC = (busNRC + payloadNRC + progNRC + iatNRC)/1000.;
        //recurring costs include learning curve
        double missionRC = (busRC + payloadRC + progRC + iatRC + opsCost)/1000.;
        double sLearn = 0.95; //95% learning curve, means doubling N reduces average cost by 5% (See  SMAD p 809) 
        double blearn =  1 + Math.log(sLearn)/ Math.log(2);
        double lLearn = Math.pow(nsat, blearn);
        missionRC = lLearn * missionRC + launchCost;
        
        //total mission cost
        lifeCycleCost = missionNRC + missionRC;
        
       //record all costs into spacecraft
        s.setProperty("cost:payload", String.valueOf(payloadCost));
        s.setProperty("cost:payload-non-recurring", String.valueOf(payloadNRC));
        s.setProperty("cost:payload-recurring", String.valueOf(payloadRC));
        s.setProperty("cost:bus-non-recurring", String.valueOf(busNRC));
        s.setProperty("cost:bus-recurring", String.valueOf(busRC));
        s.setProperty("cost:spacecraft", String.valueOf(spacecraftCost));
        s.setProperty("cost:spacecraft-non-recurring", String.valueOf(spacecraftNRC));
        s.setProperty("cost:spacecraft-recurring", String.valueOf(spacecraftRC));
        s.setProperty("cost:IAT", String.valueOf(iatCost));
        s.setProperty("cost:IAT-non-recurring", String.valueOf(iatNRC));
        s.setProperty("cost:IAT-recurring", String.valueOf(iatRC));
        s.setProperty("cost:programmatic", String.valueOf(progCost));
        s.setProperty("cost:programmatic-non-recurring", String.valueOf(progNRC));
        s.setProperty("cost:programmatic-recurring", String.valueOf(progRC));
        s.setProperty("cost:operations", String.valueOf(opsCost));
        s.setProperty("cost:mission", String.valueOf(missionCost));
        s.setProperty("cost:mission-non-recurring", String.valueOf(missionNRC));
        s.setProperty("cost:mission-recurring", String.valueOf(missionRC));
        
        
        
        return lifeCycleCost;
    }

    private static double costOverrunTRL(Collection<Instrument> payload) {
        double minTRL = 10;
        //find the lowest TRL number
        for (Instrument inst : payload) {
            minTRL = Math.min(minTRL, Double.parseDouble(inst.getProperty("Technology-Readiness-Level")));
        }
        double rss = 8.29 * Math.exp(-0.56 * minTRL);
        return 0.017 + 0.24 * rss;
    }

    private static double payloadCost(Spacecraft s) {
        double payloadCost = 0.0;
        for (Instrument inst : s.getPayload()) {
            //check if insturment is developed domestically
            String dev = inst.getProperty("developed-by").trim();
            if (dev.startsWith("DOM")) {
                double mass = Double.parseDouble(inst.getProperty("mass#"));
                double avgPower = Double.parseDouble(inst.getProperty("characteristic-power#"));
                double avgDataRate = Double.parseDouble(inst.getProperty("average-data-rate#"));
                payloadCost += nicm(mass, avgPower, avgDataRate);
            }
        }
        return payloadCost;
    }

    /**
     * Computes the bus non recurring cost. This rule estimates bus
     * non-recurring cost using SMAD CERs
     *
     * @param s
     * @return
     */
    private static double busNonRecurringCost(Spacecraft s) {
        double cost = 0.0;
        cost += 157. * Math.pow(s.getStructure().getMass(), 0.83);
        cost += 17.8 * Math.pow(s.getPropulsion().getMass(), 0.75);
        cost += 464. * Math.pow(s.getADCS().getMass(), 0.867);
        cost += 545. * Math.pow(s.getComms().getMass(), 0.761);
        cost += 394. * Math.pow(s.getThermal().getMass(), 0.635);
        cost += 2.63 * Math.pow(s.getEPS().getMass() * s.getEPS().getPowerBOL(), 0.712);
        return cost;
    }

    /**
     * Computes the bus recurring cost. This rule estimates bus recurring cost
     * (TFU) using SMAD CERs
     *
     * @param s
     * @return
     */
    private static double busRecurringCost(Spacecraft s) {
        double cost = 0.0;
        cost += 13.1 * s.getStructure().getMass();
        cost += 4.97 * Math.pow(s.getPropulsion().getMass(), 0.823);
        cost += 293. * Math.pow(s.getADCS().getMass(), 0.777);
        cost += 653. * Math.pow(s.getComms().getMass(), 0.568);
        cost += 50.6 * Math.pow(s.getThermal().getMass(), 0.707);
        cost += 112. * Math.pow(s.getEPS().getMass(), 0.763);
        return cost;
    }

    /**
     * A very simplified version of NASA Instrument Cost Model (NICM) available
     * online
     *
     * @return the cost of the instrument
     */
    private static double nicm(double mass, double avgPower, double avgDataRate) {
        //in $FY04
        double cost = 25600. * Math.pow(avgPower / 61.5, 0.32)
                * Math.pow(mass / 53.8, 0.26)
                * Math.pow(1000. * avgDataRate / 40.4, 0.11);

        return cost / 1.097;
    }
}
