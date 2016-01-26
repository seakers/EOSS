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
import java.util.TreeMap;

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
     * @param ascending "+" for returning list in ascending order. "-"for returning list in descending order
     * @return 
     */
    public TreeMap<Nto1pair,Double> getAllInteractions(String operator,String ascending) {
        HashMap<Nto1pair,Double> unsorted_map = new HashMap<Nto1pair,Double>();
        ValueComparator2 bvc =  new ValueComparator2(map,ascending);
        TreeMap<Nto1pair,Double> sorted_map = new TreeMap<Nto1pair,Double>(bvc);
        
        for (Nto1pair key : map.keySet()) {
            Double val = map.get(key);
            if ((val==0.0 && operator.equalsIgnoreCase("0")) || (val>0.0 && operator.equalsIgnoreCase("+")) || (val<0.0 && operator.equalsIgnoreCase("-"))) {
                unsorted_map.put(key,val);
            }
        }
        sorted_map.putAll(unsorted_map);
        return sorted_map;
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


