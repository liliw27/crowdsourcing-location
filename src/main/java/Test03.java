/**
 * @author Wang Li
 * @description
 * @date 2022/10/31 13:58
 */

public class Test03 {
    private int i1 = printCommon();
    private static int i2 = printStatic();

    public Test03() {
        System.out.println("constructor");
    }

    public static int printCommon() {
        System.out.println("i1 is init!");
        return 1;
    }

    public static int printStatic() {
        System.out.println("i2 is init!");
        return 2;
    }

    public static void main(String[] args) {
        Test03 t = new Test03();
        int a=(int)Math.floor(-2.8);
        System.out.println(a);
    }
}


