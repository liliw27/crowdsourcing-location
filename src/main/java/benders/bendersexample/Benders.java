package benders.bendersexample;

import java.util.Arrays;


import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloLinearNumExprIterator;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class Benders {
	Data data;
	protected IloCplex subProblem;
	protected IloCplex master;
	IloObjective subobj;//��¼������Ŀ�꺯��
	IloLinearNumExpr subobj_expr;
	//��ż����
	protected IloNumVar[] u;	// ��ԴԼ���Ķ�ż����
	protected IloNumVar[] v;	// ����Լ���Ķ�ż����
	protected IloNumVar[][] w;	// x,yԼ���Ķ�ż����
	double[] uSource;	//��������Ŀ�꺯�����ż����u��Ӧϵ��
	double[] vDemand;	//��������Ŀ�꺯�����ż����v��Ӧϵ��
	double[][] wM;		//��������Ŀ�꺯�����ż����w��Ӧϵ��

	protected IloRange[][] subCon;	//�������Լ������
	double[][] xnum;				//��¼ԭ�����xֵ

	protected IloNumVar subcost;	//�������е�Ŀ��ֵ����Ӧ�ɳڵ�������ģ����q
	protected IloNumVar[][] y;		//�������еı���
	public static final double FUZZ = 1.0e-7;

	int[][] y1;//�������б���y��ʼֵ
	public Benders(Data d) {
		// TODO Auto-generated constructor stub
		this.data = d;
	}
	//��1
	void setOne(int[][] a) {
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				a[i][j] = 1;
			}
		}
	}
	//�����������������cplexģ��
	public void bendersModel() throws IloException {
		subProblem = new IloCplex();//������
		master = new IloCplex();	//������
		subProblem.setOut(null);
		master.setOut(null);
		//������ʼ��
		y1 = new int[data.SourcesSize][data.DemandsSize];
		setOne(y1);// ��ʼ������y=[1]
		u = new IloNumVar[data.SourcesSize];
		v = new IloNumVar[data.DemandsSize];
		w = new IloNumVar[data.SourcesSize][data.DemandsSize];
		uSource = new double[data.SourcesSize];
		vDemand = new double[data.DemandsSize];
		wM = new double[data.SourcesSize][data.DemandsSize];
		y = new IloNumVar[data.SourcesSize][data.DemandsSize];
		subCon = new IloRange[data.SourcesSize][data.DemandsSize];
		xnum = new double[data.SourcesSize][data.DemandsSize];
		//����Լ��
		subcost = master.numVar(0.0, Double.MAX_VALUE, IloNumVarType.Float, "subcost");
		for (int i = 0; i < data.SourcesSize; i++) {
			u[i] = subProblem.numVar(0.0, Double.MAX_VALUE,IloNumVarType.Float, "u_" + i);
		}
		for (int i = 0; i < data.DemandsSize; i++) {
			v[i] = subProblem.numVar(0.0, Double.MAX_VALUE,IloNumVarType.Float, "v_" + i);
		}
		for (int i = 0; i < data.SourcesSize; i++) {
			for (int j = 0; j < data.DemandsSize; j++) {
				y[i][j] = master.numVar(0, 1, IloNumVarType.Int, "y_" + i + "_" + j);
				w[i][j] = subProblem.numVar(0.0, Double.MAX_VALUE,IloNumVarType.Float, "w_" + i + "_" + j);
			}
		}

		// ������
		IloNumExpr expr0 = master.numExpr();
		for (int i = 0; i < data.SourcesSize; i++) {
			for (int j = 0; j < data.DemandsSize; j++) {
				expr0 = master.sum(expr0, master.prod(data.fixed_c[i][j], y[i][j]));
			}
		}
		master.addMinimize(master.sum(subcost, expr0), "TotalCost");
		// attach a Benders callback to the master
		master.use(new BendersCallback());

		//������
		// ������Ŀ�꺯��
		subobj_expr = subProblem.linearNumExpr();
		IloLinearNumExpr obj = subProblem.linearNumExpr();
		for (int i = 0; i < data.SourcesSize; i++) {
			uSource[i] = -data.supply[i];
			obj.addTerm(uSource[i], u[i]);
			subobj_expr.addTerm(uSource[i], u[i]);
		}
		for (int i = 0; i < data.DemandsSize; i++) {
			vDemand[i] = data.demand[i];
			obj.addTerm(vDemand[i], v[i]);
			subobj_expr.addTerm(vDemand[i], v[i]);
		}
		for (int i = 0; i < data.SourcesSize; i++) {
			for (int j = 0; j < data.DemandsSize; j++) {
				wM[i][j] = -data.M[i][j];
				obj.addTerm(wM[i][j] * y1[i][j], w[i][j]);
			}
		}
		subobj = subProblem.addMaximize(obj, "dualSubCost");
		// ������Լ������
		for (int i = 0; i < data.SourcesSize; i++) {
			for (int j = 0; j < data.DemandsSize; j++) {
				IloNumExpr expr = subProblem.numExpr();
				IloNumExpr expr1 = subProblem.numExpr();
				expr = subProblem.sum(subProblem.prod(-1, u[i]), v[j]);
				expr1 = subProblem.sum(expr, subProblem.prod(-1, w[i][j]));
				subCon[i][j] = subProblem.addLe(expr1, data.c[i][j],"C"+i+"_"+j);
			}
		}
		// turn off the presolver for the main model
		subProblem.setParam(IloCplex.BooleanParam.PreInd, false);
		subProblem.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Primal);
	}
	//benders�����������Լ��
	private class BendersCallback extends IloCplex.LazyConstraintCallback {
		public void main() throws IloException {
			double zMaster = getValue(subcost);//���������л��subcost������ֵ
			//����������еı���yֵ
			int[][] y2 = new int[data.SourcesSize][data.DemandsSize];
			for (int i = 0; i < data.SourcesSize; i++) {
				for (int j = 0; j < data.DemandsSize; j++) {
					double aa = getValue(y[i][j]);
					if (aa > 0.5) {
						y2[i][j] = 1;
					} else {
						y2[i][j] = 0;
					}
				}
			}
			//�����ɳڵ��������е� ����yֵ����������Ŀ�꺯��
//			subProblem.remove(subobj);
			 IloLinearNumExpr subobj_expr0 = subProblem.linearNumExpr();
			 for (int i = 0; i < data.SourcesSize; i++) {
					subobj_expr0.addTerm(uSource[i], u[i]);
				}
				for (int i = 0; i < data.DemandsSize; i++) {
					subobj_expr0.addTerm(vDemand[i], v[i]);
				}
				for (int i = 0; i < data.SourcesSize; i++) {
					for (int j = 0; j < data.DemandsSize; j++) {
						subobj_expr0.addTerm(wM[i][j] * y2[i][j], w[i][j]);
					}
				}
//			subobj_expr1 = (IloLinearNumExpr) subProblem.sum(subobj_expr,subobj_expr1);
//			subobj = subProblem.addMaximize(subobj_expr0, "dualSubCost");
			subobj.setExpr(subobj_expr0);//����������Ŀ�꺯��
			subProblem.solve();
			IloCplex.Status status = subProblem.getStatus();
			//�ж�����������״̬
			if (status == IloCplex.Status.Unbounded) {
				// �������
				IloLinearNumExpr ray = subProblem.getRay();//����������һ��������
				System.out.println("getRay returned " + ray.toString());
				//��¼�����ߵĲ���
				IloLinearNumExprIterator it = ray.linearIterator();
				double[] ucoef = new double[data.SourcesSize];	//��������u��ϵ��
				double[] vcoef = new double[data.DemandsSize];	//��������v��ϵ��
				double[][] wcoef = new double[data.SourcesSize][data.DemandsSize];//��������w��ϵ��
				while (it.hasNext()) {
					IloNumVar var = it.nextNumVar();
					boolean varFound = false;
					for (int i = 0; i < data.SourcesSize && !varFound; i++) {
						if (var.equals(u[i])) {
							ucoef[i] = it.getValue();
							varFound = true;
						}
						for (int j = 0; j < data.DemandsSize && !varFound; j++) {
							if (var.equals(w[i][j])) {
								wcoef[i][j] = it.getValue() * wM[i][j];
								varFound = true;
							}
						}
					}
					for (int i = 0; i < data.DemandsSize && !varFound; i++) {
						if (var.equals(v[i])) {
							vcoef[i] = it.getValue();
							varFound = true;
						}
					}
				}
				//����Ҫ��ӵ�Լ������
				IloNumExpr expr1 = master.numExpr();
				double expr2 = 0;
				for (int i = 0; i < data.SourcesSize; i++) {
					expr2 += ucoef[i] * uSource[i];
					expr1 = master.sum(expr1, master.scalProd(wcoef[i], y[i]));
				}
				for (int j = 0; j < data.DemandsSize; j++) {
					expr2 += vcoef[j] * vDemand[j];
				}
				//���Լ�����̵���������
				IloConstraint r = add(master.le(master.sum(expr1, expr2), 0));
				System.out.println("\n>>> Adding feasibility cut: " + r + "\n");

			} else if (status == IloCplex.Status.Optimal) {
				if (zMaster < subProblem.getObjValue() - FUZZ) {
					//�������н⣬�����Ž⼴һ����ֵ��
					double[] ucoef = new double[data.SourcesSize];//������u��ϵ��
					double[] vcoef = new double[data.DemandsSize];//������v��ϵ��
					double[][] wcoef = new double[data.SourcesSize][data.DemandsSize];//������w��ϵ��
					ucoef = subProblem.getValues(u);
					vcoef = subProblem.getValues(v);
					for (int i = 0; i < data.SourcesSize; i++) {
						wcoef[i] = subProblem.getValues(w[i]);
					}
					
					//����Ҫ��ӵ�Լ������
					double expr3 = 0;
					IloNumExpr expr4 = master.numExpr();
					for (int i = 0; i < data.SourcesSize; i++) {
						expr3 += ucoef[i] * uSource[i];
						for (int j = 0; j < data.DemandsSize; j++) {
							wcoef[i][j] = wcoef[i][j] * wM[i][j];
						}
					}
					for (int j = 0; j < data.DemandsSize; j++) {
						expr3 += vcoef[j] * vDemand[j];
					}
					for (int i = 0; i < data.SourcesSize; i++) {
						expr4 = master.sum(expr4, master.scalProd(wcoef[i], y[i]));
					}
					//���Լ�����̵���������
					IloConstraint r = add((IloRange) master.le(master.sum(expr3, expr4), subcost));
					System.out.println("\n>>> Adding optimality cut: " + r + "\n");
				} else {
					System.out.println("\n>>> Accepting new incumbent with value " + getObjValue() + "\n");
					// the master and subproblem flow costs match
					// -- record the subproblem flows in case this proves to be the
					// winner (saving us from having to solve the LP one more time
					// once the master terminates)
					//��������subcostֵ���������е����Ž�ֵ���
					for (int i = 0; i < data.SourcesSize; i++) {
						xnum[i] = subProblem.getDuals(subCon[i]);
					}
				}
			} else {
				// unexpected status -- report but do nothing
				//���ֲ�ϣ�����ֵ�״̬���Ƿ�
				System.err.println("\n!!! Unexpected subproblem solution status: " + status + "\n");
			}
		}
	}
	//���bendersģ�Ͳ����ƽ��
	public final Solution solve() throws IloException {
		Solution s = new Solution();
		//��¼���
		if (master.solve()) {
			s.cost = master.getObjValue();
			s.ship = new double[data.SourcesSize][];
			s.link_y = new double[data.SourcesSize][];
			for (int i = 0; i < data.SourcesSize; i++) {
				s.link_y[i] = master.getValues(y[i]);
				s.ship[i] = Arrays.copyOf(xnum[i], data.DemandsSize);
			}
		}
		s.status = master.getCplexStatus();
		return s;
	}

}
