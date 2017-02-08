%runs the post-run analysis
%reads .res file and puts all results data into one mfile

% path = 'C:\Users\SEAK2\Nozomi\EOSS\problems\climateCentric\result\IDETC2017';
path = '/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric/result/IDETC 2017';
[~,fHV,~] = getAllResults(strcat(path,filesep,'baseline'),'','');
baseline = fHV;

[nfe,fHV,~] = getAllResults(strcat(path,filesep,'emoea_operator_static'),'','');
constraint = fHV;

[~,fHV,~] = getAllResults(strcat(path,filesep,'emoea_operator_random'),'','');
random = fHV;

[~,fHV,~] = getAllResults(strcat(path,filesep,'emoea_operator_aos'),'','');
aos = fHV;

[dataPoints, ~] = size(fHV);
base_constraint_metric_sig = zeros(dataPoints,1);
base_rand_metric_sig = zeros(dataPoints,1);
base_aos_metric_sig = zeros(dataPoints,1);

sigLevel = 0.05;
for i = 1:dataPoints
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
    
    [~,h] = ranksum(baseline(i,:),random(i,:));
    if h==1 %then significant difference and medians are different at 0.95 confidence
        med_diff = median(baseline(i,:))-median(random(i,:));
        if med_diff < 0
            base_rand_metric_sig(i) = -1;
        else
            base_rand_metric_sig(i) = 1;
        end
    else
        base_rand_metric_sig(i) = 0;
    end
    
    [~,h] = ranksum(baseline(i,:),aos(i,:));
    if h==1 %then significant difference and medians are different at 0.95 confidence
        med_diff = median(baseline(i,:))-median(aos(i,:));
        if med_diff < 0
            base_aos_metric_sig(i) = -1;
        else
            base_aos_metric_sig(i) = 1;
        end
    else
        base_aos_metric_sig(i) = 0;
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
h = fill(X,Y,'r','EdgeColor','none');
alpha(h,0.15) %sest transparency

X = [nfe(:,1);flipud(nfe(:,1))];
stddev = std(random,0,2);
mu = mean(random,2)-mu_norm;
Y = [mu-stddev;flipud(mu+stddev)];
h = fill(X,Y,'c','EdgeColor','none');
alpha(h,0.15) %sest transparency

X = [nfe(:,1);flipud(nfe(:,1))];
stddev = std(aos,0,2);
mu = mean(aos,2)-mu_norm;
Y = [mu-stddev;flipud(mu+stddev)];
h = fill(X,Y,'b','EdgeColor','none');
alpha(h,0.15) %sest transparency

handles = plot(nfe(:,1),mean(baseline,2)-mu_norm,'-k',...
    nfe(:,1),mean(constraint,2)-mu_norm,'-r',...
    nfe(:,1),mean(random,2)-mu_norm,'-c',...
    nfe(:,1),mean(aos,2)-mu_norm,'-b','LineWidth',1);

%plot where the performance is statistically significantly different

plot(nfe(base_constraint_metric_sig==-1,1),ones(sum(base_constraint_metric_sig==-1),1)*0.2,'.k','LineWidth',1)
plot(nfe(base_constraint_metric_sig==1,1),ones(sum(base_constraint_metric_sig==1),1)*0.21,'ok','LineWidth',1)
plot(nfe(base_rand_metric_sig==-1,1),ones(sum(base_rand_metric_sig==-1),1)*0.3,'.k','LineWidth',1)
plot(nfe(base_rand_metric_sig==1,1),ones(sum(base_rand_metric_sig==1),1)*0.31,'ok','LineWidth',1)
plot(nfe(base_aos_metric_sig==-1,1),ones(sum(base_aos_metric_sig==-1),1)*0.4,'.k','LineWidth',1)
plot(nfe(base_aos_metric_sig==1,1),ones(sum(base_aos_metric_sig==1),1)*0.41,'ok','LineWidth',1)
axis([0,5000,0,1])
hold off
xlabel('NFE')
ylabel('HV(alg)-HV(\epsilon-MOEA)')
legend(handles, '\epsilonMOEA', 'O-Static','O-Rand','O-AOS','Location','SouthEast')
set(gca,'FontSize',16);


