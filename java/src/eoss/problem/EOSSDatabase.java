/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import java.util.ArrayList;

/**
 * Stores static information about the problem like the instruments and orbits.
 * This class is not threadsafe so it is assumed that the database is created
 * before any additional threads are created and that nothing is added or
 * removed by any threads throughout search
 *
 * @author nozomihitomi
 */
public final class EOSSDatabase {

    private static ArrayList<Instrument> instruments;

    private static ArrayList<Orbit> orbits;

    private static ArrayList<Bus> buses;

    private static EOSSDatabase instance;

    /**
     * Singleton pattern constructor
     */
    private EOSSDatabase() {
        EOSSDatabase.instruments = new ArrayList<>();
        EOSSDatabase.orbits = new ArrayList<>();
        EOSSDatabase.buses = new ArrayList<>();
    }

    /**
     * Returns the database instance
     *
     * @return
     */
    public static EOSSDatabase getInstance() {
        if (instance == null) {
            instance = new EOSSDatabase();
        }
        return instance;
    }

    public EOSSDatabase(ArrayList<Instrument> instruments, ArrayList<Orbit> orbits, ArrayList<Bus> buses) {
        EOSSDatabase.instruments = instruments;
        EOSSDatabase.orbits = orbits;
        EOSSDatabase.buses = buses;
    }

    public static ArrayList<Instrument> getInstruments() {
        return instruments;
    }

    public static void addInstrument(Instrument instrument) {
        EOSSDatabase.instruments.add(instrument);
    }

    public static void removeInstrument(Instrument instrument) {
        EOSSDatabase.instruments.remove(instrument);
    }

    public static ArrayList<Orbit> getOrbits() {
        return orbits;
    }

    public static void addOrbit(Orbit orbit) {
        EOSSDatabase.orbits.add(orbit);
    }

    public static void removeOrbit(Orbit orbit) {
        EOSSDatabase.orbits.remove(orbit);
    }

    public static ArrayList<Bus> getBuses() {
        return buses;
    }

    public static void addBus(Bus bus) {
        EOSSDatabase.buses.add(bus);
    }

    public static void removeBus(Bus bus) {
        EOSSDatabase.buses.remove(bus);
    }

}
