package benders.bendersexample;
import ilog.cplex.IloCplex;

public class Solution {
	public double cost;  // �ܻ���
	public double[][] ship;  //������
	public double[][] link_y;//״ָ̬�꣬��Ӧy
	public IloCplex.CplexStatus status;  // status returned by CPLEX
	//���ship��ֵ
	public void print_ship() {
		for (int i = 0; i < ship.length; i++) {
			for (int j = 0; j < ship[i].length; j++) {
				System.out.printf("\t%d -> %d: %f", i, j, ship[i][j],"  ");
			}
			System.out.println();
		}
	}
}
