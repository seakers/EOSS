/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package architecture.util;

import java.util.HashMap;

/**
 * Map for what the fuzzy values are interpreted as.
 *
 * @author nozomihitomi
 */
public final class ValueMap {

    private final static HashMap<String, Interval> valueInterval = new HashMap<>();
    private final static HashMap<Interval, String> intervalValue = new HashMap<>();

    private static ValueMap instance;
    
    private ValueMap() {        
        ValueMap.addInterval("Full", new Interval("interval", 1.0, 1.0));
        ValueMap.addInterval("Most", new Interval("interval", 0.66, 1.0));
        ValueMap.addInterval("Some", new Interval("interval", 0.33, 0.66));
        ValueMap.addInterval("Marginal", new Interval("interval", 0.0, 0.33));
    }

    /**
     * Adds the name of the interval and the value of the interval to the value
     * map
     *
     * @param name
     * @param interval
     */
    public static void addInterval(String name, Interval interval) {
        ValueMap.valueInterval.put(name, interval);
        ValueMap.intervalValue.put(interval, name);
    }

    /**
     * Gets the HashMap<String, Interval> that describes this value map
     *
     * @return
     */
    public static HashMap<String, Interval> getValueInterval() {
        if(ValueMap.instance == null){
            ValueMap.instance = new ValueMap();
        }
        return valueInterval;
    }

    /**
     * Gets the HashMap<Interval, String> that describes this value map
     *
     * @return
     */
    public static HashMap<Interval, String> getIntervalValue() {
        if(ValueMap.instance == null){
            ValueMap.instance = new ValueMap();
        }
        return intervalValue;
    }
}
