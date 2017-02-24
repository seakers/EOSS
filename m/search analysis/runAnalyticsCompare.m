%runs the post-run analysis
%reads .res file and puts all results data into one mfile

% path = 'C:\Users\SEAK2\Nozomi\EOSS\problems\climateCentric\result\IDETC2017';


path = '/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric/result/IDETC 2017';
[nfe,fHV,~] = getAllResults(strcat(path,filesep,'baseline'),'','');

nexperiments = 7;
hv = zeros(nexperiments,size(fHV,1),30);
hv(1,:,:) = fHV;

[~,fHV,~] = getAllResults(strcat(path,filesep,'emoea_operator_static'),'','');
hv(2,:,:) = fHV;

[~,fHV,~] = getAllResults(strcat(path,filesep,'emoea_operator_random'),'','');
hv(3,:,:) = fHV;

[~,fHV,~] = getAllResults(strcat(path,filesep,'emoea_operator_aos'),'','');
hv(4,:,:) = fHV;

[~,fHV,~] = getAllResults(strcat(path,filesep,'emoea_constraint_cpd'),'','');
hv(5,:,:) = fHV;

[~,fHV,~] = getAllResults(strcat(path,filesep,'emoea_constraint_disjunction'),'','');
hv(6,:,:) = fHV;

[~,fHV,~] = getAllResults(strcat(path,filesep,'emoea_constraint_adaptive_archive'),'','');
hv(7,:,:) = fHV;

[dataPoints, ~] = size(fHV);
base_experiment_metric_sig = zeros(dataPoints,nexperiments-1);

sigLevel = 0.05;
for i = 2:nexperiments
    for j=1:dataPoints
        [~,h] = ranksum(squeeze(hv(1,j,:)),squeeze(hv(i,j,:)));
        if h==1 %then significant difference and medians are different at 0.95 confidence
            med_diff = median(squeeze(hv(1,j,:)))-median(squeeze(hv(i,j,:)));
            if med_diff < 0
                base_experiment_metric_sig(j,i) = -1;
            else
                base_experiment_metric_sig(j,i) = 1;
            end
        else
            base_experiment_metric_sig(j,i) = 0;
        end
    end
end

%plot standard dev areas
 colors = {
    [0         0.4470    0.7410]
    [0.8500    0.3250    0.0980]
    [0.9290    0.6940    0.1250]
    [0.4940    0.1840    0.5560]
    [0.4660    0.6740    0.1880]
    [0.3010    0.7450    0.9330]
    [0.6350    0.0780    0.1840]};
%mu_norm =  mean(squeeze(hv(1,:,:)),2);
mu_norm = zeros(size(hv,2),1);

figure(1)
cla
hold on
handles = [];
plot([0,5000],[0,0],'--k');
for i=2:nexperiments
% X = [nfe(:,1);flipud(nfe(:,1))];
% stddev = std(squeeze(hv(i,:,:)),0,2);
mu = (mean(squeeze(hv(i,:,:)),2)-mu_norm);
% Y = [mu-stddev;flipud(mu+stddev)];
% h = fill(X,Y,'k','EdgeColor','none');
% alpha(h,0.15) %sest transparency
handles = [handles plot(nfe(:,1), mu, '-', 'Color',colors{i-1})];
ind = or(base_experiment_metric_sig(:,i)==1,base_experiment_metric_sig(:,i)==-1);
plot(nfe(ind,1),mu(ind),'LineStyle','none','Marker','.','MarkerSize', 20,'Color',colors{i-1});
end

%plot where the performance is statistically significantly different

% plot(nfe(base_constraint_metric_sig==-1,1),ones(sum(base_constraint_metric_sig==-1),1)*0.2,'.k','LineWidth',1)
% plot(nfe(base_constraint_metric_sig==1,1),ones(sum(base_constraint_metric_sig==1),1)*0.21,'ok','LineWidth',1)
% plot(nfe(base_rand_metric_sig==-1,1),ones(sum(base_rand_metric_sig==-1),1)*0.3,'.k','LineWidth',1)
% plot(nfe(base_rand_metric_sig==1,1),ones(sum(base_rand_metric_sig==1),1)*0.31,'ok','LineWidth',1)
% plot(nfe(base_aos_metric_sig==-1,1),ones(sum(base_aos_metric_sig==-1),1)*0.4,'.k','LineWidth',1)
% plot(nfe(base_aos_metric_sig==1,1),ones(sum(base_aos_metric_sig==1),1)*0.41,'ok','LineWidth',1)
axis([0,5000,0,0.8])
hold off
xlabel('NFE')
ylabel('HV')
legend(handles, 'O-Rand', 'O-AOS', 'C-CPD','C-DNF','C-ACH','Location','SouthEast')
set(gca,'FontSize',16);


