/*
 * The MIT License (MIT)
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
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
package org.spongepowered.despector.ast.io.insn;

import static org.objectweb.asm.Opcodes.*;
import static org.spongepowered.despector.util.ConditionUtil.inverse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.despector.ast.io.insn.Locals.Local;
import org.spongepowered.despector.ast.io.insn.Locals.LocalInstance;
import org.spongepowered.despector.ast.members.insn.Statement;
import org.spongepowered.despector.ast.members.insn.StatementBlock;
import org.spongepowered.despector.ast.members.insn.arg.CastArg;
import org.spongepowered.despector.ast.members.insn.arg.CompareArg;
import org.spongepowered.despector.ast.members.insn.arg.InstanceOfArg;
import org.spongepowered.despector.ast.members.insn.arg.Instruction;
import org.spongepowered.despector.ast.members.insn.arg.NewArrayArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.DoubleConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.FloatConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.IntConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.LongConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.NullConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.StringConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.cst.TypeConstantArg;
import org.spongepowered.despector.ast.members.insn.arg.field.ArrayLoadArg;
import org.spongepowered.despector.ast.members.insn.arg.field.FieldArg;
import org.spongepowered.despector.ast.members.insn.arg.field.InstanceFieldArg;
import org.spongepowered.despector.ast.members.insn.arg.field.LocalArg;
import org.spongepowered.despector.ast.members.insn.arg.field.StaticFieldArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.AddArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.DivideArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.MultiplyArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.NegArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.RemainderInstruction;
import org.spongepowered.despector.ast.members.insn.arg.operator.ShiftLeftArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.ShiftRightArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.SubtractArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.UnsignedShiftRightArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.bitwise.AndInstruction;
import org.spongepowered.despector.ast.members.insn.arg.operator.bitwise.OrArg;
import org.spongepowered.despector.ast.members.insn.arg.operator.bitwise.XorArg;
import org.spongepowered.despector.ast.members.insn.assign.ArrayAssignment;
import org.spongepowered.despector.ast.members.insn.assign.FieldAssignment;
import org.spongepowered.despector.ast.members.insn.assign.InstanceFieldAssignment;
import org.spongepowered.despector.ast.members.insn.assign.LocalAssignment;
import org.spongepowered.despector.ast.members.insn.assign.StaticFieldAssignment;
import org.spongepowered.despector.ast.members.insn.branch.DoWhile;
import org.spongepowered.despector.ast.members.insn.branch.For;
import org.spongepowered.despector.ast.members.insn.branch.If;
import org.spongepowered.despector.ast.members.insn.branch.Switch;
import org.spongepowered.despector.ast.members.insn.branch.Switch.Case;
import org.spongepowered.despector.ast.members.insn.branch.While;
import org.spongepowered.despector.ast.members.insn.branch.condition.AndCondition;
import org.spongepowered.despector.ast.members.insn.branch.condition.BooleanCondition;
import org.spongepowered.despector.ast.members.insn.branch.condition.CompareCondition;
import org.spongepowered.despector.ast.members.insn.branch.condition.Condition;
import org.spongepowered.despector.ast.members.insn.branch.condition.OrCondition;
import org.spongepowered.despector.ast.members.insn.function.InstanceMethodInvoke;
import org.spongepowered.despector.ast.members.insn.function.InvokeStatement;
import org.spongepowered.despector.ast.members.insn.function.New;
import org.spongepowered.despector.ast.members.insn.function.StaticMethodInvoke;
import org.spongepowered.despector.ast.members.insn.misc.Increment;
import org.spongepowered.despector.ast.members.insn.misc.Return;
import org.spongepowered.despector.ast.members.insn.misc.Throw;
import org.spongepowered.despector.util.AstUtil;
import org.spongepowered.despector.util.ConditionUtil;
import org.spongepowered.despector.util.TypeHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The opcode decompiler which takes as input a list of opcode instructions and
 * produces a complete ast.
 * 
 * <p> The paper 'No More Gotos: Decompilation Using Pattern-Independent
 * Control-Flow Structuring and Semantics-Preserving Transformations' by Yakdan,
 * Eschweiler, Gerhards-Padilla, and Smith would be a helpful resource for
 * anyone looking to understand the code here. While not implemented exactly
 * word for word you will find many similar concepts used here. It can be found
 * at http://www.internetsociety.org/sites/default/files/11_4_2.pdf. </p>
 */
public class OpcodeDecompiler {

    @SuppressWarnings("unchecked")
    public static StatementBlock decompile(InsnList instructions, Locals locals, List<TryCatchBlockNode> tryCatchBlocks, DecompilerOptions options) {
        List<AbstractInsnNode> ops = Lists.newArrayList(instructions.iterator());
        StatementBlock block = new StatementBlock(StatementBlock.Type.METHOD, locals);

        Map<Label, Integer> label_indices = Maps.newHashMap();
        for (int index = 0; index < ops.size(); index++) {
            AbstractInsnNode next = ops.get(index);
            if (next instanceof LabelNode) {
                label_indices.put(((LabelNode) next).getLabel(), index);
            }
        }
        locals.bakeInstances(label_indices);

        // Form a graph by breaking up the opcodes at every jump and every label
        // targetted by another jump.
        List<OpcodeBlock> graph = makeGraph(ops, label_indices);
        // Perform some cleanup on the graph like removing blocks containing
        // only labels and splitting the opcodes associated with a jump away
        // from opcodes for statements preceeding the control flow statement.
        cleanupGraph(graph);

        // TODO: remove debug
        for (OpcodeBlock b : graph) {
            b.print();
        }

        // Performs a sequence of transformations to conver the graph into a
        // simple array of partially decompiled block sections.
        List<BlockSection> flat_graph = flattenGraph(graph, locals);

        // Append all block sections to the output in order. This finalizes all
        // decompilation of statements not already decompiled.
        for (BlockSection op : flat_graph) {
            appendBlock(op, block);
        }

        return block;
    }

    @SuppressWarnings("unchecked")
    private static List<OpcodeBlock> makeGraph(List<AbstractInsnNode> instructions, Map<Label, Integer> label_indices) {
        Set<Integer> break_points = new HashSet<>();

        // Loop through and mark all jumo and return opcodes as break points
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode next = instructions.get(i);
            if (next instanceof JumpInsnNode) {
                break_points.add(i);
                // also break before labels targetted by jump opcodes to have a
                // break between the body of an if block and the statements
                // after it
                break_points.add(label_indices.get(((JumpInsnNode) next).label.getLabel()));
                continue;
            } else if (next instanceof TableSwitchInsnNode) {
                break_points.add(i);
                TableSwitchInsnNode ts = (TableSwitchInsnNode) next;
                for (LabelNode l : (List<LabelNode>) ts.labels) {
                    break_points.add(label_indices.get(l.getLabel()));
                }
                break_points.add(label_indices.get(ts.dflt.getLabel()));
            } else if (next instanceof LookupSwitchInsnNode) {
                break_points.add(i);
                LookupSwitchInsnNode ts = (LookupSwitchInsnNode) next;
                for (LabelNode l : (List<LabelNode>) ts.labels) {
                    break_points.add(label_indices.get(l.getLabel()));
                }
                break_points.add(label_indices.get(ts.dflt.getLabel()));
            }
            int op = next.getOpcode();
            if (op <= RETURN && op >= IRETURN) {
                break_points.add(i);
            }
        }
        // Sort the break points
        List<Integer> sorted_break_points = new ArrayList<>(break_points);
        sorted_break_points.sort(Comparator.naturalOrder());
        Map<Integer, OpcodeBlock> blocks = new HashMap<>();
        List<OpcodeBlock> block_list = new ArrayList<>();

        int last_brk = 0;
        for (int brk : sorted_break_points) {
            // accumulate the opcodes beween the next breakpoint and the last
            // breakpoint. Also split the last opcode off into a special field
            // it it is a jump. We use this to more easily tell which blocks
            // have a conditional outcome.
            OpcodeBlock block = new OpcodeBlock();
            block_list.add(block);
            block.break_point = brk;
            for (int i = last_brk; i < brk; i++) {
                block.opcodes.add(instructions.get(i));
            }
            AbstractInsnNode last = instructions.get(brk);
            block.last = last;
            blocks.put(brk, block);
            last_brk = brk + 1;
        }

        for (Map.Entry<Integer, OpcodeBlock> e : blocks.entrySet()) {
            // Now we go through and form an edge from any block and the block
            // it flows (or jumps) into next.
            OpcodeBlock block = e.getValue();
            if (block.last instanceof LabelNode) {
                OpcodeBlock next = blocks.get(sorted_break_points.get(sorted_break_points.indexOf(e.getKey()) + 1));
                block.last = null;
                block.target = next;
            } else if (block.last instanceof JumpInsnNode) {
                Label label = ((JumpInsnNode) block.last).label.getLabel();
                block.target = blocks.get(sorted_break_points.get(sorted_break_points.indexOf(label_indices.get(label)) + 1));
                if (block.last.getOpcode() != GOTO) {
                    OpcodeBlock next = blocks.get(sorted_break_points.get(sorted_break_points.indexOf(e.getKey()) + 1));
                    block.else_target = next;
                }
            } else if (block.last instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode ts = (TableSwitchInsnNode) block.last;
                for (LabelNode l : (List<LabelNode>) ts.labels) {
                    Label label = l.getLabel();
                    block.additional_targets.put(label,
                            blocks.get(sorted_break_points.get(sorted_break_points.indexOf(label_indices.get(label)) + 1)));
                }
                Label label = ts.dflt.getLabel();
                block.additional_targets.put(label, blocks.get(sorted_break_points.get(sorted_break_points.indexOf(label_indices.get(label)) + 1)));
            } else if (block.last instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode ts = (LookupSwitchInsnNode) block.last;
                for (LabelNode l : (List<LabelNode>) ts.labels) {
                    Label label = l.getLabel();
                    block.additional_targets.put(label,
                            blocks.get(sorted_break_points.get(sorted_break_points.indexOf(label_indices.get(label)) + 1)));
                }
                Label label = ts.dflt.getLabel();
                block.additional_targets.put(label, blocks.get(sorted_break_points.get(sorted_break_points.indexOf(label_indices.get(label)) + 1)));
            }
        }

        return block_list;
    }

    private static void remap(List<OpcodeBlock> blocks, OpcodeBlock from, OpcodeBlock to) {
        for (OpcodeBlock other : blocks) {
            if (other.target == from) {
                other.target = to;
            }
            if (other.else_target == from) {
                other.else_target = to;
            }
            for (Label key : other.additional_targets.keySet()) {
                OpcodeBlock t = other.additional_targets.get(key);
                if (t == from) {
                    other.additional_targets.put(key, to);
                }
            }
        }
    }

    private static void cleanupGraph(List<OpcodeBlock> blocks) {
        for (Iterator<OpcodeBlock> it = blocks.iterator(); it.hasNext();) {
            OpcodeBlock block = it.next();
            if (AstUtil.isEmptyOfLogic(block.opcodes) && block.last == null) {
                // some conditions can create an empty block as a breakpoint
                // gets inserted on either side of a label immediately following
                // a jump

                for (OpcodeBlock other : blocks) {
                    if (other == block) {
                        continue;
                    }
                    if (other.target == block) {
                        other.target = block.target;
                    }
                    if (other.else_target == block) {
                        other.else_target = block.target;
                    }
                }
                it.remove();
            }
        }

        // Split the opcodes that form the condition away from the preceding
        // statements.
        List<OpcodeBlock> fblocks = new ArrayList<>();
        for (OpcodeBlock block : blocks) {
            if (block.isGoto()) {
                if (AstUtil.isEmptyOfLogic(block.opcodes)) {
                    fblocks.add(block);
                    continue;
                }
                OpcodeBlock header = new OpcodeBlock();
                header.break_point = block.break_point;
                header.opcodes.addAll(block.opcodes);
                block.opcodes.clear();
                // Have to ensure that we remap any blocks that were
                // targeting this block to target the header.
                remap(blocks, block, header);
                remap(fblocks, block, header);
                header.target = block;
                fblocks.add(header);
                fblocks.add(block);
            } else if (block.isConditional() || block.isSwitch()) {
                int cond_start = AstUtil.findStartLastStatement(block.opcodes, block.last);
                if (cond_start > 0) {
                    OpcodeBlock header = new OpcodeBlock();
                    header.break_point = block.break_point;
                    for (int i = 0; i < cond_start; i++) {
                        header.opcodes.add(block.opcodes.get(i));
                    }
                    if (AstUtil.isEmptyOfLogic(header.opcodes)) {
                        // If there are no useful opcodes left in the header
                        // then we do not perform the split.
                        fblocks.add(block);
                        continue;
                    }
                    for (int i = cond_start - 1; i >= 0; i--) {
                        block.opcodes.remove(i);
                    }
                    // Have to ensure that we remap any blocks that were
                    // targeting this block to target the header.
                    remap(blocks, block, header);
                    remap(fblocks, block, header);
                    header.target = block;
                    fblocks.add(header);
                    fblocks.add(block);
                } else {
                    fblocks.add(block);
                }
            } else {
                fblocks.add(block);
            }
        }

        blocks.clear();
        blocks.addAll(fblocks);

        // TODO identify ternary blocks (but no transformations yet)

        // populate a field of blocks targetting a block. Currently unused but
        // might be useful (Its from an older version of this code).
        for (int i = 0; i < blocks.size(); i++) {
            OpcodeBlock block = blocks.get(i);
            if (block.isConditional()) {
                block.else_target.targetted_by.add(block);
            }
            if (block.isJump()) {
                block.target.targetted_by.add(block);
            } else if (block.target != null && blocks.indexOf(block.target) != i + 1) {
                block.target.targetted_by.add(block);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<BlockSection> flattenGraph(List<OpcodeBlock> blocks, Locals locals) {
        List<BlockSection> final_blocks = new ArrayList<>();

        List<OpcodeBlock> region = new ArrayList<>();

        // here we're splitting a graph into regions by points that both
        // dominate and post-dominate the rest of the graph (eg. points that all
        // control points pass through).

        // we can then process these regions and transform them into a single
        // block representing the control flow statement of the region. Each
        // region will always contain exactly one control flow statement (not
        // counting nesting).

        // We iterate the blocks in order to ensure that we always fine the
        // earliest block of any control flow statement which makes sure that we
        // don't miss any parts of the control flow statement.

        for (int i = 0; i < blocks.size(); i++) {
            OpcodeBlock region_start = blocks.get(i);
            if (region_start.isSwitch()) {
                if (region_start.last instanceof TableSwitchInsnNode || region_start.last instanceof LookupSwitchInsnNode) {
                    List<LabelNode> labels = null;
                    List<Integer> keys = null;
                    LabelNode dflt = null;
                    if (region_start.last instanceof TableSwitchInsnNode) {
                        TableSwitchInsnNode ts = (TableSwitchInsnNode) region_start.last;
                        labels = ts.labels;
                        dflt = ts.dflt;
                        keys = new ArrayList<>();
                        for (int k = ts.min; k <= ts.max; k++) {
                            keys.add(k);
                        }
                    } else if (region_start.last instanceof LookupSwitchInsnNode) {
                        LookupSwitchInsnNode ts = (LookupSwitchInsnNode) region_start.last;
                        labels = ts.labels;
                        dflt = ts.dflt;
                        keys = ts.keys;
                    }
                    SwitchBlockSection sswitch = new SwitchBlockSection(region_start);
                    final_blocks.add(sswitch);
                    Map<Label, SwitchCaseBlockSection> cases = new HashMap<>();
                    int index = 0;
                    OpcodeBlock end = null;
                    for (LabelNode l : labels) {
                        SwitchCaseBlockSection cs = cases.get(l.getLabel());
                        if (cs != null) {
                            cs.targets.add(keys.get(index++));
                            continue;
                        }
                        cs = new SwitchCaseBlockSection();
                        sswitch.cases.add(cs);
                        cases.put(l.getLabel(), cs);
                        cs.targets.add(keys.get(index++));
                        List<OpcodeBlock> case_region = new ArrayList<>();
                        OpcodeBlock block = region_start.additional_targets.get(l.getLabel());
                        case_region.add(block);
                        int start = blocks.indexOf(block) + 1;
                        block = blocks.get(start);
                        while (!region_start.additional_targets.containsValue(block) && block != end) {
                            case_region.add(block);
                            start++;
                            block = blocks.get(start);
                        }

                        OpcodeBlock last = case_region.get(case_region.size() - 1);
                        if (last.isGoto()) {
                            end = last.target;
                            case_region.remove(last);
                            cs.breaks = true;
                        }

                        cs.body.addAll(flattenGraph(case_region, locals));
                    }
                    {
                        SwitchCaseBlockSection cs = cases.get(dflt.getLabel());
                        if (cs != null) {
                            cs.isDefault = true;
                            cs.targets.add(index);
                            continue;
                        }
                        cs = new SwitchCaseBlockSection();
                        cases.put(dflt.getLabel(), cs);
                        sswitch.cases.add(cs);
                        List<OpcodeBlock> case_region = new ArrayList<>();
                        OpcodeBlock block = region_start.additional_targets.get(dflt.getLabel());
                        case_region.add(block);
                        int start = blocks.indexOf(block) + 1;
                        block = blocks.get(start);
                        while (!region_start.additional_targets.containsValue(block) && block != end) {
                            case_region.add(block);
                            start++;
                            block = blocks.get(start);
                        }
                        cs.isDefault = true;
                        cs.body.addAll(flattenGraph(case_region, locals));
                    }
                    i = blocks.indexOf(end) - 1;
                }
                continue;
            }
            int end = -1;
            boolean targeted_in_future = false;
            if (!region_start.isJump()) {
                for (OpcodeBlock t : region_start.targetted_by) {
                    int index = blocks.indexOf(t);
                    if (index > i) {
                        targeted_in_future = true;
                        if (index > end) {
                            end = index;
                        }
                    }
                }
                // if the block is targeted by a block farther in the code then
                // this block is the first block in a do_while loop
                if (!targeted_in_future) {
                    // If the next block isn't conditional then we simply append
                    // it
                    // to the output.
                    final_blocks.add(new InlineBlockSection(region_start));
                    continue;
                }
            }
            if (end == -1) {
                end = getRegionEnd(blocks, i);
            } else {
                end++;
            }

            if (end == -1) {
                // Any conditional block should always form the start of a
                // region at this level.
                throw new IllegalStateException("Conditional jump not part of control flow statement??");
            }

            region.clear();
            for (int o = i; o < end; o++) {
                region.add(blocks.get(o));
            }
            // process the region down to a single block
            final_blocks.add(processRegion(region, locals, blocks.get(end), targeted_in_future ? 0 : 1));
            i = end - 1;
        }

        return final_blocks;
    }

    private static BlockSection processRegion(List<OpcodeBlock> region, Locals locals, OpcodeBlock ret, int body_start) {

        // This is a recursive function which processes a region and determines
        // which control flow statement is suitable for it.

        // The first step is to find any points within the region that are sub
        // regions (eg. both dominate and post-domintate the rest of the graph
        // excluding the end point of the current region). These sub-regions are
        // then processes ahead of time into their own control flow statements
        // which are then nested inside of this region.

        for (int i = body_start; i < region.size() - 1; i++) {
            OpcodeBlock next = region.get(i);
            if (!next.isJump()) {
                continue;
            }

            // The end block is already not included in `region` so we can
            // simply try and get the region end of any block in this region and
            // if there is an end defined then we know that it forms a sub
            // region.

            int end = getRegionEnd(region, i);

            if (end != -1 && end < region.size() - 1) {

                List<OpcodeBlock> subregion = new ArrayList<>();
                for (int o = i; o < end; o++) {
                    subregion.add(region.get(o));
                }

                BlockSection s = processRegion(subregion, locals, region.get(end), i + 1);

                // the first block is set to the condensed subregion block and
                // the rest if the blocks in the subregion are removed.
                OpcodeBlock replacement = region.get(i);
                replacement.internal = s;
                replacement.target = region.get(end);
                replacement.else_target = null;
                replacement.opcodes.clear();

                for (int o = 1; o < subregion.size(); o++) {
                    region.remove(subregion.get(o));
                }
            }
        }
        OpcodeBlock start = region.get(0);
        if (body_start == 0) {
            List<OpcodeBlock> condition_blocks = new ArrayList<>();
            int cond_start = region.size() - 1;
            OpcodeBlock next = region.get(cond_start);
            while (next.isConditional()) {
                condition_blocks.add(0, next);
                cond_start--;
                next = region.get(cond_start);
            }
            cond_start++;
            Condition cond = makeCondition(condition_blocks, locals, start, ret);

            DoWhileBlockSection section = new DoWhileBlockSection(cond);

            for (int i = 0; i < cond_start; i++) {
                next = region.get(i);
                if (next.internal != null) {
                    section.body.add(next.internal);
                } else if (next.isConditional()) {
                    // If we encounter another conditional block then its an
                    // error
                    // as we should have already processed all sub regions
                    throw new IllegalStateException("Unexpected conditional when building if body");
                } else {
                    section.body.add(new InlineBlockSection(next));
                }
            }
            return section;
        }
        if (start.isGoto()) {
            List<OpcodeBlock> condition_blocks = new ArrayList<>();
            OpcodeBlock next = start.target;
            int pos = region.indexOf(next);
            int cond_start = pos;
            while (next.isConditional()) {
                condition_blocks.add(next);
                pos++;
                if (pos >= region.size()) {
                    break;
                }
                next = region.get(pos);
            }

            OpcodeBlock body = region.get(1);
            Condition cond = makeCondition(condition_blocks, locals, body, ret);

            WhileBlockSection section = new WhileBlockSection(cond);

            for (int i = 1; i < cond_start; i++) {
                next = region.get(i);
                if (next.internal != null) {
                    section.body.add(next.internal);
                } else if (next.isConditional()) {
                    // If we encounter another conditional block then its an
                    // error
                    // as we should have already processed all sub regions
                    throw new IllegalStateException("Unexpected conditional when building if body");
                } else {
                    section.body.add(new InlineBlockSection(next));
                }
            }

            return section;
        }
        // split the region into the condition and the body
        body_start = 1;
        List<OpcodeBlock> condition_blocks = new ArrayList<>();
        OpcodeBlock next = region.get(body_start);
        condition_blocks.add(start);
        while (next.isConditional()) {
            condition_blocks.add(next);
            body_start++;
            next = region.get(body_start);
        }
        OpcodeBlock body = region.get(body_start);
        OpcodeBlock cond_ret = ret;
        for (OpcodeBlock c : condition_blocks) {
            if (c.target != body && !condition_blocks.contains(c.target)) {
                cond_ret = c.target;
                break;
            }
        }

        // form the condition from the header
        Condition cond = makeCondition(condition_blocks, locals, body, cond_ret);
        IfBlockSection section = new IfBlockSection(cond);
        int else_start = region.size();
        if (cond_ret != ret) {
            else_start = region.indexOf(cond_ret);
        }
        // Append the body
        for (int i = body_start; i < else_start; i++) {
            next = region.get(i);
            if (next.internal != null) {
                section.body.add(next.internal);
            } else if (next.isConditional()) {
                // If we encounter another conditional block then its an
                // error
                // as we should have already processed all sub regions
                throw new IllegalStateException("Unexpected conditional when building if body");
            } else {
                section.body.add(new InlineBlockSection(next));
            }
        }

        while (cond_ret != ret) {
            if (cond_ret.isConditional()) {
                List<OpcodeBlock> elif_condition = new ArrayList<>();
                next = region.get(body_start);
                elif_condition.add(cond_ret);
                body_start = region.indexOf(cond_ret) + 1;
                while (next.isConditional()) {
                    elif_condition.add(next);
                    body_start++;
                    next = region.get(body_start);
                }
                OpcodeBlock elif_body = region.get(body_start);
                cond_ret = ret;
                for (OpcodeBlock c : elif_condition) {
                    if (c.target != elif_body && !elif_condition.contains(c.target)) {
                        cond_ret = c.target;
                        break;
                    }
                }
                Condition elif_cond = makeCondition(elif_condition, locals, elif_body, cond_ret);
                ElifBlockSection elif = new ElifBlockSection(elif_cond);
                section.elif.add(elif);
                int elif_end = region.size();
                if (cond_ret != ret) {
                    elif_end = region.indexOf(cond_ret);
                }
                // Append the body
                for (int i = body_start; i < elif_end; i++) {
                    next = region.get(i);
                    if (next.internal != null) {
                        elif.body.add(next.internal);
                    } else if (next.isConditional()) {
                        // If we encounter another conditional block then
                        // its an
                        // error
                        // as we should have already processed all sub
                        // regions
                        throw new IllegalStateException("Unexpected conditional when building elif body");
                    } else {
                        elif.body.add(new InlineBlockSection(next));
                    }
                }
            } else {
                else_start = region.indexOf(cond_ret);
                for (int i = else_start; i < region.size(); i++) {
                    next = region.get(i);
                    if (next.internal != null) {
                        section.else_.add(next.internal);
                    } else if (next.isConditional()) {
                        // If we encounter another conditional block then
                        // its an
                        // error
                        // as we should have already processed all sub
                        // regions
                        throw new IllegalStateException("Unexpected conditional when building else body");
                    } else {
                        section.else_.add(new InlineBlockSection(next));
                    }
                }
                break;
            }
        }

        return section;
    }

    private static int getRegionEnd(List<OpcodeBlock> blocks, int start) {
        OpcodeBlock region_start = blocks.get(start);
        // if the target is behind the start then we break as this is likely the
        // condition of a do-while

        // the check is less or equal because when a header is split from a
        // condition its break point is set to the same as the condition, in a
        // simple do-while the condition targets the body which was originally
        // split from the condition and therefore has the same break_point so we
        // do this check to catch that. There are no conditions where two
        // conditions may have the same break point that could break this (ie.
        // the case where a condition was targeting a condition after it that
        // has the same break_point is impossible).
        if (region_start.target.break_point <= region_start.break_point) {
            return -1;
        }
        int end_a = blocks.indexOf(region_start.target);
        if (end_a == -1 && region_start.target != null) {
            end_a = blocks.size();
        }
        int end_b = blocks.indexOf(region_start.else_target);
        if (end_b == -1 && region_start.else_target != null) {
            end_b = blocks.size();
        }

        // Use the target of the start node as a starting point for our search
        int end = Math.max(end_a, end_b);
        boolean isGoto = region_start.isGoto();
        return getRegionEnd(blocks, start, end, isGoto);
    }

    private static int getRegionEnd(List<OpcodeBlock> blocks, int start, int end, boolean isGoto) {

        // TODO: break and continue targeting labels on outer loops will break
        // this completely

        // This is a rather brute force search for the next node after the start
        // node which post-dominates the preceding nodes.
        int end_extension = 0;
        int end_a, end_b;

        check: while (true) {
            for (int o = 0; o < start; o++) {
                OpcodeBlock next = blocks.get(o);
                end_a = blocks.indexOf(next.target);
                if (end_a == -1 && next.target != null)
                    end_a = blocks.size();
                end_b = blocks.indexOf(next.else_target);
                if (end_b == -1 && next.else_target != null)
                    end_b = blocks.size();
                if ((end_a > start && end_a < end) || (end_b > start && end_b < end)) {
                    // If any block before the start points into the region then
                    // our start node wasn't actually the start of a subregion.
                    return -1;
                }
            }
            for (int o = start + 1; o < end; o++) {
                OpcodeBlock next = blocks.get(o);
                end_a = blocks.indexOf(next.target);
                if (end_a == -1 && next.target != null)
                    end_a = blocks.size();
                end_b = blocks.indexOf(next.else_target);
                if (end_b == -1 && next.else_target != null)
                    end_b = blocks.size();

                int new_end = Math.max(end_a, end_b);

                if (new_end > end) {
                    // We've found a block inside the current region that points
                    // to a block past the current end of the region. Resize the
                    // region to include it and restart the search.
                    end = new_end;
                    continue check;
                }
            }
            if (isGoto) {
                OpcodeBlock next = blocks.get(end);
                int pos_ext = end_extension;
                while (next.isConditional()) {
                    end_a = blocks.indexOf(next.target);
                    end_b = blocks.indexOf(next.else_target);
                    if ((end_a > start && end_a < end) || (end_b > start && end_b < end)) {
                        end_extension = ++pos_ext;
                        next = blocks.get(end + end_extension);
                        continue;
                    }
                    pos_ext++;
                    next = blocks.get(end + pos_ext);
                }
            }
            for (int o = end + end_extension; o < blocks.size(); o++) {
                OpcodeBlock next = blocks.get(o);
                end_a = blocks.indexOf(next.target);
                if (end_a == -1 && next.target != null)
                    end_a = blocks.size();
                end_b = blocks.indexOf(next.else_target);
                if (end_b == -1 && next.else_target != null)
                    end_b = blocks.size();
                if ((end_a > start && end_a < end) || (end_b > start && end_b < end)) {
                    return -1;
                }
            }
            break;
        }
        if (end >= blocks.size()) {
            return -1;
        }
        return end + end_extension;
    }

    private static Condition makeSimpleCondition(OpcodeBlock block, Locals locals) {

        // This forms the condition representing the conditional jump of the
        // given block

        StatementBlock dummy = new StatementBlock(StatementBlock.Type.IF, locals);
        Deque<Instruction> dummy_stack = new ArrayDeque<>();
        appendBlock(block, dummy, locals, dummy_stack);

        // TOOD support remaining conditional jump opcodes
        switch (block.last.getOpcode()) {
        case IFEQ: {
            if (dummy_stack.size() != 1 || !dummy.getStatements().isEmpty()) {
                throw new IllegalStateException();
            }
            Instruction val = dummy_stack.pop();
            return new BooleanCondition(val, true);
        }
        case IFNE: {
            if (dummy_stack.size() != 1 || !dummy.getStatements().isEmpty()) {
                throw new IllegalStateException();
            }
            Instruction val = dummy_stack.pop();
            return new BooleanCondition(val, false);
        }
        case IF_ICMPLT: {
            if (dummy_stack.size() != 2 || !dummy.getStatements().isEmpty()) {
                throw new IllegalStateException();
            }
            Instruction b = dummy_stack.pop();
            Instruction a = dummy_stack.pop();
            return new CompareCondition(a, b, CompareCondition.fromOpcode(block.last.getOpcode()));
        }
        case IFNULL: {
            if (dummy_stack.size() != 1 || !dummy.getStatements().isEmpty()) {
                throw new IllegalStateException();
            }
            Instruction val = dummy_stack.pop();
            return new CompareCondition(val, new NullConstantArg(), CompareCondition.CompareOperator.EQUAL);
        }
        case IFNONNULL: {
            if (dummy_stack.size() != 1 || !dummy.getStatements().isEmpty()) {
                throw new IllegalStateException();
            }
            Instruction val = dummy_stack.pop();
            return new CompareCondition(val, new NullConstantArg(), CompareCondition.CompareOperator.NOT_EQUAL);
        }
        default:
            throw new IllegalStateException("Unsupported conditional jump opcode " + block.last.getOpcode());
        }
    }

    private static void dfs(ConditionGraphNode next, Deque<Condition> stack) {

        // performs a depth-first-search to populate each node in the graph's
        // partial conditions

        if (!stack.isEmpty()) {
            // Add the condition up to this point to the partial conditions of
            // this node. This represents a path from the root to this node and
            // the condition of that path is the and of all simple conditions of
            // the nodes along the path
            if (stack.size() == 1) {
                next.partial_conditions.add(stack.peek());
            } else {
                Condition partial = new AndCondition(stack);
                next.partial_conditions.add(partial);
            }
        }
        if (next.condition == null) {
            return;
        }
        // Push the simple condition of this node to the stack and recurse into
        // the target branch
        stack.addLast(next.condition);
        dfs(next.target, stack);
        stack.pollLast();
        // Same thing for the else_target except we push the inverse of this
        // node's condition
        stack.addLast(inverse(next.condition));
        dfs(next.else_target, stack);
        stack.pollLast();
    }

    private static Condition makeCondition(List<OpcodeBlock> blocks, Locals locals, OpcodeBlock body, OpcodeBlock ret) {
        List<ConditionGraphNode> nodes = new ArrayList<>(blocks.size());

        ConditionGraphNode body_node = new ConditionGraphNode(null, null, null, body);
        ConditionGraphNode ret_node = new ConditionGraphNode(null, null, null, ret);

        // Forms a condition from a set of conditional jumps. This is done by
        // performing a depth first search of the nodes which form the
        // condition. Each path through the graph from root to the start of the
        // body is found and combined with OR to form a very much expanded
        // version of the condition for this block. This is then simplified
        // before being returned.

        for (int i = 0; i < blocks.size(); i++) {
            OpcodeBlock next = blocks.get(i);
            // make the nodes and compute the simple condition for each node.
            nodes.add(new ConditionGraphNode(makeSimpleCondition(next, locals), null, null, next));
        }

        for (int i = 0; i < blocks.size(); i++) {
            OpcodeBlock next = blocks.get(i);
            ConditionGraphNode node = nodes.get(i);
            // connect the nodes
            if (next.target == body) {
                node.target = body_node;
            } else if (next.target == ret) {
                node.target = ret_node;
            } else {
                int target = blocks.indexOf(next.target);
                node.target = nodes.get(target);
            }
            if (next.else_target == body) {
                node.else_target = body_node;
            } else if (next.else_target == ret) {
                node.else_target = ret_node;
            } else {
                int target = blocks.indexOf(next.else_target);
                node.else_target = nodes.get(target);
            }
        }

        ConditionGraphNode start = nodes.get(0);

        // perform the dfs
        Deque<Condition> stack = new ArrayDeque<>();
        dfs(start, stack);

        OrCondition condition = new OrCondition(body_node.partial_conditions);
        return ConditionUtil.simplifyCondition(condition);
    }

    private static void appendBlock(BlockSection block_section, StatementBlock block) {
        Deque<Instruction> stack = Queues.newArrayDeque();

        // Appends a block to the output, constructing the needed control flow
        // statements and decompiling any opcodes that are not yet formed into
        // statements.

        if (block_section instanceof IfBlockSection) {
            IfBlockSection ifblock = (IfBlockSection) block_section;
            StatementBlock body = new StatementBlock(StatementBlock.Type.IF, block.getLocals());
            for (BlockSection body_section : ifblock.body) {
                appendBlock(body_section, body);
            }
            If iff = new If(ifblock.condition, body);
            for (ElifBlockSection elif : ifblock.elif) {
                StatementBlock elif_body = new StatementBlock(StatementBlock.Type.IF, block.getLocals());
                for (BlockSection body_section : elif.body) {
                    appendBlock(body_section, elif_body);
                }
                iff.new Elif(elif.condition, elif_body);
            }
            if (!ifblock.else_.isEmpty()) {
                StatementBlock else_body = new StatementBlock(StatementBlock.Type.IF, block.getLocals());
                for (BlockSection body_section : ifblock.else_) {
                    appendBlock(body_section, else_body);
                }
                iff.new Else(else_body);
            }
            block.append(iff);
        } else if (block_section instanceof InlineBlockSection) {
            OpcodeBlock op = ((InlineBlockSection) block_section).block;
            appendBlock(op, block, block.getLocals(), stack);
        } else if (block_section instanceof WhileBlockSection) {
            WhileBlockSection whileblock = (WhileBlockSection) block_section;
            StatementBlock body = new StatementBlock(StatementBlock.Type.WHILE, block.getLocals());
            for (BlockSection body_section : whileblock.body) {
                appendBlock(body_section, body);
            }
            if (!block.getStatements().isEmpty()) {
                Statement last = block.getStatements().get(block.getStatements().size() - 1);
                if (last instanceof LocalAssignment) {
                    LocalAssignment assign = (LocalAssignment) last;
                    LocalInstance local = assign.getLocal();
                    if (AstUtil.references(whileblock.condition, local)) {
                        Increment increment = null;
                        if (!body.getStatements().isEmpty()) {
                            Statement body_last = body.getStatements().get(body.getStatements().size() - 1);
                            if (body_last instanceof Increment && ((Increment) body_last).getLocal() == local) {
                                increment = (Increment) body_last;
                            }
                        }
                        block.getStatements().remove(last);
                        if (increment != null) {
                            body.getStatements().remove(increment);
                        }
                        For ffor = new For(last, whileblock.condition, increment, body);
                        block.append(ffor);
                        return;
                    }
                }
            }
            While wwhile = new While(whileblock.condition, body);
            block.append(wwhile);
        } else if (block_section instanceof DoWhileBlockSection) {
            DoWhileBlockSection dowhileblock = (DoWhileBlockSection) block_section;
            StatementBlock body = new StatementBlock(StatementBlock.Type.WHILE, block.getLocals());
            for (BlockSection body_section : dowhileblock.body) {
                appendBlock(body_section, body);
            }
            DoWhile dowhile = new DoWhile(dowhileblock.condition, body);
            block.append(dowhile);
        } else if (block_section instanceof SwitchBlockSection) {
            SwitchBlockSection ts = (SwitchBlockSection) block_section;
            appendBlock(ts.switchblock, block, block.getLocals(), stack);
            Switch sswitch = new Switch(stack.pop());
            for (SwitchCaseBlockSection cs : ts.cases) {
                StatementBlock body = new StatementBlock(StatementBlock.Type.SWITCH, block.getLocals());
                for (BlockSection body_section : cs.body) {
                    appendBlock(body_section, body);
                }
                sswitch.new Case(body, cs.breaks, cs.isDefault, cs.targets);
            }
            block.append(sswitch);
        } else {
            throw new IllegalStateException("Unknown block section " + block_section);
        }
    }

    private static void appendBlock(OpcodeBlock op, StatementBlock block, Locals locals, Deque<Instruction> stack) {

        // Decompiles a set of opcodes into statements.

        for (int index = 0; index < op.opcodes.size() + 1; index++) {
            int label_index = op.break_point - (op.opcodes.size() - index);
            AbstractInsnNode next;
            if (index < op.opcodes.size()) {
                next = op.opcodes.get(index);
            } else if (op.isReturn() && op.last != null) {
                next = op.last;
            } else {
                break;
            }
            if (next instanceof LabelNode) {
                continue;
            } else if (next instanceof FrameNode) {
                continue;
            } else if (next instanceof LineNumberNode) {
                continue;
            }

            switch (next.getOpcode()) {
            case NOP:
                break;
            case ACONST_NULL:
                stack.push(new NullConstantArg());
                break;
            case ICONST_M1:
                stack.push(new IntConstantArg(-1));
                break;
            case ICONST_0:
                stack.push(new IntConstantArg(0));
                break;
            case ICONST_1:
                stack.push(new IntConstantArg(1));
                break;
            case ICONST_2:
                stack.push(new IntConstantArg(2));
                break;
            case ICONST_3:
                stack.push(new IntConstantArg(3));
                break;
            case ICONST_4:
                stack.push(new IntConstantArg(4));
                break;
            case ICONST_5:
                stack.push(new IntConstantArg(5));
                break;
            case LCONST_0:
                stack.push(new LongConstantArg(0));
                break;
            case LCONST_1:
                stack.push(new LongConstantArg(1));
                break;
            case FCONST_0:
                stack.push(new FloatConstantArg(0));
                break;
            case FCONST_1:
                stack.push(new FloatConstantArg(1));
                break;
            case FCONST_2:
                stack.push(new FloatConstantArg(2));
                break;
            case DCONST_0:
                stack.push(new DoubleConstantArg(0));
                break;
            case DCONST_1:
                stack.push(new DoubleConstantArg(1));
                break;
            case BIPUSH:
            case SIPUSH: {
                IntInsnNode insn = (IntInsnNode) next;
                stack.push(new IntConstantArg(insn.operand));
                break;
            }
            case LDC: {
                LdcInsnNode ldc = (LdcInsnNode) next;
                if (ldc.cst instanceof String) {
                    stack.push(new StringConstantArg((String) ldc.cst));
                } else if (ldc.cst instanceof Integer) {
                    stack.push(new IntConstantArg((Integer) ldc.cst));
                } else if (ldc.cst instanceof Float) {
                    stack.push(new FloatConstantArg((Float) ldc.cst));
                } else if (ldc.cst instanceof Long) {
                    // LDC_W appears to be merged with this opcode by asm so
                    // long
                    // and double constants will also be here
                    stack.push(new LongConstantArg((Long) ldc.cst));
                } else if (ldc.cst instanceof Double) {
                    stack.push(new DoubleConstantArg((Double) ldc.cst));
                } else if (ldc.cst instanceof Type) {
                    stack.push(new TypeConstantArg((Type) ldc.cst));
                } else {
                    throw new IllegalStateException("Unsupported ldc constant: " + ldc.cst);
                }
                break;
            }
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD: {
                VarInsnNode var = (VarInsnNode) next;
                Local local = locals.getLocal(var.var);
                stack.push(new LocalArg(local.getInstance(label_index)));
                break;
            }
            case IALOAD:
            case LALOAD:
            case FALOAD:
            case DALOAD:
            case AALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD: {
                Instruction index_arg = stack.pop();
                Instruction var = stack.pop();
                stack.push(new ArrayLoadArg(var, index_arg));
                break;
            }
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE: {
                VarInsnNode var = (VarInsnNode) next;
                Instruction val = stack.pop();
                Local local = locals.getLocal(var.var);
                block.append(new LocalAssignment(local.getInstance(label_index), val));
                break;
            }
            case IASTORE:
            case LASTORE:
            case FASTORE:
            case DASTORE:
            case AASTORE:
            case BASTORE:
            case CASTORE:
            case SASTORE: {
                Instruction val = stack.pop();
                Instruction index_arg = stack.pop();
                Instruction var = stack.pop();
                if (var instanceof NewArrayArg) {
                    NewArrayArg array = (NewArrayArg) var;
                    if (array.getInitializer() == null) {
                        array.setInitialValues(new Instruction[((IntConstantArg) array.getSize()).getConstant()]);
                    }
                    array.getInitializer()[((IntConstantArg) index_arg).getConstant()] = val;
                    break;
                }
                block.append(new ArrayAssignment(var, index_arg, val));
                break;
            }
            case POP: {
                Instruction arg = stack.pop();
                if (arg instanceof InstanceMethodInvoke || arg instanceof StaticMethodInvoke) {
                    block.append(new InvokeStatement(arg));
                }
                break;
            }
            case POP2: {
                for (int i = 0; i < 2; i++) {
                    Instruction arg = stack.pop();
                    if (arg instanceof InstanceMethodInvoke || arg instanceof StaticMethodInvoke) {
                        block.append(new InvokeStatement(arg));
                    }
                }
                break;
            }
            case DUP: {
                stack.push(stack.peek());
                break;
            }
            case DUP_X1: {
                Instruction val = stack.pop();
                Instruction val2 = stack.pop();
                stack.push(val);
                stack.push(val2);
                stack.push(val);
                break;
            }
            case DUP_X2: {
                Instruction val = stack.pop();
                Instruction val2 = stack.pop();
                Instruction val3 = stack.pop();
                stack.push(val);
                stack.push(val3);
                stack.push(val2);
                stack.push(val);
                break;
            }
            case DUP2: {
                Instruction val = stack.pop();
                Instruction val2 = stack.peek();
                stack.push(val);
                stack.push(val2);
                stack.push(val);
                break;
            }
            case DUP2_X1: {
                Instruction val = stack.pop();
                Instruction val2 = stack.pop();
                Instruction val3 = stack.pop();
                stack.push(val2);
                stack.push(val);
                stack.push(val3);
                stack.push(val2);
                stack.push(val);
                break;
            }
            case DUP2_X2: {
                Instruction val = stack.pop();
                Instruction val2 = stack.pop();
                Instruction val3 = stack.pop();
                Instruction val4 = stack.pop();
                stack.push(val2);
                stack.push(val);
                stack.push(val4);
                stack.push(val3);
                stack.push(val2);
                stack.push(val);
                break;
            }
            case SWAP: {
                Instruction val = stack.pop();
                Instruction val2 = stack.pop();
                stack.push(val);
                stack.push(val2);
                break;
            }
            case IADD:
            case LADD:
            case FADD:
            case DADD: {
                Instruction right = stack.pop();
                Instruction left = stack.pop();
                stack.push(new AddArg(left, right));
                break;
            }
            case ISUB:
            case LSUB:
            case FSUB:
            case DSUB: {
                Instruction right = stack.pop();
                Instruction left = stack.pop();
                stack.push(new SubtractArg(left, right));
                break;
            }
            case IMUL:
            case LMUL:
            case FMUL:
            case DMUL: {
                Instruction right = stack.pop();
                Instruction left = stack.pop();
                stack.push(new MultiplyArg(left, right));
                break;
            }
            case IDIV:
            case LDIV:
            case FDIV:
            case DDIV: {
                Instruction right = stack.pop();
                Instruction left = stack.pop();
                stack.push(new DivideArg(left, right));
                break;
            }
            case IREM:
            case LREM:
            case FREM:
            case DREM: {
                Instruction right = stack.pop();
                Instruction left = stack.pop();
                stack.push(new RemainderInstruction(left, right));
                break;
            }
            case INEG:
            case LNEG:
            case FNEG:
            case DNEG: {
                Instruction right = stack.pop();
                stack.push(new NegArg(right));
                break;
            }
            case ISHL:
            case LSHL: {
                Instruction right = stack.pop();
                Instruction left = stack.pop();
                stack.push(new ShiftLeftArg(left, right));
                break;
            }
            case ISHR:
            case LSHR: {
                Instruction right = stack.pop();
                Instruction left = stack.pop();
                stack.push(new ShiftRightArg(left, right));
                break;
            }
            case IUSHR:
            case LUSHR: {
                Instruction right = stack.pop();
                Instruction left = stack.pop();
                stack.push(new UnsignedShiftRightArg(left, right));
                break;
            }
            case IAND:
            case LAND: {
                Instruction right = stack.pop();
                Instruction left = stack.pop();
                stack.push(new AndInstruction(left, right));
                break;
            }
            case IOR:
            case LOR: {
                Instruction right = stack.pop();
                Instruction left = stack.pop();
                stack.push(new OrArg(left, right));
                break;
            }
            case IXOR:
            case LXOR: {
                Instruction right = stack.pop();
                Instruction left = stack.pop();
                stack.push(new XorArg(left, right));
                break;
            }
            case IINC: {
                IincInsnNode inc = (IincInsnNode) next;
                Local local = locals.getLocal(inc.var);
                Increment insn = new Increment(local.getInstance(label_index), inc.incr);
                block.append(insn);
                break;
            }
            case L2I:
            case F2I:
            case D2I:
                stack.push(new CastArg("I", stack.pop()));
                break;
            case I2L:
            case F2L:
            case D2L:
                stack.push(new CastArg("J", stack.pop()));
                break;
            case I2F:
            case L2F:
            case D2F:
                stack.push(new CastArg("F", stack.pop()));
                break;
            case I2D:
            case F2D:
            case L2D:
                stack.push(new CastArg("D", stack.pop()));
                break;
            case I2B:
                stack.push(new CastArg("B", stack.pop()));
                break;
            case I2C:
                stack.push(new CastArg("C", stack.pop()));
                break;
            case I2S:
                stack.push(new CastArg("S", stack.pop()));
                break;
            case LCMP:
            case FCMPL:
            case FCMPG:
            case DCMPL:
            case DCMPG: {
                Instruction right = stack.pop();
                Instruction left = stack.pop();
                stack.push(new CompareArg(left, right));
                break;
            }
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE:
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case GOTO:
            case JSR:
            case RET:
            case IFNULL:
            case IFNONNULL:
                // All jumps are handled by the implicit structure of the
                // graph
                break;
            case IRETURN:
            case LRETURN:
            case FRETURN:
            case DRETURN:
            case ARETURN:
                block.append(new Return(stack.pop()));
                break;
            case RETURN:
                block.append(new Return());
                break;
            case GETSTATIC: {
                FieldInsnNode field = (FieldInsnNode) next;
                String owner = field.owner;
                if (!owner.startsWith("[")) {
                    owner = "L" + owner + ";";
                }
                FieldArg arg = new StaticFieldArg(field.name, field.desc, owner);
                stack.push(arg);
                break;
            }
            case PUTSTATIC: {
                FieldInsnNode field = (FieldInsnNode) next;
                Instruction val = stack.pop();
                String owner = field.owner;
                if (!owner.startsWith("[")) {
                    owner = "L" + owner + ";";
                }
                FieldAssignment assign = new StaticFieldAssignment(field.name, field.desc, owner, val);
                block.append(assign);
                break;
            }
            case GETFIELD: {
                FieldInsnNode field = (FieldInsnNode) next;
                String owner = field.owner;
                if (!owner.startsWith("[")) {
                    owner = "L" + owner + ";";
                }
                FieldArg arg = new InstanceFieldArg(field.name, field.desc, owner, stack.pop());
                stack.push(arg);
                break;
            }
            case PUTFIELD: {
                FieldInsnNode field = (FieldInsnNode) next;
                Instruction val = stack.pop();
                Instruction owner = stack.pop();
                String owner_t = field.owner;
                if (!owner_t.startsWith("[")) {
                    owner_t = "L" + owner_t + ";";
                }
                FieldAssignment assign = new InstanceFieldAssignment(field.name, field.desc, owner_t, owner, val);
                block.append(assign);
                break;
            }
            case INVOKESPECIAL: {
                MethodInsnNode ctor = (MethodInsnNode) next;
                if (ctor.name.equals("<init>")) {
                    Instruction[] args = new Instruction[TypeHelper.paramCount(ctor.desc)];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = stack.pop();
                    }
                    New new_arg = (New) stack.pop();
                    if (stack.peek() instanceof New) {
                        New new_arg2 = (New) stack.pop();
                        if (new_arg2 == new_arg) {
                            new_arg.setCtorDescription(ctor.desc);
                            new_arg.setParameters(args);
                            stack.push(new_arg);
                            break;
                        }
                        stack.push(new_arg2);
                    }
                    New insn = new New(new_arg.getType(), ctor.desc, args);
                    block.append(new InvokeStatement(insn));
                    break;
                }
            }
            case INVOKEVIRTUAL:
            case INVOKEINTERFACE: {
                MethodInsnNode method = (MethodInsnNode) next;
                String ret = TypeHelper.getRet(method.desc);
                Instruction[] args = new Instruction[TypeHelper.paramCount(method.desc)];
                for (int i = args.length - 1; i >= 0; i--) {
                    args[i] = stack.pop();
                }
                Instruction callee = stack.pop();
                String owner = method.owner;
                if (!owner.startsWith("[")) {
                    owner = "L" + owner + ";";
                }
                InstanceMethodInvoke arg = new InstanceMethodInvoke(method.name, method.desc, owner, args, callee);
                if (ret.equals("V")) {
                    block.append(new InvokeStatement(arg));
                } else {
                    stack.push(arg);
                }
                break;
            }
            case INVOKESTATIC: {
                MethodInsnNode method = (MethodInsnNode) next;
                String ret = TypeHelper.getRet(method.desc);
                Instruction[] args = new Instruction[TypeHelper.paramCount(method.desc)];
                for (int i = args.length - 1; i >= 0; i--) {
                    args[i] = stack.pop();
                }
                String owner = method.owner;
                if (!owner.startsWith("[")) {
                    owner = "L" + owner + ";";
                }
                StaticMethodInvoke arg = new StaticMethodInvoke(method.name, method.desc, owner, args);
                if (ret.equals("V")) {
                    block.append(new InvokeStatement(arg));
                } else {
                    stack.push(arg);
                }
                break;
            }
            case INVOKEDYNAMIC:
                // TODO
                break;
            case NEW: {
                String type = ((TypeInsnNode) next).desc;
                stack.push(new New("L" + type + ";", null, null));
                break;
            }
            case NEWARRAY:
            case ANEWARRAY: {
                Instruction size = stack.pop();
                String array_type = null;
                if (next instanceof IntInsnNode) {
                    IntInsnNode array = (IntInsnNode) next;
                    array_type = AstUtil.opcodeToType(array.operand);
                } else if (next instanceof TypeInsnNode) {
                    TypeInsnNode array = (TypeInsnNode) next;
                    array_type = array.desc;
                }
                stack.push(new NewArrayArg(array_type, size, null));
                break;
            }
            case ARRAYLENGTH:
                stack.push(new InstanceFieldArg("length", "I", "hidden-array-field", stack.pop()));
                break;
            case ATHROW:
                block.append(new Throw(stack.pop()));
                break;
            case CHECKCAST: {
                TypeInsnNode cast = (TypeInsnNode) next;
                String desc = cast.desc;
                if (!desc.startsWith("[")) {
                    desc = "L" + desc + ";";
                }
                stack.push(new CastArg(desc, stack.pop()));
                break;
            }
            case INSTANCEOF: {
                TypeInsnNode insn = (TypeInsnNode) next;
                Instruction val = stack.pop();
                String type = insn.desc;
                if (!type.startsWith("[")) {
                    type = "L" + insn.desc + ";";
                }
                stack.push(new InstanceOfArg(val, type));
                break;
            }
            case MONITORENTER:
            case MONITOREXIT:
                // TODO synchronized
                throw new IllegalStateException();
                // break;
            case MULTIANEWARRAY:
                // TODO
                throw new IllegalStateException();
            default:
                System.err.println("Unsupported opcode: " + next.getOpcode());
                throw new IllegalStateException();
            }
        }
    }

    private static class OpcodeBlock {

        // TODO split this up into a few types for jumps, switches, decompiled,
        // and normal blocks
        public int break_point;
        public final List<AbstractInsnNode> opcodes = new ArrayList<>();
        public AbstractInsnNode last;
        public OpcodeBlock target;
        public OpcodeBlock else_target;
        public final Map<Label, OpcodeBlock> additional_targets = new HashMap<>();

        public Set<OpcodeBlock> targetted_by = new HashSet<>();
        public BlockSection internal = null;

        public OpcodeBlock() {

        }

        public boolean isGoto() {
            return this.last != null && this.last.getOpcode() == GOTO;
        }

        public boolean isJump() {
            return this.last != null && this.last instanceof JumpInsnNode;
        }

        public boolean isReturn() {
            return this.last != null && this.last.getOpcode() >= IRETURN && this.last.getOpcode() <= RETURN;
        }

        public boolean isConditional() {
            return this.else_target != null;
        }

        public boolean isSwitch() {
            return this.last != null && (this.last.getOpcode() == TABLESWITCH || this.last.getOpcode() == LOOKUPSWITCH);
        }

        public void print() {
            System.out.println("Block " + this.break_point + ":");
            for (AbstractInsnNode op : this.opcodes) {
                System.out.println(AstUtil.insnToString(op));
            }
            System.out.println("Last: " + (this.last != null ? AstUtil.insnToString(this.last) : "null"));
            System.out.println("Target: " + (this.target != null ? this.target.break_point : -1));
            System.out.println("Else Target: " + (this.else_target != null ? this.else_target.break_point : -1));
        }

    }

    public static class ConditionGraphNode {

        public Condition condition;
        public ConditionGraphNode target;
        public ConditionGraphNode else_target;
        public OpcodeBlock source;

        public List<Condition> partial_conditions = new ArrayList<>();

        public ConditionGraphNode(Condition c, ConditionGraphNode t, ConditionGraphNode e, OpcodeBlock s) {
            this.condition = c;
            this.target = t;
            this.else_target = e;
            this.source = s;
        }
    }

    private static abstract class BlockSection {

        public BlockSection() {
        }

    }

    private static class InlineBlockSection extends BlockSection {

        public OpcodeBlock block;

        public InlineBlockSection(OpcodeBlock block) {
            this.block = block;
        }
    }

    private static class IfBlockSection extends BlockSection {

        public Condition condition;
        public final List<BlockSection> body = new ArrayList<>();
        public List<BlockSection> else_ = new ArrayList<>();
        public List<ElifBlockSection> elif = new ArrayList<>();

        public IfBlockSection(Condition cond) {
            this.condition = cond;
        }
    }

    private static class ElifBlockSection {

        public Condition condition;
        public final List<BlockSection> body = new ArrayList<>();

        public ElifBlockSection(Condition cond) {
            this.condition = cond;
        }

    }

    private static class WhileBlockSection extends BlockSection {

        public Condition condition;
        public final List<BlockSection> body = new ArrayList<>();

        public WhileBlockSection(Condition cond) {
            this.condition = cond;
        }
    }

    private static class DoWhileBlockSection extends BlockSection {

        public Condition condition;
        public final List<BlockSection> body = new ArrayList<>();

        public DoWhileBlockSection(Condition cond) {
            this.condition = cond;
        }
    }

    private static class SwitchBlockSection extends BlockSection {

        public OpcodeBlock switchblock;
        public final List<SwitchCaseBlockSection> cases = new ArrayList<>();

        public SwitchBlockSection(OpcodeBlock s) {
            this.switchblock = s;
        }
    }

    private static class SwitchCaseBlockSection {

        public final List<BlockSection> body = new ArrayList<>();
        public final List<Integer> targets = new ArrayList<>();
        public boolean breaks;
        public boolean isDefault;

        public SwitchCaseBlockSection() {

        }

    }

}
