function climateCentricViolations(datafile)

%load the file
load(datafile)
str = strsplit(datafile,filesep);
str2 = strsplit(str{end},'.');
filename = str2{1};

massThreshold = 3000;
dutyCycleThreshold = 0.5;
packingEfficiencyThreshold = 0.4;

%compute the violations for each constraint
violations = zeros(length(nfe),6);
%compute mass violations
vm = (mass-massThreshold)./abs(mass);
vm(vm<0) = 0;
violations(:,1) = sum(vm,2);
%compute duty cycle violation
vd = (dutyCycleThreshold-dutyCycle)/dutyCycleThreshold;
vd(vd<0) = 0; %for the satellites that meet the threshold
vd(vd==1) = 0; %for the satellites with no instruments
violations(:,2) = sum(vd,2);
%compute packing efficiency violation
vpe = (packingEfficiencyThreshold-packingEfficiency)/packingEfficiencyThreshold;
vpe(vpe<0) = 0; %for the satellites that meet the threshold
vpe(vpe==1) = 0; %for the satellites with no instruments
violations(:,3) = sum(vpe,2);
violations(:,4) = instorb;
violations(:,5) = synergy;
violations(:,6) = interference;

%compute the solutions with any violations over nfe
uniqueNFE = unique(nfe);
anyViolation = zeros(length(unique(nfe)),6);
for i=2:length(uniqueNFE)
    ind = and(nfe > uniqueNFE(i-1), nfe <= uniqueNFE(i));
    for j =1:6
        temp = violations(ind,j);
        anyViolation(i,j) = sum(temp>0)/sum(ind);
    end
end

%compute the average violation in the population over nfe
meanViolation = zeros(length(unique(nfe)),6);
for i=2:length(uniqueNFE)
    ind = and(nfe > uniqueNFE(i-1), nfe <= uniqueNFE(i));
    for j =1:6
        meanViolation(i,j) = mean(violations(ind,j));
    end
end


%plot fraction of solutions with any violation (smooth signal)
h = figure(1);
cla
windowSize = 100;
wts = ones(1,windowSize)/windowSize;
hold on
for i=1:6
    v = filter(wts,1,anyViolation(:,i));
    plot(uniqueNFE - windowSize/2,v);
end
legend('Mass','DutyCycle','PackingEfficiency','InstrumentOrbit','Synergy','Interference')
xlabel('NFE')
ylabel('Fraction of Solutions with any violation')
axis([0,5000,0,1])
saveas(h, strcat(filename, '_violations_history.fig'))
saveas(h, strcat(filename, '_violations_history.png'))

%plot sum of violations in population
h = figure(2);
cla
windowSize = 100;
wts = ones(1,windowSize)/windowSize;
hold on
for i=1:6
    v = filter(wts,1,meanViolation(:,i));
    plot(uniqueNFE - windowSize/2,v);
end
legend('Mass','DutyCycle','PackingEfficiency','InstrumentOrbit','Synergy','Interference')
xlabel('NFE')
ylabel('Mean violation')
axis([0,5000,0,1])
saveas(h, strcat(filename, '_violations_history.fig'))
saveas(h, strcat(filename, '_violations_history.png'))

%plot heat map of violations in objective space
h = figure(3);
cla
constraints = {'mass','dutycycle','packing efficiency','instrument-orbit','synergy','interference'};
for i=1:size(violations,2)
    subplot(3,2,i)
    scatter(-objectives(:,1),objectives(:,2), 5, violations(:,i));
    colormap jet
    c = colorbar;
    c.Label.String = 'violations';
    title(constraints{i})
    axis([0,0.35, 0, 2.5e4])
end
saveas(h, strcat(filename, '_violations_map.fig'))
saveas(h, strcat(filename, '_violations_map.png'))

%plot heat map of violations in objective space
h = figure(4);
cla
scatter(-objectives(:,1),objectives(:,2), 5, nfe);
colormap jet
c = colorbar;
c.Label.String = 'NFE';
axis([0,0.35, 0, 2.5e4])
saveas(h, strcat(filename, '_NFE_map.fig'))
saveas(h, strcat(filename, '_NFE_map.png'))

end



