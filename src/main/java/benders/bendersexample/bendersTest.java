package benders.bendersexample;


import ilog.concert.IloException;

public class bendersTest {
/*
 * �˴���������֣�1.ֱ�ӽ���cplexģ����⡣2.������������������cplexģ�ͣ�����cplex�ڲ�Benders��⺯����⡣
			  3.������������������cplexģ�ͣ�����������α����Benders��������⡣
                              �������ֱַ��ӦSingleModel�ࡢBenders�ࡢBenders_next�ࡣ
 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		Data data = new Data();
		String path = "src/main/java/benders/bendersexample/test1.txt";
		data.read_data(path);
	    long start;
	    long end;
	    //�����򵥵ı�׼ģ�����(SingleModel)
//	    try{
//	    	start = System.currentTimeMillis();
//	        System.out.println("\n================================="
//                    + "\n== Solving the usual MIP model =="
//                    + "\n=================================");
//	        SingleModel model = new SingleModel();
//	        model.standardMIP(data);
//	        Solution s = model.solve();
//	        end = System.currentTimeMillis();
//	        System.out.println("\n***\nThe unified model's solution has total cost "
//                    + String.format("%10.5f", s.cost));
//	        System.out.println();
//	        s.print_ship();
//	        System.out.println("\n*** Elapsed time = "+ (end - start) + " ms. ***\n");
//
//	    } catch (IloException ex) {
//	        System.err.println("\n!!!Unable to solve the unified model"
//                    + ex.getMessage() + "\n!!!");
//	        System.exit(1);
//	    }
	    
	    //benders�㷨���ģ��(Benders)
	    try{
	    	start = System.currentTimeMillis();
	        System.out.println("\n======================================="
                    + "\n== Solving via Benders decomposition =="
                    + "\n=======================================");
	        Benders model2 = new Benders(data);
	        model2.bendersModel();
	        Solution s = model2.solve();
	        end = System.currentTimeMillis();
	        System.out.println("\n***\nThe benders model's solution has total cost "
                    + String.format("%10.5f", s.cost));
	        System.out.println();
	        s.print_ship();
	        System.out.println("\n*** Elapsed time = "+ (end - start) + " ms. ***\n");
	    }catch (IloException ex) {
	        System.err.println("\n!!!Unable to solve the Benders model:\n"
                    + ex.getMessage() + "\n!!!");
	        System.exit(2);
	    }
	    //������ʵ�ַ���(Benders_next)
	    try{
	    	start = System.currentTimeMillis();
	        System.out.println("\n======================================="
                    + "\n== Solving via Benders decomposition =="
                    + "\n=======================================");
	        Benders_next model3 = new Benders_next(data);
	        model3.bendersModel();
	        Solution s = model3.solve();
	        end = System.currentTimeMillis();
	        System.out.println("\n***\nThe benders model's solution has total cost "
                    + String.format("%10.5f", s.cost));
	        System.out.println();
	        s.print_ship();
	        System.out.println("\n*** Elapsed time = "+ (end - start) + " ms. ***\n");
	    }catch (IloException ex) {
	        System.err.println("\n!!!Unable to solve the Benders model:\n"
                    + ex.getMessage() + "\n!!!");
	        System.exit(2);
	    }
	}

}
