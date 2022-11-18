package benders;

import MIP.mipDeterministic.MipD;
import benders.master.Mip;
import ilog.concert.IloException;
import io.Reader;
import model.Instance;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author Wang Li
 * @description
 * @date 2022/11/17 20:57
 */
public class BendersSolver {
    public static void main(String[] args) throws FileNotFoundException, IloException {
        File file=new File("dataset/instance/instance");

        Instance instance= Reader.readInstance(file,10,0,5,30,15,1);
        Mip mip=new Mip(instance);

        long runTime=System.currentTimeMillis();
        System.out.println("Starting branch and bound for "+instance.getName());
        mip.solve();
        runTime=System.currentTimeMillis()-runTime;

        if(mip.isFeasible()){

            System.out.println("Objective: "+mip.getObjectiveValue());
            System.out.println("Runtime: "+runTime);
            System.out.println("Is optimal: "+mip.isOptimal());
            System.out.println("Bound: "+mip.getLowerBound());
            System.out.println("Nodes: "+mip.getNrOfNodes());

        }else{
            System.out.println("MIP infeasible!");
        }

    }
}
