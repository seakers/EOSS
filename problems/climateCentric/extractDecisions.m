
% javaaddpath C:\Users\SEAK2\Nozomi\EOSS\dist\EOSS.jar
javaaddpath /Users/nozomihitomi/Dropbox/EOSS/dist/EOSS.jar
import org.moeaframework.*
% path = 'C:\Users\SEAK2\Nozomi\EOSS\problems\climateCentric\result\';
path = '/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric/result/';

h1 = figure(1);

clear pop
files = dir(strcat(path,'AIAA SciTech',filesep,'HeMOEA_AdaptivePursuit_SI-A_1ptC+BitM*allpop.pop'));
for j=1:length(files)
    filename2 = strcat(path,'AIAA SciTech',filesep,files(j).name);
    popJava = org.moeaframework.core.PopulationIO.read(java.io.File(filename2));
    pop = zeros(popJava.size,2);
    dec = zeros(popJava.size,60);
    
    iter = popJava.iterator;
    ind = 1;
    while(iter.hasNext)
        soln = iter.next;
        pop(ind,:) = soln.getObjectives;
        for i=1:soln.getVariable(1).getNumberOfBits;
            if soln.getVariable(1).get(i-1)
                dec(ind,i) = 1;
            end
        end
        ind = ind + 1;
    end
    
    obj = pop;
    stringName = strsplit(files(j).name,'.');
    saveStr = stringName{1};
    
    save(saveStr, 'obj', 'dec');
    
    % scatter(-pop(:,1),pop(:,2)*33495.939796,'b')
    %
    % axis([0,0.35,0,12000])
    
end