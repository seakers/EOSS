/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.spacecraft;

import eoss.problem.Orbit;
import eoss.problem.Orbits;
import java.util.Arrays;
import org.orekit.utils.Constants;

/**
 * A factory to create an attitude dynamics and control system sized to a
 * spacecraft flying in an orbit
 *
 * @author nozomihitomi
 */
public class ADCSFactory implements BusComponentFactory {

    private static final double D = 5.0; //residual dipole
    private static final double offNadir = 2.0; //slew angle
    private static final double adcsRequirement = 0.01; //adcs requirement
    private static final double q = 0.6;

    private ADCSType setType(double requirement) {
        if (requirement < 0.1) {
            return ADCSType.THREEAXIS;
        } else if (requirement < 1) {
            return ADCSType.SPINNER;
        } else {
            return ADCSType.GRAVGRADIENT;
        }
    }

    /**
     * Computes the gravity gradient disturbance torque. See SMAD page 367 Tg =
     * 3 / 2. * muE. * (1. / R. ^ 3). * (Iz - Iy). * sin(2. * theta. * Rad);
     */
    private double gravityGradientTorque(double Iy, double Iz, Orbit o, double offnadir) {
        return 1.5 * Constants.WGS84_EARTH_MU * (1 / Math.pow(o.getSemimajorAxis(), 3)) * (Iz - Iy) * Math.sin(offnadir);
    }

    /**
     * "This function computes the aerodynamic disturbance torque. See SMAD page
     * 367."
     */
    private double aerotorque(Orbit o, double Cd, double As, double cpacg) {
        double V = Orbits.velocity(o, o.getSemimajorAxis());
        double rho = Orbits.atomsphericDensity(o);
        return 0.5 * rho * As * Math.pow(V, 2) * Cd * cpacg;
    }

    /**
     * "This function computes the solar pressure disturbance torque. See SMAD
     * page 367."
     *
     * @param As
     * @param q
     * @param sun
     * @return
     */
    private double solarPressureTorque(double As, double q, Orbit o, double cpscg) {
        return (1367 / 3e8) * As * (1 + q) * Math.cos(Orbits.worstSunAngle(o)) * cpscg;
    }

    /**
     * "This function computes the magnetic field disturbance torque. See SMAD
     * page 367."
     *
     * @param D
     * @param a
     * @return
     */
    private double magneticFieldTorque(double D, Orbit o) {
        return 2 * 7.96e15 * D * Math.pow(o.getSemimajorAxis(), -3);
    }

    /**
     * "This function estimates the drag coefficient from the dimensions of the
     * satellite based on the article by Wallace et al Refinements in the
     * Determination of Satellite Drag Coefficients: Method for Resolving
     * Density Discrepancies"
     */
    private double estimateDragCoeff(double[] dimensions) {
        Arrays.sort(dimensions);
        double lOverD = dimensions[dimensions.length-1] / dimensions[0];
        if (lOverD > 3.0) {
            return 3.3;
        } else {
            return 2.2;
        }
    }

    private double computeMaxDisturbance(Orbit o, double offNadir, double Iy, double Iz, double Cd, double As, double cpacg, double cpscg, double D, double q) {
        double Tg = gravityGradientTorque(Iy, Iz, o, offNadir);
        double Ta = aerotorque(o, Cd, As, cpacg);
        double Tsp = solarPressureTorque(As, q, o, cpscg);
        double Tm = magneticFieldTorque(D, o);
        return Math.max(Math.max(Ta, Tg), Math.max(Tsp, Tm));
    }

    /**
     * This function computes the momentum storage capacity that a RW needs to
     * have to compensate for a permanent sinusoidal disturbance torque that
     * accumulates over a 1/4 period
     */
    private double computeRWMomentum(double Td, Orbit o) {
        return (1 / Math.sqrt(2)) * Td * 0.25 * Orbits.period(o);
    }

    /**
     * This function estimates the mass of a RW from its momentum storage
     * capacity. It can also be used to estimate the mass of an attitude control
     * system
     *
     * @param h
     * @return
     */
    private double estimateAttCtrlMass(double h) {
        return 1.5 * Math.pow(h, 0.6);
    }

    /**
     * This function estimates the power of a RW from its torque authority
     *
     * @param T
     * @return
     */
    private double estimateRWPower(double T) {
        return 200 * T;
    }

    /**
     * This function estimates the mass of the sensor required for attitude
     * determination from its knowledge accuracy requirement.It is based on data
     * from BAll Aerospace, Honeywell, and SMAD chapter 10 page 327
     *
     * @param acc
     * @return
     */
    private double estimateAttDetMass(double acc) {
        return 10. * Math.pow(acc, -0.316);
    }

    private double getStarTrackerMass(double req) {
        if (req < 0.1) {
            return 24.5; //Ball HAST
        } else if (req < 3) {
            return 9.5; //Ball CT-602 
        } else if (req < 5) {
            return 6.5; //Ball CT-633
        } else if (req < 52) {
            return 3.2; // Ball HAST 
        } else {
            throw new UnsupportedOperationException("No compatible star tracker found");
        }
    }

    @Override
    public BusComponent create(Spacecraft s, Orbit o, double lifetime) {
        ADCSType type = setType(adcsRequirement);
        double Iy = s.getInertiaMoments()[1];
        double Iz = s.getInertiaMoments()[2];
        double x = s.getDimensions()[0];
        double y = s.getDimensions()[1];
        double As = x * y;
        double cpscg = 0.2 * x;
        double cpacg = 0.2 * x;
        double cd = estimateDragCoeff(s.getDimensions());
        double torque = computeMaxDisturbance(o, offNadir, Iy, Iz, cd, As, cpacg, cpscg, D, q);
        double rwMomentum = computeRWMomentum(torque, o);
        double ctrlMass = estimateAttCtrlMass(rwMomentum);
        double detMass = estimateAttDetMass(adcsRequirement);
        double elMass = 4 * ctrlMass + 3 * detMass;
        double strMass = 0.01 * s.getDryMass();
        return new ADCS(type, elMass + strMass, estimateRWPower(torque));
    }

}
