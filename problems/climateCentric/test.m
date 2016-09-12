
% javaaddpath C:\Users\SEAK2\Nozomi\EOSS\dist\EOSS.jar
javaaddpath /Users/nozomihitomi/Dropbox/EOSS/dist/EOSS.jar
import org.moeaframework.*
% path = 'C:\Users\SEAK2\Nozomi\EOSS\problems\climateCentric\result\';
path = '/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric/result/';

h1 = figure(1);

for i=1:1
% subplot(1,2,i)
clear pop
files = dir(strcat(path,'*2nd.pop'));
filename1 = strcat(path,files(randi(length(files))).name);
filename = strsplit(filename1,'2nd');
filename1 = strcat(filename{1},'2nd.pop');
popJava = org.moeaframework.core.PopulationIO.read(java.io.File(filename1));
pop = zeros(popJava.size,2);
iter = popJava.iterator;
ind = 1;
while(iter.hasNext)
    pop(ind,:) = iter.next.getObjectives;
    ind = ind + 1;
end
scatter(-pop(:,1),pop(:,2)*33495.939796,60,'b','filled','LineWidth',2)

hold on
clear pop
filename2 = strcat(filename{1},'ARC.pop');
popJava = org.moeaframework.core.PopulationIO.read(java.io.File(filename2));
pop = zeros(popJava.size,2);
iter = popJava.iterator;
ind = 1;
while(iter.hasNext)
    pop(ind,:) = iter.next.getObjectives;
    ind = ind + 1;
end
scatter(-pop(:,1),pop(:,2)*33495.939796,50,'r','LineWidth',2)

clear pop
filename2 = strcat(filename{1},'ARM.pop');
popJava = org.moeaframework.core.PopulationIO.read(java.io.File(filename2));
pop = zeros(popJava.size,2);
iter = popJava.iterator;
ind = 1;
while(iter.hasNext)
    pop(ind,:) = iter.next.getObjectives;
    ind = ind + 1;
end
scatter(-pop(:,1),pop(:,2)*33495.939796,50,'g','LineWidth',2)

axis([0,0.35,0,12000])
legend('\epsilon-MOEA','Method1','Method2','Location','NorthWest')
hold off
end
