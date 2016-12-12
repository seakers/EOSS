%Takes n random final populations and plots them out to show how final
%populations from two methods differ

n_representatives = 3;
path = '/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric/result/AIAA SciTech/';
method1 = '5000eval_eMOEA_baseline/';
method2 = '5000eval_innovization_opsAsConstraints/';
method3 = '5000eval_learning_withSinglecross/';

files1 = dir(strcat(path,method1,'*.obj'));
ind1 = randi(length(files1),n_representatives);

files2 = dir(strcat(path,method2,'*.obj'));
ind2 = randi(length(files2),n_representatives);

files3 = dir(strcat(path,method3,'*.obj'));
ind3 = randi(length(files3),n_representatives);

figure(1)
for i=1:n_representatives
    subplot(1,n_representatives,i);
    pop1 = csvread(strcat(path,method1,files1(ind1(i)).name));
    pop2 = csvread(strcat(path,method2,files2(ind2(i)).name));
    pop3 = csvread(strcat(path,method3,files3(ind3(i)).name));
    scatter(-pop1(:,1),pop1(:,2)*33495.939796,'k')
    hold on
    scatter(-pop2(:,1),pop2(:,2)*33495.939796,'b')
    scatter(-pop3(:,1),pop3(:,2)*33495.939796,'r')
    hold off
    xlabel('Scientific Benefit')
    axis([0,0.3,0,10000])
    set(gca,'FontSize',16);
end
subplot(1,n_representatives,1)
legend( '\epsilonMOEA','KDO+C','KDO+AOS')
ylabel('Lifecycle cost ($FY10M)')
