function bool =assignment_12x5_orb3_feat2047(soln)
bool = false;
if( not( logical( abs( soln(25:36) -[0,1,1,1,1,1,1,1,1,1,1,0]))))
bool = true;
end
