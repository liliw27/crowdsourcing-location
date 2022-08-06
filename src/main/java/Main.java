import MIP.mipDeterministic.MipD;
import ilog.concert.IloException;
import io.Reader;
import model.Instance;

import java.io.File;
import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) throws FileNotFoundException, IloException {
//        File file=new File("dataset/instance_7.txt");
//        InstanceSimple instance= Reader.read(file);
//        Mip mip=new Mip(instance);

        File file=new File("dataset/instance/instance");

        Instance instance= Reader.readInstance(file,1,0,3,20,2,1);
        MipD mip=new MipD(instance,1);

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
