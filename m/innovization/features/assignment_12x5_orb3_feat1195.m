function bool =assignment_12x5_orb3_feat1195(soln)
bool = false;
if( not( logical( abs( soln(25:36) -[0,1,0,0,1,0,1,0,1,0,1,0]))))
bool = true;
end
