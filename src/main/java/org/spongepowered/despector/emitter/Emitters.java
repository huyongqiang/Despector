/*
 * The MIT License (MIT)
 *
 * Copyright (c) Despector <https://despector.voxelgenesis.com>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.despector.emitter;

import org.spongepowered.despector.Language;
import org.spongepowered.despector.ast.insn.condition.AndCondition;
import org.spongepowered.despector.ast.insn.condition.BooleanCondition;
import org.spongepowered.despector.ast.insn.condition.CompareCondition;
import org.spongepowered.despector.ast.insn.condition.InverseCondition;
import org.spongepowered.despector.ast.insn.condition.OrCondition;
import org.spongepowered.despector.ast.insn.cst.DoubleConstant;
import org.spongepowered.despector.ast.insn.cst.FloatConstant;
import org.spongepowered.despector.ast.insn.cst.IntConstant;
import org.spongepowered.despector.ast.insn.cst.LongConstant;
import org.spongepowered.despector.ast.insn.cst.NullConstant;
import org.spongepowered.despector.ast.insn.cst.StringConstant;
import org.spongepowered.despector.ast.insn.cst.TypeConstant;
import org.spongepowered.despector.ast.insn.misc.Cast;
import org.spongepowered.despector.ast.insn.misc.InstanceOf;
import org.spongepowered.despector.ast.insn.misc.MultiNewArray;
import org.spongepowered.despector.ast.insn.misc.NewArray;
import org.spongepowered.despector.ast.insn.misc.NumberCompare;
import org.spongepowered.despector.ast.insn.misc.Ternary;
import org.spongepowered.despector.ast.insn.op.NegativeOperator;
import org.spongepowered.despector.ast.insn.op.Operator;
import org.spongepowered.despector.ast.insn.var.ArrayAccess;
import org.spongepowered.despector.ast.insn.var.InstanceFieldAccess;
import org.spongepowered.despector.ast.insn.var.LocalAccess;
import org.spongepowered.despector.ast.insn.var.StaticFieldAccess;
import org.spongepowered.despector.ast.kotlin.Elvis;
import org.spongepowered.despector.ast.kotlin.When;
import org.spongepowered.despector.ast.stmt.assign.ArrayAssignment;
import org.spongepowered.despector.ast.stmt.assign.FieldAssignment;
import org.spongepowered.despector.ast.stmt.assign.InstanceFieldAssignment;
import org.spongepowered.despector.ast.stmt.assign.LocalAssignment;
import org.spongepowered.despector.ast.stmt.assign.StaticFieldAssignment;
import org.spongepowered.despector.ast.stmt.branch.Break;
import org.spongepowered.despector.ast.stmt.branch.DoWhile;
import org.spongepowered.despector.ast.stmt.branch.For;
import org.spongepowered.despector.ast.stmt.branch.ForEach;
import org.spongepowered.despector.ast.stmt.branch.If;
import org.spongepowered.despector.ast.stmt.branch.Switch;
import org.spongepowered.despector.ast.stmt.branch.TryCatch;
import org.spongepowered.despector.ast.stmt.branch.While;
import org.spongepowered.despector.ast.stmt.invoke.InstanceMethodInvoke;
import org.spongepowered.despector.ast.stmt.invoke.InvokeStatement;
import org.spongepowered.despector.ast.stmt.invoke.Lambda;
import org.spongepowered.despector.ast.stmt.invoke.MethodReference;
import org.spongepowered.despector.ast.stmt.invoke.New;
import org.spongepowered.despector.ast.stmt.invoke.StaticMethodInvoke;
import org.spongepowered.despector.ast.stmt.misc.Comment;
import org.spongepowered.despector.ast.stmt.misc.Increment;
import org.spongepowered.despector.ast.stmt.misc.Return;
import org.spongepowered.despector.ast.stmt.misc.Throw;
import org.spongepowered.despector.ast.type.AnnotationEntry;
import org.spongepowered.despector.ast.type.ClassEntry;
import org.spongepowered.despector.ast.type.EnumEntry;
import org.spongepowered.despector.ast.type.FieldEntry;
import org.spongepowered.despector.ast.type.InterfaceEntry;
import org.spongepowered.despector.ast.type.MethodEntry;
import org.spongepowered.despector.emitter.bytecode.BytecodeEmitter;
import org.spongepowered.despector.emitter.bytecode.BytecodeEmitterContext;
import org.spongepowered.despector.emitter.bytecode.instruction.BytecodeDoubleConstantEmitter;
import org.spongepowered.despector.emitter.bytecode.instruction.BytecodeFloatConstantEmitter;
import org.spongepowered.despector.emitter.bytecode.instruction.BytecodeInstanceMethodInvokeEmitter;
import org.spongepowered.despector.emitter.bytecode.instruction.BytecodeIntConstantEmitter;
import org.spongepowered.despector.emitter.bytecode.instruction.BytecodeLocalAccessEmitter;
import org.spongepowered.despector.emitter.bytecode.instruction.BytecodeLongConstantEmitter;
import org.spongepowered.despector.emitter.bytecode.instruction.BytecodeNullConstantEmitter;
import org.spongepowered.despector.emitter.bytecode.instruction.BytecodeStaticFieldAccessEmitter;
import org.spongepowered.despector.emitter.bytecode.instruction.BytecodeStringConstantEmitter;
import org.spongepowered.despector.emitter.bytecode.instruction.BytecodeTypeConstantEmitter;
import org.spongepowered.despector.emitter.bytecode.statement.BytecodeIfEmitter;
import org.spongepowered.despector.emitter.bytecode.statement.BytecodeInvokeStatementEmitter;
import org.spongepowered.despector.emitter.bytecode.statement.BytecodeLocalAssignmentEmitter;
import org.spongepowered.despector.emitter.bytecode.statement.BytecodeReturnEmitter;
import org.spongepowered.despector.emitter.bytecode.type.BytecodeClassEntryEmitter;
import org.spongepowered.despector.emitter.bytecode.type.BytecodeMethodEntryEmitter;
import org.spongepowered.despector.emitter.java.JavaEmitter;
import org.spongepowered.despector.emitter.java.JavaEmitterContext;
import org.spongepowered.despector.emitter.java.condition.AndConditionEmitter;
import org.spongepowered.despector.emitter.java.condition.BooleanConditionEmitter;
import org.spongepowered.despector.emitter.java.condition.CompareConditionEmitter;
import org.spongepowered.despector.emitter.java.condition.InverseConditionEmitter;
import org.spongepowered.despector.emitter.java.condition.OrConditionEmitter;
import org.spongepowered.despector.emitter.java.instruction.ArrayLoadEmitter;
import org.spongepowered.despector.emitter.java.instruction.CastEmitter;
import org.spongepowered.despector.emitter.java.instruction.CompareEmitter;
import org.spongepowered.despector.emitter.java.instruction.DoubleConstantEmitter;
import org.spongepowered.despector.emitter.java.instruction.DynamicInvokeEmitter;
import org.spongepowered.despector.emitter.java.instruction.FieldAccessEmitter;
import org.spongepowered.despector.emitter.java.instruction.FloatConstantEmitter;
import org.spongepowered.despector.emitter.java.instruction.InstanceMethodInvokeEmitter;
import org.spongepowered.despector.emitter.java.instruction.InstanceOfEmitter;
import org.spongepowered.despector.emitter.java.instruction.IntConstantEmitter;
import org.spongepowered.despector.emitter.java.instruction.LocalAccessEmitter;
import org.spongepowered.despector.emitter.java.instruction.LongConstantEmitter;
import org.spongepowered.despector.emitter.java.instruction.MethodReferenceEmitter;
import org.spongepowered.despector.emitter.java.instruction.MultiNewArrayEmitter;
import org.spongepowered.despector.emitter.java.instruction.NegativeEmitter;
import org.spongepowered.despector.emitter.java.instruction.NewArrayEmitter;
import org.spongepowered.despector.emitter.java.instruction.NewEmitter;
import org.spongepowered.despector.emitter.java.instruction.NullConstantEmitter;
import org.spongepowered.despector.emitter.java.instruction.OperatorEmitter;
import org.spongepowered.despector.emitter.java.instruction.StaticMethodInvokeEmitter;
import org.spongepowered.despector.emitter.java.instruction.StringConstantEmitter;
import org.spongepowered.despector.emitter.java.instruction.TernaryEmitter;
import org.spongepowered.despector.emitter.java.instruction.TypeConstantEmitter;
import org.spongepowered.despector.emitter.java.special.AnnotationEmitter;
import org.spongepowered.despector.emitter.java.special.AnonymousClassEmitter;
import org.spongepowered.despector.emitter.java.special.GenericsEmitter;
import org.spongepowered.despector.emitter.java.special.PackageEmitter;
import org.spongepowered.despector.emitter.java.special.PackageInfoEmitter;
import org.spongepowered.despector.emitter.java.statement.ArrayAssignmentEmitter;
import org.spongepowered.despector.emitter.java.statement.BreakEmitter;
import org.spongepowered.despector.emitter.java.statement.CommentEmitter;
import org.spongepowered.despector.emitter.java.statement.DoWhileEmitter;
import org.spongepowered.despector.emitter.java.statement.FieldAssignmentEmitter;
import org.spongepowered.despector.emitter.java.statement.ForEachEmitter;
import org.spongepowered.despector.emitter.java.statement.ForEmitter;
import org.spongepowered.despector.emitter.java.statement.IfEmitter;
import org.spongepowered.despector.emitter.java.statement.IncrementEmitter;
import org.spongepowered.despector.emitter.java.statement.InvokeEmitter;
import org.spongepowered.despector.emitter.java.statement.LocalAssignmentEmitter;
import org.spongepowered.despector.emitter.java.statement.ReturnEmitter;
import org.spongepowered.despector.emitter.java.statement.SwitchEmitter;
import org.spongepowered.despector.emitter.java.statement.ThrowEmitter;
import org.spongepowered.despector.emitter.java.statement.TryCatchEmitter;
import org.spongepowered.despector.emitter.java.statement.WhileEmitter;
import org.spongepowered.despector.emitter.java.type.AnnotationEntryEmitter;
import org.spongepowered.despector.emitter.java.type.ClassEntryEmitter;
import org.spongepowered.despector.emitter.java.type.EnumEntryEmitter;
import org.spongepowered.despector.emitter.java.type.FieldEntryEmitter;
import org.spongepowered.despector.emitter.java.type.InterfaceEntryEmitter;
import org.spongepowered.despector.emitter.java.type.MethodEntryEmitter;
import org.spongepowered.despector.emitter.kotlin.KotlinEmitter;
import org.spongepowered.despector.emitter.kotlin.condition.KotlinBooleanConditionEmitter;
import org.spongepowered.despector.emitter.kotlin.condition.KotlinCompareConditionEmitter;
import org.spongepowered.despector.emitter.kotlin.condition.KotlinInverseConditionEmitter;
import org.spongepowered.despector.emitter.kotlin.instruction.ElvisEmitter;
import org.spongepowered.despector.emitter.kotlin.instruction.KotlinCastEmitter;
import org.spongepowered.despector.emitter.kotlin.instruction.KotlinInstanceOfEmitter;
import org.spongepowered.despector.emitter.kotlin.instruction.KotlinNewEmitter;
import org.spongepowered.despector.emitter.kotlin.instruction.KotlinOperatorEmitter;
import org.spongepowered.despector.emitter.kotlin.instruction.KotlinTernaryEmitter;
import org.spongepowered.despector.emitter.kotlin.instruction.WhenEmitter;
import org.spongepowered.despector.emitter.kotlin.instruction.method.KotlinInstanceMethodInvokeEmitter;
import org.spongepowered.despector.emitter.kotlin.instruction.method.KotlinStaticMethodInvokeEmitter;
import org.spongepowered.despector.emitter.kotlin.special.KotlinCompanionClassEmitter;
import org.spongepowered.despector.emitter.kotlin.special.KotlinDataClassEmitter;
import org.spongepowered.despector.emitter.kotlin.special.KotlinGenericsEmitter;
import org.spongepowered.despector.emitter.kotlin.special.KotlinPackageEmitter;
import org.spongepowered.despector.emitter.kotlin.statement.KotlinForEachEmitter;
import org.spongepowered.despector.emitter.kotlin.statement.KotlinForEmitter;
import org.spongepowered.despector.emitter.kotlin.statement.KotlinInvokeEmitter;
import org.spongepowered.despector.emitter.kotlin.statement.KotlinLocalAssignmentEmitter;
import org.spongepowered.despector.emitter.kotlin.type.KotlinClassEntryEmitter;
import org.spongepowered.despector.emitter.kotlin.type.KotlinEnumEntryEmitter;
import org.spongepowered.despector.emitter.kotlin.type.KotlinMethodEntryEmitter;

import java.util.EnumMap;

/**
 * Standard emitters.
 */
public final class Emitters {

    public static final EmitterSet JAVA_SET = new EmitterSet();
    public static final EmitterSet KOTLIN_SET = new EmitterSet();
    public static final EmitterSet BYTECODE_SET = new EmitterSet();

    public static final Emitter<JavaEmitterContext> JAVA = new JavaEmitter();
    public static final Emitter<JavaEmitterContext> KOTLIN = new KotlinEmitter();
    public static final Emitter<?> WILD = new WildEmitter();
    public static final Emitter<BytecodeEmitterContext> BYTECODE = new BytecodeEmitter();

    @SuppressWarnings("rawtypes")
    private static final EnumMap<Language, Emitter> EMITTERS = new EnumMap<>(Language.class);

    static {

        JAVA_SET.setSpecialEmitter(AnnotationEmitter.class, new AnnotationEmitter());
        JAVA_SET.setSpecialEmitter(GenericsEmitter.class, new GenericsEmitter());
        JAVA_SET.setSpecialEmitter(AnonymousClassEmitter.class, new AnonymousClassEmitter());
        JAVA_SET.setSpecialEmitter(PackageInfoEmitter.class, new PackageInfoEmitter());
        JAVA_SET.setSpecialEmitter(PackageEmitter.class, new PackageEmitter());

        JAVA_SET.setAstEmitter(ClassEntry.class, new ClassEntryEmitter());
        JAVA_SET.setAstEmitter(EnumEntry.class, new EnumEntryEmitter());
        JAVA_SET.setAstEmitter(InterfaceEntry.class, new InterfaceEntryEmitter());
        JAVA_SET.setAstEmitter(AnnotationEntry.class, new AnnotationEntryEmitter());

        JAVA_SET.setAstEmitter(FieldEntry.class, new FieldEntryEmitter());
        JAVA_SET.setAstEmitter(MethodEntry.class, new MethodEntryEmitter());

        JAVA_SET.setStatementEmitter(ArrayAssignment.class, new ArrayAssignmentEmitter());
        JAVA_SET.setStatementEmitter(Break.class, new BreakEmitter());
        JAVA_SET.setStatementEmitter(Comment.class, new CommentEmitter());
        JAVA_SET.setStatementEmitter(DoWhile.class, new DoWhileEmitter());
        FieldAssignmentEmitter fld_assign = new FieldAssignmentEmitter();
        JAVA_SET.setStatementEmitter(FieldAssignment.class, fld_assign);
        JAVA_SET.setStatementEmitter(InstanceFieldAssignment.class, fld_assign);
        JAVA_SET.setStatementEmitter(StaticFieldAssignment.class, fld_assign);
        JAVA_SET.setStatementEmitter(For.class, new ForEmitter());
        JAVA_SET.setStatementEmitter(ForEach.class, new ForEachEmitter());
        JAVA_SET.setStatementEmitter(If.class, new IfEmitter());
        JAVA_SET.setStatementEmitter(Increment.class, new IncrementEmitter());
        JAVA_SET.setStatementEmitter(InvokeStatement.class, new InvokeEmitter());
        JAVA_SET.setStatementEmitter(LocalAssignment.class, new LocalAssignmentEmitter());
        JAVA_SET.setStatementEmitter(Return.class, new ReturnEmitter());
        JAVA_SET.setStatementEmitter(Switch.class, new SwitchEmitter());
        JAVA_SET.setStatementEmitter(Throw.class, new ThrowEmitter());
        JAVA_SET.setStatementEmitter(TryCatch.class, new TryCatchEmitter());
        JAVA_SET.setStatementEmitter(While.class, new WhileEmitter());

        JAVA_SET.setInstructionEmitter(ArrayAccess.class, new ArrayLoadEmitter());
        JAVA_SET.setInstructionEmitter(Cast.class, new CastEmitter());
        JAVA_SET.setInstructionEmitter(NumberCompare.class, new CompareEmitter());
        JAVA_SET.setInstructionEmitter(DoubleConstant.class, new DoubleConstantEmitter());
        FieldAccessEmitter fld = new FieldAccessEmitter();
        JAVA_SET.setInstructionEmitter(InstanceFieldAccess.class, fld);
        JAVA_SET.setInstructionEmitter(FloatConstant.class, new FloatConstantEmitter());
        JAVA_SET.setInstructionEmitter(InstanceMethodInvoke.class, new InstanceMethodInvokeEmitter());
        JAVA_SET.setInstructionEmitter(InstanceOf.class, new InstanceOfEmitter());
        JAVA_SET.setInstructionEmitter(IntConstant.class, new IntConstantEmitter());
        JAVA_SET.setInstructionEmitter(LocalAccess.class, new LocalAccessEmitter());
        JAVA_SET.setInstructionEmitter(LongConstant.class, new LongConstantEmitter());
        JAVA_SET.setInstructionEmitter(NegativeOperator.class, new NegativeEmitter());
        JAVA_SET.setInstructionEmitter(NewArray.class, new NewArrayEmitter());
        JAVA_SET.setInstructionEmitter(New.class, new NewEmitter());
        JAVA_SET.setInstructionEmitter(NullConstant.class, new NullConstantEmitter());
        OperatorEmitter op = new OperatorEmitter();
        JAVA_SET.setInstructionEmitter(Operator.class, op);
        JAVA_SET.setInstructionEmitter(StaticMethodInvoke.class, new StaticMethodInvokeEmitter());
        JAVA_SET.setInstructionEmitter(StringConstant.class, new StringConstantEmitter());
        JAVA_SET.setInstructionEmitter(Ternary.class, new TernaryEmitter());
        JAVA_SET.setInstructionEmitter(TypeConstant.class, new TypeConstantEmitter());
        JAVA_SET.setInstructionEmitter(StaticFieldAccess.class, fld);
        JAVA_SET.setInstructionEmitter(Lambda.class, new DynamicInvokeEmitter());
        JAVA_SET.setInstructionEmitter(MultiNewArray.class, new MultiNewArrayEmitter());
        JAVA_SET.setInstructionEmitter(MethodReference.class, new MethodReferenceEmitter());

        JAVA_SET.setConditionEmitter(AndCondition.class, new AndConditionEmitter());
        JAVA_SET.setConditionEmitter(OrCondition.class, new OrConditionEmitter());
        JAVA_SET.setConditionEmitter(InverseCondition.class, new InverseConditionEmitter());
        JAVA_SET.setConditionEmitter(CompareCondition.class, new CompareConditionEmitter());
        JAVA_SET.setConditionEmitter(BooleanCondition.class, new BooleanConditionEmitter());

        KOTLIN_SET.clone(JAVA_SET);

        KOTLIN_SET.setAstEmitter(ClassEntry.class, new KotlinClassEntryEmitter());
        KOTLIN_SET.setAstEmitter(EnumEntry.class, new KotlinEnumEntryEmitter());
        KOTLIN_SET.setAstEmitter(MethodEntry.class, new KotlinMethodEntryEmitter());

        KOTLIN_SET.setSpecialEmitter(KotlinDataClassEmitter.class, new KotlinDataClassEmitter());
        KOTLIN_SET.setSpecialEmitter(KotlinCompanionClassEmitter.class, new KotlinCompanionClassEmitter());
        KOTLIN_SET.setSpecialEmitter(PackageEmitter.class, new KotlinPackageEmitter());
        KOTLIN_SET.setSpecialEmitter(GenericsEmitter.class, new KotlinGenericsEmitter());

        KOTLIN_SET.setStatementEmitter(InvokeStatement.class, new KotlinInvokeEmitter());
        KOTLIN_SET.setStatementEmitter(LocalAssignment.class, new KotlinLocalAssignmentEmitter());
        KOTLIN_SET.setStatementEmitter(ForEach.class, new KotlinForEachEmitter());
        KOTLIN_SET.setStatementEmitter(For.class, new KotlinForEmitter());

        KOTLIN_SET.setInstructionEmitter(InstanceMethodInvoke.class, new KotlinInstanceMethodInvokeEmitter());
        KOTLIN_SET.setInstructionEmitter(StaticMethodInvoke.class, new KotlinStaticMethodInvokeEmitter());
        KOTLIN_SET.setInstructionEmitter(Ternary.class, new KotlinTernaryEmitter());
        KOTLIN_SET.setInstructionEmitter(InstanceOf.class, new KotlinInstanceOfEmitter());
        KOTLIN_SET.setInstructionEmitter(Cast.class, new KotlinCastEmitter());
        KOTLIN_SET.setInstructionEmitter(Elvis.class, new ElvisEmitter());
        KOTLIN_SET.setInstructionEmitter(When.class, new WhenEmitter());
        KOTLIN_SET.setInstructionEmitter(Operator.class, new KotlinOperatorEmitter());
        KOTLIN_SET.setInstructionEmitter(New.class, new KotlinNewEmitter());

        KOTLIN_SET.setConditionEmitter(BooleanCondition.class, new KotlinBooleanConditionEmitter());
        KOTLIN_SET.setConditionEmitter(CompareCondition.class, new KotlinCompareConditionEmitter());
        KOTLIN_SET.setConditionEmitter(InverseCondition.class, new KotlinInverseConditionEmitter());

        EMITTERS.put(Language.JAVA, JAVA);
        EMITTERS.put(Language.KOTLIN, KOTLIN);
        EMITTERS.put(Language.ANY, WILD);

        BYTECODE_SET.setAstEmitter(ClassEntry.class, new BytecodeClassEntryEmitter());
        BYTECODE_SET.setAstEmitter(MethodEntry.class, new BytecodeMethodEntryEmitter());

        BYTECODE_SET.setStatementEmitter(LocalAssignment.class, new BytecodeLocalAssignmentEmitter());
        BYTECODE_SET.setStatementEmitter(Return.class, new BytecodeReturnEmitter());
        BYTECODE_SET.setStatementEmitter(InvokeStatement.class, new BytecodeInvokeStatementEmitter());
        BYTECODE_SET.setStatementEmitter(If.class, new BytecodeIfEmitter());

        BYTECODE_SET.setInstructionEmitter(IntConstant.class, new BytecodeIntConstantEmitter());
        BYTECODE_SET.setInstructionEmitter(InstanceMethodInvoke.class, new BytecodeInstanceMethodInvokeEmitter());
        BYTECODE_SET.setInstructionEmitter(LocalAccess.class, new BytecodeLocalAccessEmitter());
        BYTECODE_SET.setInstructionEmitter(StringConstant.class, new BytecodeStringConstantEmitter());
        BYTECODE_SET.setInstructionEmitter(StaticFieldAccess.class, new BytecodeStaticFieldAccessEmitter());
        BYTECODE_SET.setInstructionEmitter(FloatConstant.class, new BytecodeFloatConstantEmitter());
        BYTECODE_SET.setInstructionEmitter(DoubleConstant.class, new BytecodeDoubleConstantEmitter());
        BYTECODE_SET.setInstructionEmitter(LongConstant.class, new BytecodeLongConstantEmitter());
        BYTECODE_SET.setInstructionEmitter(NullConstant.class, new BytecodeNullConstantEmitter());
        BYTECODE_SET.setInstructionEmitter(TypeConstant.class, new BytecodeTypeConstantEmitter());
    }

    /**
     * Gets the emitter for the given language.
     */
    @SuppressWarnings("unchecked")
    public static <E extends AbstractEmitterContext> Emitter<E> get(Language lang) {
        return EMITTERS.get(lang);
    }

    private Emitters() {

    }

}
