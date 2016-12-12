%runs the post-run analysis
%reads .res file and puts all results data into one mfile

path = '/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric';
[~,fHV,~] = getAllResults(strcat(path,filesep,'result/AIAA SciTech/5000eval_eMOEA_baseline'),'','');
baseline = fHV;

[nfe,fHV,~] = getAllResults(strcat(path,filesep,'result/AIAA SciTech/5000eval_learning_withSinglecross'),'','');
experiment = fHV;


[~,fHV,~] = getAllResults(strcat(path,filesep,'result/AIAA SciTech/5000eval_innovization_opsAsConstraints'),'','');
constraint = fHV;

[dataPoints, ~] = size(fHV);
base_exp_metric_sig = zeros(dataPoints,1);
base_constraint_metric_sig = zeros(dataPoints,1);
exp_constraint_metric_sig = zeros(dataPoints,1);
sigLevel = 0.05;
for i = 1:dataPoints
    [~,h] = ranksum(baseline(i,:),experiment(i,:));
    if h==1 %then significant difference and medians are different at 0.95 confidence
        med_diff = median(baseline(i,:))-median(experiment(i,:));
        if med_diff < 0
            base_exp_metric_sig(i) = -1;
        else
            base_exp_metric_sig(i) = 1;
        end
    else
        base_exp_metric_sig(i) = 0;
    end
    
    [~,h] = ranksum(baseline(i,:),constraint(i,:));
    if h==1 %then significant difference and medians are different at 0.95 confidence
        med_diff = median(baseline(i,:))-median(constraint(i,:));
        if med_diff < 0
            base_constraint_metric_sig(i) = -1;
        else
            base_constraint_metric_sig(i) = 1;
        end
    else
        base_constraint_metric_sig(i) = 0;
    end
    
    [~,h] = ranksum(experiment(i,:),constraint(i,:));
    if h==1 %then significant difference and medians are different at 0.95 confidence
        med_diff = median(experiment(i,:))-median(constraint(i,:));
        if med_diff < 0
            exp_constraint_metric_sig(i) = -1;
        else
            exp_constraint_metric_sig(i) = 1;
        end
    else
        exp_constraint_metric_sig(i) = 0;
    end
end

figure(1)

%plot standard dev areas

% mu_norm = mean(baseline,2);
mu_norm = zeros(length(baseline),1);

X = [nfe(:,1);flipud(nfe(:,1))];
stddev = std(baseline,0,2);
mu = mean(baseline,2)-mu_norm;
Y = [mu-stddev;flipud(mu+stddev)];
h = fill(X,Y,'k','EdgeColor','none');
alpha(h,0.15) %sest transparency
hold on

X = [nfe(:,1);flipud(nfe(:,1))];
stddev = std(constraint,0,2);
mu = mean(constraint,2)-mu_norm;
Y = [mu-stddev;flipud(mu+stddev)];
h = fill(X,Y,'b','EdgeColor','none');
alpha(h,0.15) %sest transparency

X = [nfe(:,1);flipud(nfe(:,1))];
stddev = std(experiment,0,2);
mu = mean(experiment,2)-mu_norm;
Y = [mu-stddev;flipud(mu+stddev)];
h = fill(X,Y,'r','EdgeColor','none');
alpha(h,0.15) %sest transparency

handles = plot(nfe(:,1),mean(baseline,2)-mu_norm,'-k',...
    nfe(:,1),mean(constraint,2)-mu_norm,'-b',...
    nfe(:,1),mean(experiment,2)-mu_norm,'-r','LineWidth',1);

%plot where the performance is statistically significantly different

plot(nfe(base_exp_metric_sig==-1,1),ones(sum(base_exp_metric_sig==-1),1)*0.5,'-k','LineWidth',1)
plot(nfe(base_exp_metric_sig==1,1),ones(sum(base_exp_metric_sig==1),1)*0.5,'-k','LineWidth',1)
plot(nfe(exp_constraint_metric_sig==-1,1),ones(sum(exp_constraint_metric_sig==-1),1)*1.25,'-k','LineWidth',1)
plot(nfe(exp_constraint_metric_sig==1,1),ones(sum(exp_constraint_metric_sig==1),1)*1.25,'-k','LineWidth',1)
plot(nfe(base_constraint_metric_sig==-1,1),ones(sum(base_constraint_metric_sig==-1),1)*2,'-k','LineWidth',1)
plot(nfe(base_constraint_metric_sig==1,1),ones(sum(base_constraint_metric_sig==1),1)*2,'-k','LineWidth',1)
axis([0,5000,0,4])
%plot vertical lines for learning stages
plot([1000,1000],[0,4.0],':k')
plot([2000,2000],[0,4.0],':k')
plot([3000,3000],[0,4.0],':k')
plot([4000,4000],[0,4.0],':k')

hold off
xlabel('NFE')
ylabel('HV(alg)-HV(\epsilon-MOEA)')
legend(handles, '\epsilonMOEA', 'KDO+C','KDO+AOS','Location','SouthEast')
set(gca,'FontSize',16);


