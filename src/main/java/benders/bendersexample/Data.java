package benders.bendersexample;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

public class Data {
	int SourcesSize;    //��Ӧ������
	int DemandsSize;	//���������
	double []supply;	//��Ӧ����
	double []demand;	//�������
	double [][]c;		//��Ӧ��λ��Դ�Ļ���
	double [][]fixed_c;	//�̶�����
	double[][] M;		//����������Mij
	//��������
	public void read_data(String path) throws Exception {
		String line = null;
		String[] substr = null;
		Scanner cin = new Scanner(new BufferedReader(new FileReader(path)));
		for (int i = 0; i < 2; i++) {
			line = cin.nextLine();
		}
		line.trim();
		substr = line.split(("\\s+"));
		SourcesSize = Integer.parseInt(substr[0]);
		DemandsSize = Integer.parseInt(substr[1]);
		supply = new double[SourcesSize];
		demand = new double[DemandsSize];
		c = new double[SourcesSize][DemandsSize];
		fixed_c = new double[SourcesSize][DemandsSize];
		M = new double[SourcesSize][DemandsSize];
		for (int i = 0; i < 2; i++) {
			line = cin.nextLine();
		}
		line.trim();
		substr = line.split(("\\s+"));
		for (int i = 0; i < SourcesSize; i++) {
			supply[i] = Integer.parseInt(substr[i]);
		}
		for (int i = 0; i < 2; i++) {
			line = cin.nextLine();
		}
		line.trim();
		substr = line.split(("\\s+"));
		for (int i = 0; i < DemandsSize; i++) {
			demand[i] = Integer.parseInt(substr[i]);
		}
		line = cin.nextLine();
		for (int i = 0; i < SourcesSize; i++) {
			line = cin.nextLine();
			line.trim();
			substr = line.split(("\\s+"));
			for (int j = 0; j < DemandsSize; j++) {
				c[i][j] = Integer.parseInt(substr[j]);
			}
		}
		
		line = cin.nextLine();
		for (int i = 0; i < SourcesSize; i++) {
			line = cin.nextLine();
			line.trim();
			substr = line.split(("\\s+"));
			for (int j = 0; j < DemandsSize; j++) {
				fixed_c[i][j] = Integer.parseInt(substr[j]);
			}
		}
		cin.close();
		//����M����
		 for (int i = 0; i < SourcesSize; i++) {
			 for (int j = 0; j < DemandsSize; j++) {
			 M[i][j] = Math.min(supply[i], demand[j]);
			 }
		 }
	}
}
