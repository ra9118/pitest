/*
 * Copyright 2010 Henry Coles
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.pitest.dependency;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.pitest.bytecode.ASMVersion;
import org.pitest.dependency.DependencyAccess.Member;

import java.util.function.Consumer;

class DependencyClassVisitor extends ClassVisitor {

  private final Consumer<DependencyAccess> typeReceiver;
  private String                              className;

  protected DependencyClassVisitor(final ClassVisitor visitor,
      final Consumer<DependencyAccess> typeReceiver) {
    super(ASMVersion.ASM_VERSION, visitor);
    this.typeReceiver = filterOutJavaLangObject(typeReceiver);
  }

  private Consumer<DependencyAccess> filterOutJavaLangObject(
      final Consumer<DependencyAccess> child) {
    return a -> {
      if (!a.getDest().getOwner().equals("java/lang/Object")) {
        child.accept(a);
      }

    };
  }

  @Override
  public void visit(final int version, final int access, final String name,
      final String signature, final String superName, final String[] interfaces) {
    this.className = name;
  }

  @Override
  public MethodVisitor visitMethod(final int access, final String name,
      final String desc, final String signature, final String[] exceptions) {
    final MethodVisitor methodVisitor = this.cv.visitMethod(access, name, desc,
        signature, exceptions);

    final Member me = new Member(this.className, name);
    return new DependencyAnalysisMethodVisitor(me, methodVisitor,
        this.typeReceiver);
  }

  private static class DependencyAnalysisMethodVisitor extends MethodVisitor {

    private final Member                        member;
    private final Consumer<DependencyAccess> typeReceiver;

    DependencyAnalysisMethodVisitor(final Member member,
        final MethodVisitor methodVisitor,
        final Consumer<DependencyAccess> typeReceiver) {
      super(ASMVersion.ASM_VERSION, methodVisitor);
      this.typeReceiver = typeReceiver;
      this.member = member;
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
        final String name, final String desc, boolean itf) {
      this.typeReceiver.accept(new DependencyAccess(this.member, new Member(
          owner, name)));
      this.mv.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
        final String name, final String desc) {
      this.typeReceiver.accept(new DependencyAccess(this.member, new Member(
          owner, name)));
      this.mv.visitFieldInsn(opcode, owner, name, desc);
    }
  }

}
