package litil.cg.samples;

import org.objectweb.asm.*;

public class Sample3Dump implements Opcodes {

    public static byte[] dump() throws Exception {

        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, "litil/cg/samples/Sample3", null, "java/lang/Object", null);

        cw.visitSource("Sample3.java", null);

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
            mv.visitLocalVariable("this", "Llitil/cg/samples/Sample3;", null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "gcd", "(II)I", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(5, l0);
            mv.visitVarInsn(ILOAD, 2);
            Label l1 = new Label();
            mv.visitJumpInsn(IFNE, l1);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLineNumber(6, l2);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(IRETURN);
            mv.visitLabel(l1);
            mv.visitLineNumber(8, l1);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitInsn(IREM);
            mv.visitMethodInsn(INVOKEVIRTUAL, "litil/cg/samples/Sample3", "gcd", "(II)I");
            mv.visitInsn(IRETURN);
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitLocalVariable("this", "Llitil/cg/samples/Sample3;", null, l0, l3, 0);
            mv.visitLocalVariable("a", "I", null, l0, l3, 1);
            mv.visitLocalVariable("b", "I", null, l0, l3, 2);
            mv.visitMaxs(4, 3);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
