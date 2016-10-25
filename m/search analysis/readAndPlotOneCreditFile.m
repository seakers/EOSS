function [ops, credits] = readAndPlotOneCreditFile()
%this function parses one of the .credit files created by mopAOS and plots
%the credit history. It also returns the credit history for each operator.
%ops are the operator names, credits is a two column vector for each
%operator


path = '/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric/';
respath = strcat(path,'result/AIAA SciTech/5000eval_learning_withoutCross');
origin = cd(respath);

files = dir('*.credit');
allcredits  = cell(length(files),1);
for i=1:length(files)
    expData = java.util.HashMap;
    fid = fopen(files(i).name,'r');
    while(feof(fid)==0)
        raw_iteration = strsplit(fgetl(fid),',');
        %need to split out the operator name
        line = fgetl(fid);
        [startIndex, endIndex] = regexp(line,'(EOSSOperator|OnePointCrossover).*BitFlip');
        raw_credits = strsplit(line(endIndex+1:end),',');
        op_data = zeros(length(raw_iteration)-1,2);
        for j=2:length(raw_credits)
            op_data(j-1,1)=str2double(raw_iteration{j}); %iteration
            op_data(j-1,2)=str2double(raw_credits{j}); %credit
        end
        %sometimes there is 0 iteration selection which is not valid
        op_data(~any(op_data(:,1),2),:)=[];
        expData.put(line(startIndex:endIndex),op_data);
    end
    fclose(fid);
    allcredits{i} = expData;
end

%plot
nepochs = 50;
maxEval = 5000;
epochLength = maxEval/nepochs;
all_epoch_credit_singlePoint = zeros(nepochs, length(files)); %keeps track of the epoch credits from the single point crossover 
all_epoch_select_singlePoint = zeros(nepochs, length(files)); %keeps track of the epoch selection count for the single point crossover 
all_epoch_credit_learned_ops = zeros(nepochs, length(files)); %keeps track of the epoch credits from the learned operators
all_epoch_select_learned_ops = zeros(nepochs, length(files)); %keeps track of the epoch selection count for the single point crossover 

for i=1:length(files)
    op_num = 1;
    %collect all the raw data from all operators into one history
    raw_data = zeros(maxEval, allcredits{i}.keySet.size-1);
    iter = allcredits{i}.keySet.iterator;
    while (iter.hasNext)
        op = iter.next;
        if(strcmp(op,'OnePointCrossover+BitFlip'))
            continue;
        end
        hist = allcredits{i}.get(op);
        if(size(hist,2)==1)
            hist = hist';
        end
        raw_data(hist(1:end,1)+1, 1) = hist(1:end,1);
        raw_data(hist(1:end,1)+1, op_num+1) = hist(1:end,2);
    end
    raw_data(~any(raw_data(:,1),2),:)=[];
    %add up all the credits from all the learned operators
    sumCredits = [raw_data(:,1),sum(raw_data(:,2:end),2)];
    
    %find the credits earned just by single point crossover
    single_point = allcredits{i}.get('OnePointCrossover+BitFlip');
    
    %sepearates out credits into their respective epochs
    for j=1:nepochs
        %First do the one point crossover
        %find indices that lie within epoch
        ind1 = epochLength*(j-1)<single_point(:,1);
        ind2 = single_point(:,1)<epochLength*j;
        epoch = single_point(and(ind1,ind2),:);
        if(~isempty(epoch(:,1))) %if it is empty then operator was not selected in the epoch
            all_epoch_credit_singlePoint(j,i)=mean( epoch(:,2));
            all_epoch_select_singlePoint(j,i) = length(unique(epoch(:,1)));
        end
        
        %Next do the learned operators
        %find indices that lie within epoch
        ind1 = epochLength*(j-1)<sumCredits(:,1);
        ind2 = sumCredits(:,1)<epochLength*j;
        epoch = sumCredits(and(ind1,ind2),:);
        if(~isempty(epoch(:,1))) %if it is empty then operator was not selected in the epoch
            all_epoch_credit_learned_ops(j,i)=mean( epoch(:,2));
            all_epoch_select_learned_ops(j,i) = length(unique(epoch(:,1)));
        end
    end
end

figure(1)
plot(1:nepochs,mean(all_epoch_credit_singlePoint,2),1:nepochs,mean(all_epoch_credit_learned_ops,2));
xlabel('Epoch')
ylabel('Credit earned')
legend('Single point crossover','Learned Operators');

figure(2)
%normalize the selection to make it a probability
summation = mean(all_epoch_select_singlePoint,2) + mean(all_epoch_select_learned_ops,2);
plot(1:nepochs,mean(all_epoch_select_singlePoint,2)./summation,1:nepochs,mean(all_epoch_select_learned_ops,2)./summation);
xlabel('Epoch')
ylabel('Selection frequency')
legend('Single point crossover','Learned Operators');

%save files
save('credit.mat','allcredits');

cd(origin);


end