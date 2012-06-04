package litil.cg;

import litil.TypeScope;
import litil.ast.*;
import litil.cg.ast.BcNode;
import litil.eval.Prelude;
import litil.tc.HMTypeChecker;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeGen {
    public byte[] gen(Program p) {


        HMTypeChecker hm = new HMTypeChecker();
        TypeScope tenv = Prelude.trootScope().child();
        Names names = new Names();

        List<BcNode> bc = new ArrayList<BcNode>();
        for (Instruction inst : p.instructions) {
            bc.addAll(visitTopLevel(inst, names, hm, tenv));
        }

        return visitClass(bc);
    }

    private byte[] visitClass(List<BcNode> bc) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_6, ACC_PUBLIC, "litil/Main", null, "java/lang/Object", null);

        visitConstr(bc, cw);

        for (BcNode bcNode : bc) {
            if (bcNode instanceof BcNode.DeclField) {
                BcNode.DeclField fd = (BcNode.DeclField) bcNode;
                cw.visitField(ACC_PUBLIC, fd.name, javaTypeOf(fd.type), null, null).visitEnd();
            }
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    private void visitConstr(List<BcNode> bc, ClassWriter cw) {
        MethodVisitor constrv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        constrv.visitCode();
        constrv.visitVarInsn(ALOAD, 0);
        constrv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

        for (BcNode bcNode : bc) {
            if (bcNode instanceof BcNode.InitField) {
                BcNode.InitField inif = (BcNode.InitField) bcNode;
                for (BcNode initfNode : inif.bc) {
                    constrv.visitInsn(-1);
                }
            }
        }
        constrv.visitInsn(RETURN);
        constrv.visitLocalVariable("this", "Llitil/Main;", null, null, null, 0);
        constrv.visitMaxs(1, 1);
        constrv.visitEnd();
    }

    private void visitMethod(List<BcNode> bc, MethodVisitor mw) {


        for (BcNode bcNode : bc) {
            if (bcNode instanceof BcNode.Push) {
                BcNode.Push push = (BcNode.Push) bcNode;

            }
        }

    }

    private List<BcNode> visitTopLevel(AstNode n, Names names, HMTypeChecker hm, TypeScope tenv) {

        if (n instanceof DestructuringLetBinding) {
            List<BcNode> res = new ArrayList<BcNode>();

            DestructuringLetBinding let = (DestructuringLetBinding) n;
            Type type = hm.analyze(n, tenv);
            if (let.args.isEmpty()) {//field
                String letName = ((Pattern.IdPattern) let.main).name;//FIXME: handle other patterns
                String mappedName = mapName(letName, names);
                res.add(new BcNode.DeclField(mappedName, type));

                //handle value
                List<BcNode> fieldInit = new ArrayList<BcNode>();
                for (Instruction instr : let.instructions) {
                    fieldInit.addAll(visit(instr, names, hm, tenv.child()));
                }
                res.add(new BcNode.InitField(mappedName, type, fieldInit));
                return res;
            } else {//method
                throw new IllegalArgumentException("Unsupported " + n);

            }
        } else {
            throw new IllegalArgumentException("Unsupported " + n);

        }
    }

    private List<? extends BcNode> visit(AstNode n, Names names, HMTypeChecker hm, TypeScope tenv) {

        if (n instanceof Expr) {
            if (n instanceof Expr.ENum) {
                return Arrays.asList(new BcNode.Push(((Expr.ENum) n).value));
            } else {
                throw new IllegalArgumentException("Unsupported " + n);
            }
        } else {
            throw new IllegalArgumentException("Unsupported " + n);

        }
    }

    private void visit(AstNode n, ClassWriter cw, Names names, HMTypeChecker hm, TypeScope tenv) {
        if (n instanceof DestructuringLetBinding) {
            DestructuringLetBinding let = (DestructuringLetBinding) n;
            Type type = hm.analyze(n, tenv);
            if (let.args.isEmpty()) {//field
                String letName = ((Pattern.IdPattern) let.main).name;//FIXME: handle other patterns
                String mappedName = mapName(letName, names);
                cw.visitField(ACC_PUBLIC, mappedName, javaTypeOf(type), null, null).visitEnd();
                //handle value
            } else {//method
                throw new IllegalArgumentException("Unsupported " + n);

            }
        }
        if (n instanceof LetBinding) {
            LetBinding let = (LetBinding) n;
            String mappedName = mapName(let.name, names);
            if (let.args.isEmpty()) {//field

                cw.visitField(ACC_PUBLIC, mappedName, javaTypeOf(let.type), null, null).visitEnd();
                //handle value
            } else {//method
                StringBuilder methSig = new StringBuilder("(");
                for (Named arg : let.args) {
                    methSig.append(javaTypeOf(arg.type));
                }
                methSig.append(")").append(javaTypeOf(let.type));
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, mappedName, methSig.toString(), null, null);

            }
        } else if (n instanceof DataDecl) {
            DataDecl data = (DataDecl) n;
        }
    }

    private String mapName(String name, Names names) {
        String mappedName = names.get(name);
        if (mappedName == null) {
            mappedName = name;
        } else {
            mappedName = unique(name);
        }
        names.map(name, mappedName);
        return mappedName;
    }

    private static int idx = 0;


    private String unique(String name) {
        return name + "-" + (idx++);
    }

    private void visit(Instruction n, MethodVisitor mv) {
        if (n instanceof LetBinding) {
            LetBinding let = (LetBinding) n;
            if (let.args.isEmpty()) {//field
                //mv.v
                //cw.visitField(ACC_PRIVATE, let.name, javaTypeOf(let.type), null, null).visitEnd();
                //handle value
            } else {//method
                StringBuilder methSig = new StringBuilder("(");
                for (Named arg : let.args) {
                    methSig.append(javaTypeOf(arg.type));
                }
                methSig.append(")").append(javaTypeOf(let.type));
                //MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, let.name, methSig.toString(), null, null);

            }
        }
    }

    private String javaTypeOf(Type type) {
        if (type == Type.BOOL) {
            return "Ljava/lang/Boolean;";
        } else if (type == Type.INT) {
            return "Ljava/lang/Integer;";
        } else if (type == Type.STR) {
            return "Ljava/lang/String;";
        } else {
            throw new UnsupportedOperationException("Unsopported type " + type);
        }
    }
}
