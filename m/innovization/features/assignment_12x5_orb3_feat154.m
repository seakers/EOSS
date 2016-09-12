function bool =assignment_12x5_orb3_feat154(soln)
bool = false;
if( not( logical( abs( soln(25:36) -[0,0,0,0,1,0,0,1,1,0,0,1]))))
bool = true;
end
