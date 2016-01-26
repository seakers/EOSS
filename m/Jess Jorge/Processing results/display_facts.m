function [] = display_facts(facts)
    cellfun(@(fact) disp([ ...
        'f-' int2str(fact.getFactId()) char(9) char(fact.toString()) ...
        ]), facts, 'uniformOutput', false);
end