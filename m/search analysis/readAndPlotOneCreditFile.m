function [ops, credits] = readAndPlotOneCreditFile()
%this function parses one of the .credit files created by mopAOS and plots
%the credit history. It also returns the credit history for each operator.
%ops are the operator names, credits is a two column vector for each
%operator


path = '/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric/result/IDETC 2017/';
respath = strcat(path,'emoea_constraint_adaptive');
origin = cd(respath);

files = dir('*.credit');
allcredits  = cell(length(files),1);
for i=1:length(files)
    expData = java.util.HashMap;
    fid = fopen(files(i).name,'r');
    while(feof(fid)==0)
        line = fgetl(fid);
        [~, endIndex] = regexp(line,'iteration,');
        raw_iteration = strsplit(line(endIndex+2:end),',');
        %need to split out the operator name
        line = fgetl(fid);
        [startIndex, endIndex] = regexp(line,'[A-z\+]+');
        raw_credits = strsplit(line(endIndex+2:end),',');
        op_data = zeros(length(raw_iteration)-1,2);
        for j=1:length(raw_credits)
            op_data(j,1)=str2double(raw_iteration{j}); %iteration
            op_data(j,2)=str2double(raw_credits{j}); %credit
        end
        %sometimes there is 0 iteration selection which is not valid
        op_data(~any(op_data(:,1),2),:)=[];
        expData.put(line(startIndex:endIndex),op_data);
    end
    fclose(fid);
    allcredits{i} = expData;
end

%get operator names 
iter = expData.keySet.iterator;
ops = cell(expData.size,1);
i = 1;
while(iter.hasNext)
    ops{i} = iter.next;
    i = i + 1;
end
numOps = length(ops);

%plot
nepochs = 50;
maxEval = 5000;
epochLength = maxEval/nepochs;
all_epoch_credit = zeros(expData.keySet.size, nepochs, length(files)); %keeps track of the epoch credits from the operators
all_epoch_select = zeros(expData.keySet.size, nepochs, length(files)); %keeps track of the epoch selection count for the operators

for i=1:length(files)
    for k = 1:numOps
        hist = allcredits{i}.get(ops{k});
        %sepearates out credits into their respective epochs
        for j=1:nepochs
            %find indices that lie within epoch
            ind1 = epochLength*(j-1)<hist(:,1);
            ind2 = hist(:,1)<epochLength*j;
            epoch = hist(and(ind1,ind2),:);
            if(~isempty(epoch(:,1))) %if it is empty then operator was not selected in the epoch
                all_epoch_credit(k, j, i)=mean( epoch(:,2));
                all_epoch_select(k, j, i) = length(unique(epoch(:,1)));
            end
        end 
    end
end


colors = {'b','r','k','c','g','m','y'};

figure(1)
handles = zeros(numOps,1);
for i=1:numOps
    X = [1:nepochs,fliplr(1:nepochs)];
    stddev = std(squeeze(all_epoch_credit(i,:,:)),0,2);
    mean_cred = mean(squeeze(all_epoch_credit(i,:,:)),2);
    Y = [mean_cred-stddev;flipud(mean_cred+stddev)];
    Y(Y<0) = 0; %correct for negative values
    fill(X,Y,colors{i},'EdgeColor','none');
    alpha(0.15)
    hold on
    handles(i) = plot(1:nepochs,mean(squeeze(all_epoch_credit(i,:,:)),2),colors{i}, 'LineWidth',2);
end
hold off
set(gca,'FontSize',16);
xlabel('Epoch')
ylabel('Credit earned')
legend(handles, ops);

figure(2)
%normalize the selection to make it a probability
concat_select = zeros(nepochs, length(files) * numOps);
for i=1:numOps
    concat_select(:,(i-1) * length(files) + 1:i*length(files)) = squeeze(all_epoch_select(i,:,:));
end
mins = repmat(min(concat_select(2:end,:),[],2),1,length(files));
maxs = repmat(max(concat_select(2:end,:),[],2),1,length(files));

for i=1:numOps
    all_epoch_select_norm = (squeeze(all_epoch_select(i,2:end,:))-mins)./(maxs-mins);
    X = [2:nepochs,fliplr(2:nepochs)];
    stddev_sel = std(all_epoch_select_norm,0,2);
    mean_sel = mean(all_epoch_select_norm,2);
    Y = [mean_sel-stddev_sel;flipud(mean_sel+stddev_sel)];
    Y(Y<0) = 0; %correct for negative values
    Y(Y>1) = 1; %correct for >1 values
    fill(X,Y,colors{i},'EdgeColor','none');
    alpha(0.15)
    hold on
    handles(i) = plot(2:nepochs,mean(all_epoch_select_norm,2),colors{i}, 'LineWidth',2);
end
xlabel('Epoch')
ylabel('Selection frequency')
legend(handles, ops);
hold off
set(gca,'FontSize',16);
%save files
save('credit.mat','allcredits');

cd(origin);


end