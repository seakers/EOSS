/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.spacecraft;

import eoss.problem.Instrument;
import eoss.problem.Mission;
import eoss.problem.Orbit;
import eoss.problem.Orbits;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import jess.JessException;
import jess.Rete;
import org.orekit.time.AbsoluteDate;

/**
 *
 * @author nozomihitomi
 */
public class SpacecraftDesigner {

    private final ADCSFactory adcsF;

    private final CommunicationsFactory commsF;

    private final EPSFactory epsF;

    private final LaunchAdapterFactory adapterF;

    private final PropulsionFactory propF;

    private final StructureFactory structF;

    private final ThermalFactory thermF;

    public SpacecraftDesigner() {
        this.adcsF = new ADCSFactory();
        this.commsF = new CommunicationsFactory();
        this.epsF = new EPSFactory();
        this.adapterF = new LaunchAdapterFactory();
        this.propF = new PropulsionFactory();
        this.structF = new StructureFactory();
        this.thermF = new ThermalFactory();
    }

    /**
     * Designs the spacecraft within the given mission
     *
     * @param m
     */
    public void designSpacecraft(Mission m) {
        for (Spacecraft s : m.getSpacecraft().keySet()) {
            designSpacecraft(s, m.getSpacecraft().get(s), m.getLifetime());
        }
    }

    /**
     * Designs the spacecraft given the intended orbit and the lifetime of the
     * spacecraft
     *
     * @param s spacecraft to design
     * @param o orbit intended for the spacecraft
     * @param lifetime lifetime of the mission
     */
    public void designSpacecraft(Spacecraft s, Orbit o, double lifetime) {
        //preliminary estimate
        //Estimate the dry mass of the bus as a linear function of the payload mass.
        //Use a factor 4.0 from TDRS 7, in new SMAD page 952.
        double busMass = 4 * s.getPayloadMass();

        //Estimate dry mass as the sum of bus and payload mass
        //(including antennae mass).
        double oldDryMass = (s.getPayloadMass() + busMass);

        //Estimate dimensions assuming a perfect cube of size given 
        //by average density, see SMAD page 337
        double[] dimensions = new double[3];
        Arrays.fill(dimensions, 0.25 * Math.pow(oldDryMass, 1. / 3.));
        s.setDimensions(dimensions);

        //Guess moments of inertia assuming a perfect box
        s.setInertiaMoments(boxMomentOfInertia(oldDryMass, s.getDimensions()));

        double diff = 0;
        double tolerance = 10; //in kg
        boolean converged = false;
        while (!converged) {
            //clear out all bus components
            clearBusComponents(s);

            //set dummy bus component to satellite so that we can compute dry mass
            Structure dummy = new Structure(oldDryMass - s.getPayloadMass(), 0.0);
            s.setStructure(dummy);

            //re-estimate components on new info
            setBusComponents(s, o, lifetime);

            //reset spacecraft parameters
            s.setDimensions(null);
            s.setInertiaMoments(null);

            //update moments of inertia
            double La = 1.5 * dimensions[0] + 0.5 * Math.sqrt(0.5 * s.getEPS().getSolarArrayArea());
            double Ix = 0.01 * Math.pow(s.getDryMass(), 5 / 3);
            double Iz = Ix + s.getEPS().getSolarArrayMass() * Math.pow(La, 2);
            s.setInertiaMoments(new double[]{Ix, Ix, Iz});

            //update spacecraft dimensions
            //Estimate dimensions assuming a perfect cube of size given by average density
            double r = 0.25 * Math.pow(s.getDryMass(), 1. / 3.);
            s.setDimensions(new double[]{r, r, r});

            diff = Math.abs(oldDryMass - s.getDryMass());
            converged = diff < tolerance;
            oldDryMass = s.getDryMass();
        }

        // assuming 1 seven minute pass at 500Mbps max
        double perOrbit = (s.getDataRate() * 1.2 * Orbits.period(o)) / (1024 * 8); //(GByte/orbit) 20% overhead
        double drdc = (1. * 7. * 60. * 500. * (1. / 8192.)) / perOrbit;
        s.setProperty("data-rate duty cycle", Double.toString(drdc));

        // Computes the power duty cycle assuming a limit at 10kW
        double pdc = 10000.0 / s.getEPS().getPowerBOL();
        s.setProperty("power duty cycle", Double.toString(pdc));

        double dutycycle = Math.min(drdc, pdc);
        s.setProperty("duty cycle", Double.toString(dutycycle));
    }

    /**
     * Converts the spacecraft properties to a new Jess Fact and asserts it into
     * the working memory
     *
     * @param r
     * @param s
     * @param o
     * @param name
     * @param lifetime
     * @param launchDate
     * @throws jess.JessException
     */
    public void assertFact(Rete r, Spacecraft s, Orbit o, String name, double lifetime, AbsoluteDate launchDate) throws JessException {
        StringBuilder missFact = new StringBuilder();
        if (s.getPayload().size() > 0) {
            StringBuilder payload = new StringBuilder();
            double dataRate = 0;
            ArrayList<Double> payloadDimensions = new ArrayList<>();
            payloadDimensions.add(0, 0.0); //max dimension in x, y, and z
            payloadDimensions.add(1, 0.0); //nadir-area
            payloadDimensions.add(2, 0.0); //max z dimension
            String fliesIn = name + ":" + o;
            missFact.append("(assert (MANIFEST::Mission (Name ").append(fliesIn).append(") ");
            for (Instrument inst : s.getPayload()) {
                payload.append(inst.getName()).append(" ");
                dataRate += Double.parseDouble(inst.getProperty("average-data-rate#"));
                double dx = Double.parseDouble(inst.getProperty("dimension-x#"));
                double dy = Double.parseDouble(inst.getProperty("dimension-y#"));
                double dz = Double.parseDouble(inst.getProperty("dimension-z#"));
                payloadDimensions.set(0, Math.max(payloadDimensions.get(0), Math.max(Math.max(dx, dy), dz)));
                payloadDimensions.set(1, payloadDimensions.get(1) + dx * dy);
                payloadDimensions.set(2, Math.max(payloadDimensions.get(2), dz));

                //manifest the instrument
                StringBuilder instFact = new StringBuilder();
                instFact.append("(assert (CAPABILITIES::Manifested-instrument ");
                instFact.append("(Name ").append(inst.getName()).append(")");
                Iterator iter = inst.getProperties().iterator();
                while (iter.hasNext()) {
                    String propertyName = (String) iter.next();
                    instFact.append("(").append(propertyName).append(" ").append(inst.getProperty(propertyName)).append(")");
                }
                instFact.append("(flies-in ").append(fliesIn).append(")");
                instFact.append("(orbit-altitude# ").append((int) o.getAltitude()).append(")");
                instFact.append("(orbit-inclination ").append(o.getInclination()).append(")");
                instFact.append("))");
                r.eval(instFact.toString());
            }
            missFact.append("(instruments ").append(payload.toString()).append(")");
            missFact.append("(launch-date ").append(launchDate).append(")");
            missFact.append("(lifetime ").append(lifetime).append(")");
            missFact.append("(select-orbit no) ").append(o.toJessSlots());
            missFact.append("(payload-mass# ").append(s.getPayloadMass()).append(")");
            missFact.append("(payload-power# ").append(s.getPayloadAvgPower()).append(")");
            missFact.append("(payload-peak-power# ").append(s.getPayloadAvgPower()).append(")");
            missFact.append("(payload-data-rate# ").append(dataRate).append(")");
            double perOrbit = (dataRate * 1.2 * Orbits.period(o)) / (1024 * 8); //(GByte/orbit) 20% overhead
            missFact.append("(payload-dimensions# ").append(payloadDimensions.get(0)).append(" ").append(payloadDimensions.get(1)).append(" ").append(payloadDimensions.get(2)).append(")");
            missFact.append("(sat-data-rate-per-orbit# ").append(perOrbit).append(")");
            missFact.append("(num-of-sats-per-plane# 1)))"); //should get rid of this attribute
            missFact.append("(assert (SYNERGY::cross-registered-instruments (instruments ").
                    append(payload.toString()).append(") (degree-of-cross-registration spacecraft) ").
                    append(" (platform ").append(o).append(" )))");
            r.eval(missFact.toString());

        }

    }

    private void setBusComponents(Spacecraft s, Orbit o, double lifetime) {
        //size the bus components before altering the spacecraft and change its dry mass
        ADCS adcs = (ADCS) adcsF.create(s, o, lifetime);
        ADCS dummyAdcs = new ADCS(adcs.getType(), 0.0, 0.0);
        s.setADCS(dummyAdcs); //propulsion factory needs adcs type
        LaunchAdapter lvadapter = (LaunchAdapter) adapterF.create(s, o, lifetime);
        Communications comms = (Communications) commsF.create(s, o, lifetime);
        EPS eps = (EPS) epsF.create(s, o, lifetime);
        Propulsion prop = (Propulsion) propF.create(s, o, lifetime);
        Structure struct = (Structure) structF.create(s, o, lifetime);
        Thermal therm = (Thermal) thermF.create(s, o, lifetime);

        //set all bus components
        s.setADCS(adcs);
        s.setAdapter(lvadapter);
        s.setCDH(null);
        s.setComms(comms);
        s.setEPS(eps);
        s.setGNC(null);
        s.setPropulsion(prop);
        s.setStructure(struct);
        s.setThermal(therm);
    }

    private double momentOfInertia(double k, double m, double r) {
        return k * m * Math.pow(r, 2);
    }

    /**
     *
     * @param m mass of the cube body in kg
     * @param dims the dimension of the rectangular body [x,y,z]
     * @return
     */
    private double[] boxMomentOfInertia(double m, double[] dim) {
        double[] moments = new double[3];
        for (int i = 0; i < 3; i++) {
            moments[i] = momentOfInertia(1. / 6., m, dim[i]);
        }
        return moments;
    }

    private void clearBusComponents(Spacecraft s) {
        s.setADCS(null);
        s.setAdapter(null);
        s.setCDH(null);
        s.setComms(null);
        s.setEPS(null);
        s.setGNC(null);
        s.setPropulsion(null);
        s.setStructure(null);
        s.setThermal(null);

    }

}
