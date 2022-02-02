/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.spacecraft;

import eoss.problem.Orbit;
import eoss.problem.Orbits;

/**
 * Factory to create the electrical and power subsystem sized for a specific
 * spacecraft flying in an orbit
 *
 * @author nozomihitomi
 */
public class EPSFactory implements BusComponentFactory{

    //Constants used to size the EPS
    private static final double Xe = 0.65; // efficiency from solar arrays to battery to equipment (SMAD page 415)
    private static final double Xd = 0.85; // efficiency from solar arrays to equipment (SMAD page 415)
    private static final double P0 = 253.; // in W/m2, corresponds to GaAs, see SMAD page 412,
    private static final double Id = 0.77; // See SMAD page 412
    private static final double degradation = 0.0275; // Degradation of solar arrays performance in // per year, corresponds to multi-junction
    private static final double Spec_power_SA = 25.; // in W per kg see SMAD chapter 11
    private static final double n = 0.9; // Efficiency of battery to load (SMAD page 422).
    private static final double Spec_energy_density_batt = 40.; // In Whr/kg see SMAD page 420, corresponds to Ni-H2

    /**
     * see SMAD Page 422 "This function estimates the depth of discharge of an
     * orbit". TODO need to compute depth of discharge as a function of
     */
    private double depthOfDischarge(Orbit orbit) {
        switch (orbit.getType()) {
            case GEO:
                return 0.8;
            case SSO:
                if (orbit.getRAAN().equalsIgnoreCase("DD")) {
                    return 0.6;
                }
            default:
                return 0.4;
        }
    }

    @Override
    public BusComponent create(Spacecraft s, Orbit o, double lifetime) {
        // design the solar arrays
        double avgPower = s.getPayloadAvgPower() / 0.4; // 0.3 SMAD page 340 to take into account bus power
        double peakPower = s.getPayloadPeakPower() / 0.4;  // 0.3 SMAD page 340 to take into account bus power

        double Pd = (avgPower * 0.8) + (peakPower * 0.2);
        double Pe = Pd;
        double Td = Orbits.period(o) * Orbits.fractionSunlight(o);
        double Te = Orbits.period(o) - Td;

        //What we need in terms of power from the solar arrays
        double Psa = (Pe * (Te / Xe) + Pd * (Td / Xd)) / Td;

        //What the SA technology can give
        double theta = Math.PI / 180. * Orbits.worstSunAngle(o); // Worst case Sun angle
        double P_density_BOL = Math.abs(P0 * Id * Math.cos(theta));
        double Ld = Math.pow(1 - degradation, lifetime);
        double P_density_EOL = P_density_BOL * Ld;

        //Surface required
        double Asa = Psa / P_density_EOL;

        //Power at BOL
        double P_BOL = P_density_BOL * Asa;

        //Mass of the SA
        double mass_SA = P_BOL / Spec_power_SA;// 1kg per 25W at BOL (See SMAD chapter 10).

        //Batteries
        double Cr = (Pe * Te) / (3600. * depthOfDischarge(o) * n);//because period is in seconds
        double mass_batt = Cr / Spec_energy_density_batt;

        //Others regulators, converters, wiring
        double mass_others = (0.02 + 0.0125) * P_BOL + 0.02 * s.getDryMass();//SMAD page 334, assume all the power is regulated and half is converted.

        return new EPS(Asa, mass_SA, mass_batt, mass_others, P_BOL);
    }
}
