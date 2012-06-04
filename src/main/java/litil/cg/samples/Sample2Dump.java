package litil.cg.samples;

import org.objectweb.asm.*;

public class Sample2Dump implements Opcodes {
    public static byte[] dump () throws Exception {

        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, "jaml/cg/samples/Sample2", null, "java/lang/Object", null);

        cw.visitSource("Sample2.java", null);

        {
            fv = cw.visitField(ACC_PUBLIC, "x", "I", null, null);
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
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLineNumber(4, l1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_3);
            mv.visitFieldInsn(PUTFIELD, "jaml/cg/samples/Sample2", "x", "I");
            mv.visitInsn(RETURN);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLocalVariable("this", "Ljaml/cg/samples/Sample2;", null, l0, l2, 0);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "eval", "(I)I", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(7, l0);
            mv.visitInsn(ICONST_5);
            mv.visitVarInsn(ISTORE, 2);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLineNumber(8, l1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "jaml/cg/samples/Sample2", "x", "I");
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitInsn(IMUL);
            mv.visitInsn(IADD);
            mv.visitInsn(IRETURN);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLocalVariable("this", "Ljaml/cg/samples/Sample2;", null, l0, l2, 0);
            mv.visitLocalVariable("y", "I", null, l0, l2, 1);
            mv.visitLocalVariable("z", "I", null, l1, l2, 2);
            mv.visitMaxs(3, 3);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "helios", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(12, l0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_5);
            mv.visitMethodInsn(INVOKEVIRTUAL, "jaml/cg/samples/Sample2", "eval", "(I)I");
            mv.visitInsn(POP);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLineNumber(13, l1);
            mv.visitInsn(RETURN);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLocalVariable("this", "Ljaml/cg/samples/Sample2;", null, l0, l2, 0);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
