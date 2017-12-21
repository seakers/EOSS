/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.spacecraft;

import eoss.problem.Orbit;
import eoss.problem.Orbits;
import org.orekit.utils.Constants;

/**
 *
 * @author nozomihitomi
 */
public class PropulsionFactory implements BusComponentFactory {

    /**
     * mprop = mwet*(1-exp(-dV/V0))
     *
     * @param dV
     * @param Isp
     * @param mi
     * @return
     */
    private double rocketEQmidVtomp(double dV, double Isp, double mi) {
        return mi * (1 - Math.exp(dV / (-9.81 * Isp)));
    }

    /**
     * mprop = mdry*(-1+exp(dV/V0))
     *
     * @param dV
     * @param Isp
     * @param mf
     * @return
     */
    private double rocketEQmfdVtomp(double dV, double Isp, double mf) {
        return mf * (-1 + Math.exp(dV / (9.81 * Isp)));
    }

    /**
     * Computes dry AKM mass using rules of thumb: 94% of wet AKM mass is
     * propellant, 6% motor
     *
     * @param propulsionMassInjection
     * @return
     */
    private double designPropulsionAKM(double propulsionMassInjection) {
        return propulsionMassInjection * (6. / 94.);
    }

    /**
     * {@inheritDoc} Sizing the propulsion subsystem requires information from
     * the ADCS so adcs must be set on spacecraft first
     *
     * @param s
     * @param o
     * @param lifetime
     * @return
     * @throws IllegalStateException if Spacecraft is missing ADCS subsystem
     */
    @Override
    public BusComponent create(Spacecraft s, Orbit o, double lifetime) {
        try {
            s.getADCS().getType();
        } catch (NullPointerException ex) {
            throw new IllegalStateException("Spacecraft is missing ADCS subsystem. Must set that first");
        }

        //This rule computes the propellant mass necessary for the DeltaV using the rocket equation and assuming a certain Isp.
        double dvInject = computeDeltaVInjection(o);
        double dvADCS = computeDeltaVADCS(s.getADCS().getType(), lifetime);
        double dvDrag = computeDeltaVDrag(o, lifetime);
        DeorbitStrategy deorbit = setDeorbitingMode(o);
        double dvDeorbit = computeDeltaVDeorbit(s, o, deorbit);
        double dV = dvInject + dvADCS + dvDrag + dvDeorbit;

        Propellant propellantI = selectPropellant(s, o);
        Propellant propellantA = selectPropellant(s, o);
        double mpinj = rocketEQmfdVtomp(dvInject, propellantI.getIsp(), s.getDryMass());
        double mpa = rocketEQmfdVtomp(dV - dvInject, propellantA.getIsp(), s.getDryMass());
        return new Propulsion(designPropulsionAKM(mpinj), 0, propellantI, mpinj, propellantA, mpa, deorbit);
    }

    private Propellant selectPropellant(Spacecraft s, Orbit o) {
        return Propellant.HYDRAZINE;
    }

    /**
     * "This rule computes the delta-V required for injection for GEO or MEO
     * assuming a transfer orbit with a perigee of 150km and an apogee at the
     * desired orbit, as suggested in De Weck's paper found in
     * http://strategic.mit.edu/docs/2_3_JSR_parametric_NGSO.pdf. For LEO/SSO,
     * no injection is required."
     *
     * @return
     */
    private double computeDeltaVInjection(Orbit o) {
        if (o.getType() == Orbit.OrbitType.GEO
                || o.getType() == Orbit.OrbitType.MEO) {
            return computeDVInjection(Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 150000, o);
        } else {
            return 0.0;
        }
    }

    /**
     * This rule computes the delta-V required to overcome drag. The data comes
     * from De Weck's paper found in
     * http://strategic.mit.edu/docs/2_3_JSR_parametric_NGSO.pdf
     *
     * @return
     */
    private double computeDeltaVDrag(Orbit o, double lifetime) {
        double hp = ((o.getSemimajorAxis() * (1 - o.getEccentricity()))
                - Constants.WGS84_EARTH_EQUATORIAL_RADIUS) / 1000;// dV for station-keeping(m/s/yr)
        double dv;
        if (hp < 500) {
            dv = 12;
        } else if (hp < 600) {
            dv = 5;
        } else if (hp < 1000) {
            dv = 2;
        } else {
            dv = 0;
        }
        return dv * lifetime;
    }

    /**
     * This rule computes the delta-V required for attitude control. The data
     * comes from De Weck's paper found in
     * http://strategic.mit.edu/docs/2_3_JSR_parametric_NGSO.pdf
     *
     * @param adcstype
     * @param lifetime
     * @return
     */
    private double computeDeltaVADCS(ADCSType adcstype, double lifetime) {
        double dv = Double.NaN;
        switch (adcstype) {
            case THREEAXIS:
                dv = 20.;
                break;
            case GRAVGRADIENT:
                dv = 0.;
                break;
            case SPINNER:
                dv = 0.;
                break;
            default:
                throw new UnsupportedOperationException(String.format("ADCS type %s unknown", adcstype));
        }
        return dv * lifetime;
    }

    private DeorbitStrategy setDeorbitingMode(Orbit o) {
        if (o.getType() == Orbit.OrbitType.LEO || o.getType() == Orbit.OrbitType.SSO) {
            return DeorbitStrategy.DRAG; // sets the deorbiting mode to drag-based
        } else if (o.getType() == Orbit.OrbitType.GEO || o.getType() == Orbit.OrbitType.MEO) {
            return DeorbitStrategy.GRAVEYARD; //sets the deorbiting mode to graveyard
        } else {
            return DeorbitStrategy.NONE;
        }
    }

    private double computeDeltaVDeorbit(Spacecraft s, Orbit o, DeorbitStrategy deorbit) {
        switch (deorbit) {
            case NONE:
                return 0;
            case DRAG:
                //"Computes the delta-V required for deorbiting assuming a change of semimajor axis so that the perigee is the surface of the earth
                return computeDVDeorbit(o, Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
            case GRAVEYARD:
                //"This rule computes the delta-V required for deorbiting to a graveyard orbit. 
                //This is calculated as a change of semimajor axis to raise perigee by a certain amount
                //given in http://www.iadc-online.org/Documents/IADC-UNCOPUOS-final.pdf"
                double b = s.getDimensions()[0] * s.getDimensions()[1];
                return computeDVGraveyard(o, 1.5, b, s.getDryMass());
            default:
                throw new UnsupportedOperationException(String.format("Deorbit strategy %s not recognized", deorbit));
        }
    }

    private double computeDV(double rp1, double ra1, double rp2, double ra2, Orbit o) {
        double a1 = (rp1 + ra1) / 2.;
        double a2 = (rp2 + ra2) / 2.;
        return Math.abs(Orbits.velocity(o, a2) - Orbits.velocity(o, a1));
    }

    /**
     * From elliptical orbit with perigee ainj and apogee a2 to circular orbit
     * a2 burn at current apogee a2
     *
     * @param rp1
     * @param ra1
     * @param rp2
     * @param ra2
     * @param o
     * @return
     */
    private double computeDVInjection(double ainj, Orbit o) {
        double a2 = o.getSemimajorAxis();
        return computeDV(ainj, a2, a2, a2, o);
    }

    /**
     * from circular orbit o to elliptical orbit perigee ?adeorbit apogee ?a
     * burn at future apogee ?a
     */
    private double computeDVDeorbit(Orbit o, double adeorbit) {

        double a = o.getSemimajorAxis();
        return computeDV(a, a, adeorbit, a, o);
    }

    private double computeDVGraveyard(Orbit o, double cr, double b, double m) {
        double dh = 235000. + 1.e6 * cr * (b / m);
        double a = o.getSemimajorAxis();
        return computeDV(a, a, a, a + dh, o);
    }
}
