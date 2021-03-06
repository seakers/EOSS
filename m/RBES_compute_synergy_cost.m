function [syn_S,syn_E] = RBES_compute_synergy_cost(instr1,instr2,TALK)
global params
%% no synergies
% if strcmp('CASE_STUDY','IRIDIUM')
%     orbit = get_Iridium_orbit();
% else
%     orbit = [];
% end
potent_orbits = params.potent_orbits;

[orbit1,~,co1,~] = RBES_optimize_orbit({instr1},potent_orbits,'MAX_UTILITY',[0.5 0.5]);
miss1 = create_test_mission('test',{instr1},params.startdate,params.lifetime,get_orbit_struct_from_string(orbit1));
[~,~,~,subobj_1,~,~,~] = RBES_Evaluate_Mission(miss1);

[orbit2,~,co2,~] = RBES_optimize_orbit({instr2},potent_orbits,'MAX_UTILITY',[0.5 0.5]);
miss2 = create_test_mission('test',{instr2},params.startdate,params.lifetime,get_orbit_struct_from_string(orbit2));
[~,~,~,subobj_2,~,~,~] = RBES_Evaluate_Mission(miss2);
               
[combined_score,~,~,combined_subobj] = RBES_combine_subobj_scores({subobj_1;subobj_2});
cost_nosyn = co1 + co2;

%% synergies
[orbit3,~,~,~] = RBES_optimize_orbit({instr1,instr2},potent_orbits,'MAX_UTILITY',[0.5 0.5]);
miss3 = create_test_mission('test',{instr1,instr2},params.startdate,params.lifetime,get_orbit_struct_from_string(orbit3));
[score_3,~,~,subobj_3,~,~,cost_syn,~,~] = RBES_Evaluate_Mission(miss3);

syn_S = score_3 - combined_score;
syn_E = cost_syn - cost_nosyn;
if TALK
    RBES_compare_subobjective_structs(combined_subobj,subobj_3);
end
end