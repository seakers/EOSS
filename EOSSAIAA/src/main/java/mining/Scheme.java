package mining;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */



import java.util.ArrayList;



/**
 *
 * @author Bang
 */
public class Scheme{
    
    private String name;//present, absent, inOrbit, notInOrbit, together2, togetherInOrbit2, 
    //separete2, together3, togetherInOrbit3, separete3, emptyOrbit, numOrbitUsed
    private int instrument=-1;
    private int orbit=-1;
    private int instrument2=-1, instrument3=-1;
    private int count=-1;
//    private ArrayList<String> presetFeatureNames;
    int tmpcnt;
    
    public Scheme(){
        tmpcnt = 0;
    }
    

    public int compare(Object o1) {
        
    	 int[][] data = (int[][]) o1;
        
        if (name.equals("present")) {
            for (int i = 0; i < data.length; ++i) {
                if (data[i][instrument] == 1){
                    return 1;
                }
            }
            return 0;            
        }
        else if (name.equals("absent")) {
            for (int i = 0; i < data.length; ++i) {
                if (data[i][instrument] == 1) return 0;
            }
            return 1;  
        }
        else if (name.equals("inOrbit")) {
            if (data[orbit][instrument] == 1) {return 1;}
            return 0;            
        }
        else if (name.equals("notInOrbit")) {
            if (data[orbit][instrument] == 1) return 0;
            return 1;            
        }
        else if (name.equals("together2")) {
            int together = 0;
            for (int i = 0; i < data.length && together == 0; ++i) {
                if (data[i][instrument] == data[i][instrument2] && data[i][instrument] == 1) together = 1;
            }
            return together;            
        }
        else if (name.equals("togetherInOrbit2")) {
            int together = 0;
            if (data[orbit][instrument] == 1 && data[orbit][instrument2] == 1) together = 1;
            return together;            
        }
        else if (name.equals("separate2")) {
            int separate = 1;
            for (int i = 0; i < data.length && separate == 1; ++i) {
                if (data[i][instrument] == 1 && data[i][instrument2] == 1) separate = 0;
            }            
            return separate;            
        }
        else if (name.equals("together3")) {
            int together = 0;
            for (int i = 0; i < data.length && together == 0; ++i) {
                if (1 == data[i][instrument2] && data[i][instrument] == 1 && data[i][instrument3] == 1) together = 1;
            }
            return together;            
        }
        else if (name.equals("togetherInOrbit3")) {
            int together = 0;
            if (data[orbit][instrument] == 1 && data[orbit][instrument2] == 1 && data[orbit][instrument3] == 1) together = 1;
            return together;            
        }
        else if (name.equals("separate3")) {
            int separate = 1;
            for (int i = 0; i < data.length && separate == 1; ++i) {
                if ((data[i][instrument] == 1 && data[i][instrument2] == 1) || 
                        (data[i][instrument] == 1 && data[i][instrument3] == 1) ||
                         (data[i][instrument2] == 1 && data[i][instrument3] == 1)) separate = 0;
            }            
            return separate;            
        }
        else if (name.equals("emptyOrbit")) {
            for (int i = 0; i < data[0].length; ++i) {
                if (data[orbit][i] == 1) return 0;
            }
            return 1;
        }
        else if (name.equals("numOrbitUsed")) {
            int cnt = 0;
            for (int i = 0; i < data.length; ++i) {
               boolean empty= true;
               for (int j=0; j< data[i].length;j++){
                   if(data[i][j]==1){
                       empty= false;
                   }
               }
               if(empty==true) cnt++;
            }
            if (count == data.length - cnt) return 1;
            return 0;
        }
        else if (name.equals("numInstruments")){
            int cnt = 0;
            for (int o = 0; o < data.length; ++o) {
				for (int i=0; i< data[0].length;i++){
					if (instrument==-1){ // Count all instruments
						if(data[o][i]==1){
							cnt++;
						}
					}else{ // Count specific instrument
						if(i == instrument && data[o][i]==1){
							cnt++;
						}
					}
				}
            }
            if (count == cnt) return 1;
            return 0;
        }
       
        return 0;
    }
     
    public void resetArg(){
        name = "";
        instrument=-1;
        orbit=-1;
        instrument2=-1;
        instrument3=-1;
        count=-1;
    }


    
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getInstrument() {
        return instrument;
    }

    public void setInstrument(int instrument) {
        this.instrument = instrument;
    }

    public int getOrbit() {
        return orbit;
    }

    public void setOrbit(int orbit) {
        this.orbit = orbit;
    }

    public int getInstrument2() {
        return instrument2;
    }

    public void setInstrument2(int instrument2) {
        this.instrument2 = instrument2;
    }

    public int getInstrument3() {
        return instrument3;
    }

    public void setInstrument3(int instrument3) {
        this.instrument3 = instrument3;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int number) {
        this.count = number;
    }
    
    


    public boolean presetFilter(String filterName, int[][] data, ArrayList<String> params_input){
    	
        Scheme s = new Scheme();
        
        String name = filterName;
//        System.out.println(name);
        int[] params = new int[4];
        for(int i=0;i<params_input.size();i++){
//        	System.out.println(params_input.get(i));
        	params[i] = Integer.parseInt(params_input.get(i));
        }
        
        s.setName(name);
        if (name.equals("present") || name.equals("absent")) {
        	s.setInstrument (params[0]);
        }
        else if (name.equals("inOrbit") || name.equals("notInOrbit")) {
        	s.setOrbit(params[0]);  
        	s.setInstrument (params[1]);         
        }
        else if (name.equals("together2")|| name.equals("separate2")) {
        	s.setInstrument (params[0]); 
        	s.setInstrument2 (params[1]);                
        }
        else if (name.equals("togetherInOrbit2")) {
        	s.setOrbit(params[0]);  
        	s.setInstrument (params[1]); 
        	s.setInstrument2 (params[2]);               
        }
        else if (name.equals("together3") || name.equals("separate3")) {
        	s.setInstrument (params[0]); 
        	s.setInstrument2 (params[1]);    
        	s.setInstrument3 (params[2]);
        }
        else if (name.equals("togetherInOrbit3")) {
        	s.setOrbit (params[0]); 
        	s.setInstrument (params[1]);    
        	s.setInstrument2 (params[2]);        
        	s.setInstrument3 (params[3]);  
        }
        else if (name.equals("emptyOrbit")) {
        	s.setOrbit(params[0]);
        }
        else if (name.equals("numOrbitUsed")) {
        	s.setCount(params[0]);
        }
        else if (name.equals("numInstruments")){
        	s.setInstrument(params[0]);
        	s.setCount(params[1]);
        }
        
        return s.compare(data) == 1;
	}


    
    
}