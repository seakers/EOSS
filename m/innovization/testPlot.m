function testPlot(obj,ind,stats,i)
figure(1)
hold off
scatter(-obj(:,1),obj(:,2)*33.495,'b');
hold on
scatter(-obj(stats.support2ind,1),33.495*obj(stats.support2ind,2),'g','filled');
scatter(-obj(stats.support3ind,1),33.495*obj(stats.support3ind,2),'C','filled');
scatter(-obj(stats.support1ind(:,ind(i)),1),33.495*obj(stats.support1ind(:,ind(i)),2),'r','LineWidth',2);
hold off


legend('Solutions Found','Good Region','Poor Region','Has Feature')
xlabel('Science Benefit')
ylabel('Lifecycle Cost (FY2010 $B)')