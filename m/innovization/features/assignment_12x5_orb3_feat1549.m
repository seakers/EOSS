function bool =assignment_12x5_orb3_feat1549(soln)
bool = false;
if( not( logical( abs( soln(25:36) -[0,1,1,0,0,0,0,0,1,1,0,0]))))
bool = true;
end
