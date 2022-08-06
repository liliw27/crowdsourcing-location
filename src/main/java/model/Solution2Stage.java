package model;

import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 6/19/22 6:35 PM
 */
@Data
public class Solution2Stage {
    double obj;
    int ScenarioNo;
    Set<Customer> unservedCustomers;
    Map<Worker,Set<Customer>> customerAllocationDecision;
    Map<Worker,StationCandidate> stationAllocationDecision;
    Map<Worker,Double> travelCost;

    public String toString(){

        StringBuffer stringBuffer=new StringBuffer();
        stringBuffer.append("objective of the Scenario "+ ScenarioNo+": "+obj+"\n");
        stringBuffer.append("unServedCustomers: ");
        for(Customer customer:unservedCustomers){
            stringBuffer.append(customer.getIndex()+", ");
        }
        stringBuffer.append("\n");
        for(Worker worker:customerAllocationDecision.keySet()){
            if(customerAllocationDecision.get(worker).isEmpty()){
                continue;
            }
            stringBuffer.append("worker "+worker.getIndex()+": "+"station allocation decision: "+stationAllocationDecision.get(worker).getIndex()+"; travel cost: "+ travelCost.get(worker)+"; customer allocation decistion:");
            for(Customer customer:customerAllocationDecision.get(worker)){
                stringBuffer.append(customer.getIndex()+", ");
            }
            stringBuffer.append("\n");
        }
        return stringBuffer.toString();
    }
}
