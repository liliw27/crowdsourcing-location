package MIP.mipDeterministic;

import ilog.concert.IloIntVar;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

/**
 * @author Wang Li
 * @description
 * @date 6/21/22 3:02 PM
 */
public class MipDataD {
    public final IloCplex cplex;
    //first stage variable
    public final IloIntVar[][] varsLocation;

    //second stage decision variable
    public final IloIntVar[][][] varsAssignIK;
    public final IloIntVar[][] tao;
    public final IloIntVar[][][] varsAssignSK;
    public final IloIntVar[][] unServed;
    public final IloNumVar[][][]z;

    //for travel time prediction, 35 in total
    public final IloNumVar[][][][] theta; //1. domain [0,1]
    public final IloNumVar[][] dmax1; //2. [0,1000sqart2]
    public final IloIntVar[][][] u;//3. {0,1}
    public final IloNumVar[][][] f1;//4. [0,1000]
    public final IloIntVar[][][][] varTheta;//5. {0,1}
    public final IloNumVar[][][] epsilon;//6. [0,1000]
    public final IloNumVar[][] dmax2;//7. [0,1000sqart2]
    public final IloNumVar[][][] f2;//8. [0,1000]
    public final IloNumVar[][]a1plus;//9. [0,1000]
    public final IloNumVar[][]a1minus;//10. [-1000,0]
    public final IloNumVar[][]b1plus;//11. [0,1000]
    public final IloNumVar[][]b1minus;//12. [-1000,0]
    public final IloNumVar[][]a2plus;//13. [0,1000]
    public final IloNumVar[][]a2minus;//14. [-1000,0]
    public final IloNumVar[][]b2plus;//15. [0,1000]
    public final IloNumVar[][]b2minus;//16. [-1000,0]
    public final IloNumVar[][][]gamma1;//17. [0,500*10^-0.5]
    public final IloNumVar[][][]gamma2;//18. [0,500*10^-0.5]
    public final IloNumVar[][][]gamma3;//19. [0,500*10^-0.5]
    public final IloNumVar[][][]gamma4;//20. [0,500*10^-0.5]
    public final IloNumVar[][][]gamma5;//21. [0,500*10^0.5]
    public final IloNumVar[][][]gamma6;//22. [0,500*10^0.5]
    public final IloNumVar[][][]gamma7;//23. [0,500*10^0.5]
    public final IloNumVar[][][]gamma8;//24. [0,500*10^0.5]
    public final IloNumVar[][][]gamma9;//25. [0,500*10]
    public final IloNumVar[][][]gamma10;//26. [0,500*10]
    public final IloNumVar[][][]gamma11;//27. [0,500*10]
    public final IloNumVar[][][]gamma12;//28. [0,500*10]
    public final IloNumVar[][][]gamma13;//29. [0,500*10^1.5]
    public final IloNumVar[][][]gamma14;//30. [0,500*10^1.5]
    public final IloNumVar[][][]gamma15;//31. [0,500*10^1.5]
    public final IloNumVar[][][]gamma16;//32. [0,500*10^1.5]~[0,1000*10^1.5]
    public final IloIntVar[][][][] xPrime;//33. {0,1}
    public final IloNumVar[][][] delta;//34. [0,200]
    public final IloNumVar[][][] deltaPrime;// 35. [0,200]
    public MipDataD(IloCplex cplex, IloIntVar[][] varsLocation, IloIntVar[][][] varsAssignIK, IloIntVar[][] tao, IloIntVar[][][] varsAssignSK, IloIntVar[][] unServed, IloNumVar[][][] z, IloNumVar[][][][] theta, IloNumVar[][] dmax1, IloIntVar[][][] u, IloNumVar[][][] f1, IloIntVar[][][][] varTheta, IloNumVar[][][] epsilon, IloNumVar[][] dmax2, IloNumVar[][][] f2, IloNumVar[][] a1plus, IloNumVar[][] a1minus, IloNumVar[][] b1plus, IloNumVar[][] b1minus, IloNumVar[][] a2plus, IloNumVar[][] a2minus, IloNumVar[][] b2plus, IloNumVar[][] b2minus, IloNumVar[][][] gamma1, IloNumVar[][][] gamma2, IloNumVar[][][] gamma3, IloNumVar[][][] gamma4, IloNumVar[][][] gamma5, IloNumVar[][][] gamma6, IloNumVar[][][] gamma7, IloNumVar[][][] gamma8, IloNumVar[][][] gamma9, IloNumVar[][][] gamma10, IloNumVar[][][] gamma11, IloNumVar[][][] gamma12, IloNumVar[][][] gamma13, IloNumVar[][][] gamma14, IloNumVar[][][] gamma15, IloNumVar[][][] gamma16, IloIntVar[][][][] xPrime, IloNumVar[][][] delta, IloNumVar[][][] deltaPrime) {
        this.cplex = cplex;
        this.varsLocation = varsLocation;
        this.varsAssignIK = varsAssignIK;
        this.tao = tao;
        this.varsAssignSK = varsAssignSK;
        this.unServed = unServed;
        this.z = z;
        this.theta = theta;
        this.dmax1 = dmax1;
        this.u = u;
        this.f1 = f1;
        this.varTheta = varTheta;
        this.epsilon = epsilon;
        this.dmax2 = dmax2;

        this.f2 = f2;
        this.a1plus = a1plus;
        this.a1minus = a1minus;
        this.b1plus = b1plus;
        this.b1minus = b1minus;
        this.a2plus = a2plus;
        this.a2minus = a2minus;
        this.b2plus = b2plus;
        this.b2minus = b2minus;
        this.gamma1 = gamma1;
        this.gamma2 = gamma2;
        this.gamma3 = gamma3;
        this.gamma4 = gamma4;
        this.gamma5 = gamma5;
        this.gamma6 = gamma6;
        this.gamma7 = gamma7;
        this.gamma8 = gamma8;
        this.gamma9 = gamma9;
        this.gamma10 = gamma10;
        this.gamma11 = gamma11;
        this.gamma12 = gamma12;
        this.gamma13 = gamma13;
        this.gamma14 = gamma14;
        this.gamma15 = gamma15;
        this.gamma16 = gamma16;
        this.xPrime = xPrime;
        this.delta = delta;
        this.deltaPrime = deltaPrime;
    }
}
