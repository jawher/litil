package litil.cg.samples;

import java.util.*;

import org.objectweb.asm.*;

public class Sample4Dump implements Opcodes {

    public static byte[] dump() throws Exception {

        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, "litil/cg/samples/Sample4", null, "java/lang/Object", null);

        cw.visitSource("Sample4.java", null);

        {
            fv = cw.visitField(ACC_PUBLIC, "x", "Ljava/lang/String;", null, null);
            fv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(3, l0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
            mv.visitInsn(RETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", "Llitil/cg/samples/Sample4;", null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "test", "()I", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(7, l0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn("ohai");
            mv.visitFieldInsn(PUTFIELD, "litil/cg/samples/Sample4", "x", "Ljava/lang/String;");
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLineNumber(8, l1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "litil/cg/samples/Sample4", "x", "Ljava/lang/String;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I");
            mv.visitInsn(IRETURN);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLocalVariable("this", "Llitil/cg/samples/Sample4;", null, l0, l2, 0);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
