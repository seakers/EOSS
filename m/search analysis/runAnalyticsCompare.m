%runs the post-run analysis
%reads .res file and puts all results data into one mfile

path = '/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric';
[~,fHV,~] = getAllResults(strcat(path,filesep,'result/AIAA SciTech/5000eval_eMOEA_baseline'),'','');
baseline = fHV;

[nfe,fHV,~] = getAllResults(strcat(path,filesep,'result/AIAA SciTech/5000eval_learning_withoutCross'),'','');
experiment = fHV;

[dataPoints, ~] = size(fHV);
metric_sig = zeros(dataPoints,1);
medians = zeros(dataPoints,1);
sigLevel = 0.05;
for i = 1:dataPoints
    [metric_p,h] = ranksum(baseline(i,:),experiment(i,:));
    if h==1 %then significant difference and medians are different at 0.95 confidence
        med_diff = median(baseline(i,:))-median(experiment(i,:));
        if med_diff < 0
            metric_sig(i) = -1;
        else
            metric_sig(i) = 1;
        end
    else
        metric_sig(i) = 0;
    end
end

figure(1)

plot(nfe(:,1),median(baseline,2),'--b',nfe(:,1),median(experiment,2),'--r','LineWidth',3)
hold on
%plot where the performance is statistically significantly different
plot(nfe(metric_sig==1,1),median(experiment(metric_sig==1,:),2),'b','LineWidth',6)
plot(nfe(metric_sig==-1,1),median(experiment(metric_sig==-1,:),2),'r','LineWidth',6)
hold off
xlabel('NFE')
ylabel('HV')
legend(' 1) \epsilonMOEA',' 2) AOS with Learned operators',' 1) statistically outperforms 2)',' 2) statistically outperforms 1)','Location','SouthEast')
set(gca,'FontSize',16);


