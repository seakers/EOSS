%Takes n random final populations and plots them out to show how final
%populations from two methods differ

n_representatives = 1;
path = '/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric/result/ASC Paper/';
method1 = 'baseline/';
method2 = 'emoea_operator_aos/';
method3 = 'emoea_operator_aos_checkChange/';

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
    scatter(-pop1(:,1),pop1(:,2),'k')
    hold on
    scatter(-pop2(:,1),pop2(:,2),'c')
    scatter(-pop3(:,1),pop3(:,2),'y')
    hold off
    xlabel('Scientific Benefit')
    axis([0,0.35,0,6000])
    set(gca,'FontSize',16);
end
subplot(1,n_representatives,1)
legend( '\epsilonMOEA','aos','aos-checkChange','location','northwest')
ylabel('Lifecycle cost ($FY10M)')
