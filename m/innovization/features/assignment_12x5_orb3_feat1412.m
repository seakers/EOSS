function bool =assignment_12x5_orb3_feat1412(soln)
bool = false;
if( not( logical( abs( soln(25:36) -[0,1,0,1,1,0,0,0,0,0,1,1]))))
bool = true;
end
