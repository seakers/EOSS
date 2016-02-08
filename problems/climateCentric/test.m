
javaaddpath C:\Users\SEAK2\Nozomi\EOSS\dist\EOSS.jar
import org.moeaframework.*
path = 'C:\Users\SEAK2\Nozomi\EOSS\problems\climateCentric\result\';

h1 = figure(1);

for i=1:3
subplot(1,3,i)
clear pop
files = dir('EpsilonMOEA_*.pop');
filename1 = strcat(path,files(randi(length(files))).name);
popJava = org.moeaframework.core.PopulationIO.read(java.io.File(filename1));
pop = zeros(popJava.size,2);
iter = popJava.iterator;
ind = 1;
while(iter.hasNext)
    pop(ind,:) = iter.next.getObjectives;
    ind = ind + 1;
end
% obj = dlmread(files(randi(length(files))).name);
scatter(-pop(:,1),pop(:,2)*33495.939796,'filled')
hold on
clear pop
files = dir('HeMOEA_AdaptivePursuit_SI-PF_moreCrossNoInter10*.pop');
filename2 = strcat(path,files(randi(length(files))).name);
popJava = org.moeaframework.core.PopulationIO.read(java.io.File(filename2));
pop = zeros(popJava.size,2);
iter = popJava.iterator;
ind = 1;
while(iter.hasNext)
    pop(ind,:) = iter.next.getObjectives;
    ind = ind + 1;
end
% obj = dlmread(files(randi(length(files))).name);
scatter(-pop(:,1),pop(:,2)*33495.939796)

axis([0,0.35,0,12000])
legend('eMOEA','oneCross','Location','NorthWest')
hold off
end
