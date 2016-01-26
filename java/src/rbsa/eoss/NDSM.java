/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rbsa.eoss;

/**
 *
 * @author Ana-Dani
 */
import java.util.HashMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class NDSM implements Serializable {
    private static final long serialVersionUID = -7003149292546882129L;

    //private double[][] matrix;
    private String[] elements;
    private int numel;
    private HashMap<Nto1pair,Double> map;
    private HashMap<String,Integer> indices;
    private String description;

    public NDSM(String[] el,String desc) {
        elements = el;
        numel = el.length;
        map = new HashMap<Nto1pair,Double>();
        indices = new  HashMap<String,Integer>();
        for(int i = 0;i<numel;i++) {
            indices.put(el[i],i);
    }
        description = desc;
    }

    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setInteraction(String[] el1, String el2, double x) {
        Nto1pair key = new Nto1pair(el1,el2);
        if(!map.containsKey(key)) {
            map.put(key,new Double(x));
        }
    }
    public Double getInteraction(String[] el1,String el2) {
        return map.get(new Nto1pair(el1,el2));
    }
    public String printAllInteractions() {
        String ret = "";
        for (Nto1pair key : map.keySet()) {
            Double val = map.get(key);
            if (val!=0.0) {
                System.out.println(key.toString()  + " : " + val);
            }
        }
        return ret;
    }

    /**
     *
     * @param operator "+" for filter in keeping positive values. "0" filter for keeping 0 values. "-" for keeping negative values
     * @return an sorted Treeset of interactions in ascending order of the interaction values
     */
    public TreeSet<Interaction> getAllInteractions(String operator) {
        HashSet<Interaction> unsorted_map = new HashSet<>();
        for (Nto1pair key : map.keySet()) {
            double val = map.get(key);
            if ((val==0.0 && operator.equalsIgnoreCase("0")) || (val>0.0 && operator.equalsIgnoreCase("+")) || (val<0.0 && operator.equalsIgnoreCase("-"))) {
                unsorted_map.add(new Interaction(key,val));
            }
        }
        TreeSet<Interaction> out = new TreeSet(new InteractionComparator());
        out.addAll(unsorted_map);
        return out;
    }


    public String[] getElements() {
        return elements;
    }

    public void setElements(String[] elements) {
        this.elements = elements;
    }
    
    public int getNumel() {
        return numel;
        }

    public void setNumel(int numel) {
        this.numel = numel;
    }

    public HashMap<Nto1pair, Double> getMap() {
        return map;
}

    public void setMap(HashMap<Nto1pair, Double> map) {
        this.map = map;
    }

    public HashMap<String, Integer> getIndices() {
        return indices;
    }

    public void setIndices(HashMap<String, Integer> indices) {
        this.indices = indices;
    }
    
}


