package cn.tongdun.android.plugin.excludeaar

import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter


class ProcessClassVisitor extends ClassVisitor {
    def TAG = 'ProcessClassVisitor >'
    def String mOwner
    /**
     * 扩展选项
     */
    def PluginConfig mConfig

    ProcessClassVisitor(int api, ClassVisitor classVisitor, PluginConfig config) {
        super(api, classVisitor)
        mConfig = config
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        println("${TAG} class name:${name} access:${access} signature:${signature} superName:${superName}")
        mOwner = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    @Override
    FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        return super.visitField(access, name, descriptor, signature, value)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        println("${TAG} access:${access} method name:${name} descriptor:${descriptor}  signature:${signature} exceptions:${exceptions}")
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (mv != null && "a".equals(name) && access == 25) {
            mv = new MethodEnterAdapter(Opcodes.ASM7, mv, access, name, descriptor, mOwner)
        }
        return mv
    }

    @Override
    void visitEnd() {
//        FieldVisitor fv = cv.visitField(Opcodes.ACC_PUBLIC, "dataValue", "Ljava/lang/String;", null, null);
//        fv.visitEnd()

        //增加一个方法
        MethodVisitor methodVisitor = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "handFpData", "(Lorg/json/JSONObject;)V", null, null);
        methodVisitor.visitCode();
        Label startLabel = new Label();
        Label endLabel = new Label();
        Label handlerLabel = new Label();
        methodVisitor.visitTryCatchBlock(startLabel, endLabel, handlerLabel, "java/lang/Exception");
        methodVisitor.visitLabel(startLabel);

        methodVisitor.visitLdcInsn("com.fingerprintjs.android.fpjs_pro_demo.util.DataUtil");
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        methodVisitor.visitVarInsn(Opcodes.ASTORE, 1);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitLdcInsn("handleData");
        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ICONST_0);
        methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/String;"));
        methodVisitor.visitInsn(Opcodes.AASTORE);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
        methodVisitor.visitVarInsn(Opcodes.ASTORE, 2);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ICONST_0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/json/JSONObject", "toString", "()Ljava/lang/String;", false);
        methodVisitor.visitInsn(Opcodes.AASTORE);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitLabel(endLabel);

        Label returnLabel = new Label();
        methodVisitor.visitJumpInsn(Opcodes.GOTO, returnLabel);
        methodVisitor.visitLabel(handlerLabel);

        methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Exception"});
        methodVisitor.visitVarInsn(Opcodes.ASTORE, 1);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false);
        methodVisitor.visitLabel(returnLabel);

        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(6, 3);
        methodVisitor.visitEnd();

        super.visitEnd()
    }

    private static class MethodEnterAdapter extends AdviceAdapter {
        private String mMethod;
        private int mAccess;
        private Object mValue; //方法传入的参数
        private int mLine; //代码行号
        public MethodEnterAdapter(int api, MethodVisitor methodVisitor, int access, String name, String descriptor, String owner) {
            super(api, methodVisitor, access, name, descriptor)
            mMethod = name;
            mAccess = access;
        }

        @Override
        public void visitCode() {
            Label label = new Label()
            this.visitLabel(label)
            this.visitLineNumber(1, label)
            this.visitLdcInsn("FP_LOG")
            String log = "call method: ${mMethod} line:${mLine}"
            this.visitLdcInsn(log)
            this.visitMethodInsn(INVOKESTATIC, "android/util/Log", "d", "(Ljava/lang/String;Ljava/lang/String;)I", false);
            this.visitInsn(POP)

            super.visitCode()
        }

        @Override
        public void visitLdcInsn(Object value) {
            mValue = value;
            super.visitLdcInsn(value)
        }

        @Override
        void visitInsn(int opcode) {
            if (opcode == Opcodes.ATHROW || (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)) {
                super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                super.visitLdcInsn("Method Exit...");
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            }
            super.visitInsn(opcode)
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            mLine = line;
            if (line == 15) {
                def methodVisitor = this;
                methodVisitor.with {
                    //插入日志
                    Label labelog = new Label();
                    visitLabel(labelog);
                    visitLineNumber(16, labelog);
                    visitLdcInsn("FP_LOG the value :");
                    visitVarInsn(ALOAD, 2);
                    visitMethodInsn(INVOKEVIRTUAL, "org/json/JSONObject", "toString", "()Ljava/lang/String;", false);
                    visitMethodInsn(INVOKESTATIC, "android/util/Log", "d", "(Ljava/lang/String;Ljava/lang/String;)I", false);
                    visitInsn(POP)

                    visitVarInsn(ALOAD, 2);
                    visitMethodInsn(INVOKESTATIC, "fpprog/d", "handFpData", "(Lorg/json/JSONObject;)V", false);
                }

                //插入反射代码
                /*              Label startLabel = new Label();
                              Label endLabel = new Label();
                              Label handlerLabel = new Label();
                              methodVisitor.visitTryCatchBlock(startLabel, endLabel, handlerLabel, "java/lang/Exception");

                              methodVisitor.visitLineNumber(19, startLabel);
                              methodVisitor.visitLdcInsn("com.fingerprintjs.android.fpjs_pro_demo.util.DataUtil");
                              methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
                              methodVisitor.visitVarInsn(ASTORE, 3);
                              Label label7 = new Label();
                              methodVisitor.visitLabel(label7);
                              methodVisitor.visitLineNumber(20, label7);
                              methodVisitor.visitVarInsn(ALOAD, 3);
                              methodVisitor.visitLdcInsn("handleData");
                              methodVisitor.visitInsn(ICONST_1);
                              methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Class");
                              methodVisitor.visitInsn(DUP);
                              methodVisitor.visitInsn(ICONST_0);
                              methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/String;"));
                              methodVisitor.visitInsn(AASTORE);
                              methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
                              methodVisitor.visitVarInsn(ASTORE, 4);
                              Label label8 = new Label();
                              methodVisitor.visitLabel(label8);
                              methodVisitor.visitLineNumber(21, label8);
                              methodVisitor.visitVarInsn(ALOAD, 4);
                              methodVisitor.visitInsn(ACONST_NULL);
                              methodVisitor.visitInsn(ICONST_1);
                              methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
                              methodVisitor.visitInsn(DUP);
                              methodVisitor.visitInsn(ICONST_0);
                              methodVisitor.visitVarInsn(ALOAD, 2);
                              methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "org/json/JSONObject", "toString", "()Ljava/lang/String;", false);
                              methodVisitor.visitInsn(AASTORE);
                              methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
                              methodVisitor.visitInsn(POP);

                              methodVisitor.visitLabel(endLabel);
                              methodVisitor.visitLineNumber(24, endLabel);
                              Label returnLabel = new Label();
                              methodVisitor.visitJumpInsn(GOTO, returnLabel);

                              methodVisitor.visitLabel(handlerLabel);
                              methodVisitor.visitLineNumber(22, handlerLabel);
                             // methodVisitor.visitFrame(Opcodes.F_FULL, 3, new Object[]{"java/lang/String", "java/lang/String", "org/json/JSONObject"}, 1, new Object[]{"java/lang/Exception"});
                              methodVisitor.visitVarInsn(ASTORE, 3);
                              Label label10 = new Label();
                              methodVisitor.visitLabel(label10);
                              methodVisitor.visitLineNumber(23, label10);
                              methodVisitor.visitVarInsn(ALOAD, 3);
                              methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false);
                              methodVisitor.visitLabel(returnLabel);
                              //methodVisitor.visitLineNumber(26, returnLabel);
                             // methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);*/

            }
            super.visitLineNumber(line, start)
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            super.visitVarInsn(opcode, var)
        }

        @Override
        public void endMethod() {
            super.endMethod()
        }
    }

}