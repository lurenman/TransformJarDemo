package cn.tongdun.android.plugin.excludeaar

import cn.tongdun.android.plugin.excludeaar.PluginConfig
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT
import static org.objectweb.asm.Opcodes.ACC_NATIVE

class ExcludeClassVisitor extends ClassVisitor {
    def TAG = 'ExcludeClassVisitor >'
    def String mOwner
    /**
     * 扩展选项
     */
    def PluginConfig mConfig

    ExcludeClassVisitor(int api, ClassVisitor classVisitor, PluginConfig config) {
        super(api, classVisitor)
        mConfig = config
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        println("${TAG} class name:${name} access:${access} signature:${signature} superName:${superName}")
        super.visit(version, access, name, signature, superName, interfaces)
    }

    @Override
    FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        return super.visitField(access, name, descriptor, signature, value)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }

    @Override
    void visitEnd() {
        super.visitEnd()
    }

}