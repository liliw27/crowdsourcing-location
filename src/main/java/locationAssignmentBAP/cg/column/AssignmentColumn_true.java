package locationAssignmentBAP.cg.column;

import locationAssignmentBAP.cg.pricing.PricingProblem;
import model.Customer;
import model.StationCandidate;
import model.Worker;

import java.util.Objects;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 2022/10/28 20:32
 */
public class AssignmentColumn_true extends AssignmentColumn{

    public final double cost;
    public final int demand;
    public final Worker worker;
    public final Set<Customer> customers;
    public final StationCandidate stationCandidate;

    /**
     * Constructs a new column
     *
     * @param associatedPricingProblem Pricing problem to which this column belongs
     * @param isArtificial             Is this an artificial column?
     * @param creator                  Who/What created this column?
     * @param cost
     * @param demand
     * @param worker
     * @param customers
     * @param stationCandidate
     */
    public AssignmentColumn_true(PricingProblem associatedPricingProblem, boolean isArtificial, String creator, double cost, int demand, Worker worker, Set<Customer> customers, StationCandidate stationCandidate) {
        super(associatedPricingProblem, isArtificial, creator);
        this.cost = cost;
        this.demand = demand;
        this.worker = worker;
        this.customers = customers;
        this.stationCandidate = stationCandidate;
    }
    public AssignmentColumn_true(PricingProblem associatedPricingProblem, boolean isArtificial, String creator, double cost, int demand, Worker worker, Set<Customer> customers, StationCandidate stationCandidate,double reducedCost) {
        super(associatedPricingProblem, isArtificial, creator);
        this.cost = cost;
        this.demand = demand;
        this.worker = worker;
        this.customers = customers;
        this.stationCandidate = stationCandidate;
        this.reducedCost=reducedCost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssignmentColumn_true that = (AssignmentColumn_true) o;
        return Double.compare(that.cost, cost) == 0 && demand == that.demand && Objects.equals(worker, that.worker) && Objects.equals(customers, that.customers) && Objects.equals(stationCandidate, that.stationCandidate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cost, demand, worker, customers, stationCandidate);
    }

//    @Override
//    public String toString() {
//        return "AssignmentColumn_true{" +
//                "cost=" + cost +
//                ", demand=" + demand +
//                ", worker=" + worker +
//                ", customers=" + customers +
//                ", stationCandidate=" + stationCandidate +
//                '}';
//    }
//    public int hashCode() {
//        int hash = 17;
//        hash=31*hash+stationCandidate.getNodeIndex();
//        hash=hash*31+worker.getIndexD();
////        hash=hash*31+worker.getIndexO();
//        hash=hash*31+worker.getLatD();
//        hash=hash*31+worker.getLngD();
//        for (Customer customer:customers){
////            hash=hash*31+customer.getNodeIndex();
//            hash=hash*31+customer.getLng();
//            hash=hash*31+customer.getLat();
//        }
//        hash=hash*31+demand;
//        long doubleFieldBits=Double.doubleToLongBits(cost);
//        hash=hash*31+(int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
//        doubleFieldBits=Double.doubleToLongBits(reducedCost);
//        hash=hash*31+(int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
//
//        return hash;
//    }
//
//    public boolean equals(Object o) {
//        if (this == o)
//            return true;
//        if (!(o instanceof AssignmentColumn_true))
//            return false;
//        AssignmentColumn_true other = (AssignmentColumn_true) o;
//        if(!worker.equals(other.worker)){
//            return false;
//        }
//        if(!stationCandidate.equals(other.stationCandidate)){
//            return false;
//        }
//        return (this.hashCode() == other.hashCode());
//    }

    @Override
    public String toString() {
        String string="Value: "+this.value+ " worker:"+worker.getIndex()+"; customers:";
        for(Customer customer:customers){
            string+=customer.getIndex()+",";
        }
        string+="; station:"+stationCandidate.getIndex();
        return string;
    }

    @Override
    public int compareTo(AssignmentColumn o) {
        return Double.compare(this.reducedCost, o.reducedCost);
    }
}
