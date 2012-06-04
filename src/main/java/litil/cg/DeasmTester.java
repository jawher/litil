package litil.cg;

import litil.cg.samples.Sample4Dump;

import java.lang.reflect.Method;
import java.util.Arrays;

public class DeasmTester {
    private static class ExprClassLoader extends ClassLoader {

        public Class<?> loadClass(String name, byte[] classData) throws ClassNotFoundException {
            return defineClass(name, classData, 0, classData.length);
        }
    }
    public static void main(String[] args) throws Exception {
        byte[] cdef = Sample4Dump.dump();
        ExprClassLoader ecl = new ExprClassLoader();
        Class<?> clz = ecl.loadClass("litil.cg.samples.Sample4", cdef);
        Object inst = clz.newInstance();


        Method meth = inst.getClass().getMethod("test");
        System.out.println(meth.invoke(inst));
    }
}
