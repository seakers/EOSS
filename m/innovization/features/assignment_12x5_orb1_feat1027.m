function bool =assignment_12x5_orb1_feat1027(soln)
bool = false;
if( not( logical( abs( soln(1:12) -[0,1,0,0,0,0,0,0,0,0,1,0]))))
bool = true;
end
