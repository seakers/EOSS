/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import org.hipparchus.util.FastMath;
import org.orekit.utils.Constants;

/**
 *
 * @author nozomihitomi
 */
public class Orbits {

    /**
     * Computes the inclination in degrees of a satellite in a sunsynchronous
     * orbit. Assumes circular orbit
     *
     * @param o the orbit
     * @return
     */
    public static double ssoInclination(Orbit o) {
        double kh = 10.10949;
        double cos_i = Math.pow(o.getSemimajorAxis() / Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 3.5) / (-kh);
        return 180. * Math.acos(cos_i) / Math.PI;
    }

    /**
     * Gets the period of the orbit in seconds
     *
     * @param o the orbit
     * @return
     */
    public static double period(Orbit o) {
        return 2 * FastMath.PI * FastMath.sqrt(FastMath.pow(o.getSemimajorAxis(), 3.) / Constants.WGS84_EARTH_MU);
    }

    /**
     * Estimate fraction of sunlight based on circular orbit
     *
     * @param o the orbit
     * @return
     */
    public static double fractionSunlight(Orbit o) {

        if (o.getSemimajorAxis() < 7000000.) {
            //rho is Earth subtended angle
            double rho = Math.asin(Constants.WGS84_EARTH_EQUATORIAL_RADIUS / o.getSemimajorAxis());
            double Bs = 25.;
            double phi = 2. * Math.acos(Math.cos(rho) / Math.cos(Bs));
            return 1. - phi / (2*Math.PI);
        } else {
            return 0.99;
        }
    }

    /**
     * The velocity of the spacecraft at a given distance from the main
     * gravitational body. Uses Vis-a-vis equation
     *
     * @param o the orbit
     * @param r the distance from the gravitational body
     * @return
     */
    public static double velocity(Orbit o, double r) {
        return Math.sqrt(Constants.WGS84_EARTH_MU * (2./o.getSemimajorAxis() - 1./r));
    }

    /**
     * Estimates the worst sun angle.
     *
     * @param o
     * @return
     */
    public static double worstSunAngle(Orbit o) {
        return 0.0;
    }

    /**
     * Calculates rho in kg/m^3 as a function of altitude
     *
     * @param o the orbit
     * @return
     */
    public static double atomsphericDensity(Orbit o) {
        return 1e-5 * Math.exp((o.getAltitude() - 85.) / -33.387);
    }

    /**
     * This function returns the angle in degrees subtended by the Earth from
     * the orbit
     *
     * @param o the orbit
     * @return the angle in degrees subtended by the Earth from
     * the orbit
     */
    public static double earthSubtendAngle(Orbit o) {
        return 2 * (180/Math.PI)*
                Math.asin(Constants.WGS84_EARTH_EQUATORIAL_RADIUS/o.getSemimajorAxis());

    }
}
