package litil.cg.samples;

public class Sample3 {
    public int gcd(int a, int b) {
        if(b==0) {
            return a;
        } else {
            return gcd(b, (a % b));
        }
    }
}
