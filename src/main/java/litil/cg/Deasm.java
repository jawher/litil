package litil.cg;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.PrintWriter;

public class Deasm {
    public static void main(String[] args) throws IOException {
        ClassReader cr = new ClassReader("litil.cg.samples.Sample4");
        TraceClassVisitor tcv = new TraceClassVisitor(null, new ASMifier(), new PrintWriter(System.out));

        cr.accept(tcv, 0);

    }
}