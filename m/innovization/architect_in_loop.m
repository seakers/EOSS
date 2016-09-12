%% desired
x1_bound = -.265;
x2_bound = .12;

desired = not(and(obj(:,1) > x1_bound, obj(:,2) > x2_bound));

figure(1)
subplot(1,2,1)
hold off
scatter(obj(:,1),obj(:,2));
hold on
scatter(obj(desired,1),obj(desired,2));
hold off

goodStuff = sum(dec(desired,:),1)./sum(dec,1);

subplot(1,2,2)
plot(goodStuff,'bo')

keepGood = find(goodStuff>0.27);
noKeepGood = find(goodStuff<0.015);

keepGoodStr = '[';
for i=1:length(keepGood)
    keepGoodStr = strcat(keepGoodStr,num2str(keepGood(i)),',');
end
keepGoodStr = fprintf(strcat(keepGoodStr,'\b]\n'));

noKeepGoodStr = '[';
for i=1:length(noKeepGood)
    noKeepGoodStr = strcat(noKeepGoodStr,num2str(noKeepGood(i)),',');
end
noKeepGoodStr = fprintf(strcat(noKeepGoodStr,'\b]\n\n'));

%% undesired
x1_bound = -.22;
x2_bound = .16;

undesired = and(obj(:,1) > x1_bound, obj(:,2) > x2_bound);

figure(2)
subplot(1,2,1)
hold off
scatter(obj(:,1),obj(:,2));
hold on
scatter(obj(undesired,1),obj(undesired,2));
hold off

badStuff = sum(dec(undesired,:),1)./sum(dec,1);
subplot(1,2,2)
plot(badStuff,'bo')

keepBad = find(badStuff>.96);
noKeepBad = find(badStuff<0.4);

keepBadStr = '[';
for i=1:length(keepBad)
    keepBadStr = strcat(keepBadStr,num2str(keepBad(i)),',');
end
keepBadStr = fprintf(strcat(keepBadStr,'\b]\n'));

noKeepBadStr = '[';
for i=1:length(noKeepBad)
    noKeepBadStr = strcat(noKeepBadStr,num2str(noKeepBad(i)),',');
end
noKeepBadStr = fprintf(strcat(noKeepBadStr,'\b]\n\n'));