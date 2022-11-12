/* ==========================================
 * jORLib : a free Java OR library
 * ==========================================
 *
 * Project Info:  https://github.com/jkinable/jorlib
 * Project Creator:  Joris Kinable (https://github.com/jkinable)
 *
 * (C) Copyright 2015, by Joris Kinable and Contributors.
 *
 * This program and the accompanying materials are licensed under LGPLv2.1
 *
 */
/* -----------------
 * SameColor.java
 * -----------------
 * (C) Copyright 2016, by Joris Kinable and Contributors.
 *
 * Original Author:  Joris Kinable
 * Contributor(s):   -
 *
 * $Id$
 *
 * Changes
 * -------
 *
 */
package locationAssignmentBAP.bap.branching.branchDecisions;

import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.model.LocationAssignment;
import model.Customer;
import org.jgrapht.util.VertexPair;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;

/**
 * Ensure that two vertices are assigned the same color
 * @author Joris Kinable
 * @version 29-6-2016
 */
public final class SameColumn implements BranchingDecision<LocationAssignment, AssignmentColumn> {

    /** Vertices to be colored the same **/
    public final VertexPair<Customer> vertexPair;

    public SameColumn(VertexPair<Customer> vertexPair){
        this.vertexPair=vertexPair;
    }

    /**
     * Determine whether the given column remains feasible for the child node
     * @param column column
     * @return true if the column is compliant with the branching decision
     */
    @Override
    public boolean columnIsCompatibleWithBranchingDecision(AssignmentColumn column) {
        if(column instanceof AssignmentColumn_virtual){
            return false;
        }
        AssignmentColumn_true assignmentColumn_true=(AssignmentColumn_true)column;
        return !(assignmentColumn_true.customers.contains(vertexPair.getFirst()) ^ assignmentColumn_true.customers.contains(vertexPair.getSecond()));
    }

    /**
     * Determine whether the given inequality remains feasible for the child node
     * @param inequality inequality
     * @return true
     */
    @Override
    public boolean inEqualityIsCompatibleWithBranchingDecision(AbstractInequality inequality) {
        return true;  //Cuts are not added in this example
    }

    @Override
    public String toString(){
        return "Samecolumn "+vertexPair;
    }
}
