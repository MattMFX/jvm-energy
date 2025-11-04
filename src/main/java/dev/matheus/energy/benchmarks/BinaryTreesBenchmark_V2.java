package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;

/**
 * BinaryTrees benchmark variant #2 from the Computer Language Benchmarks Game.
 * Tests GC performance by allocating and deallocating binary trees.
 * 
 * Source: benchmarksgame/binarytrees/binarytrees.java-2.java
 * Contributed by Jarkko Miettinen
 */
public class BinaryTreesBenchmark_V2 implements BenchmarkAlgorithm {
    
    private static final int MIN_DEPTH = 4;
    
    @Override
    public String getName() {
        return "binarytrees_v2";
    }
    
    @Override
    public String getDescription() {
        return "Binary trees allocation/deallocation (variant 2)";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        // Binary trees: depth 16
        return 16;
    }
    
    @Override
    public Object execute(int maxDepth) throws Exception {
        // maxDepth is now the actual tree depth (10-21)
        int stretchDepth = maxDepth + 1;
        
        int check = (TreeNode.bottomUpTree(stretchDepth)).itemCheck();
        
        TreeNode longLivedTree = TreeNode.bottomUpTree(maxDepth);
        
        int totalCheck = 0;
        for (int depth = MIN_DEPTH; depth <= maxDepth; depth += 2) {
            int iterations = 1 << (maxDepth - depth + MIN_DEPTH);
            check = 0;
            
            for (int i = 1; i <= iterations; i++) {
                check += (TreeNode.bottomUpTree(depth)).itemCheck();
            }
            totalCheck += check;
        }
        
        totalCheck += longLivedTree.itemCheck();
        return totalCheck;
    }
    
    private static class TreeNode {
        private TreeNode left, right;
        
        private static TreeNode bottomUpTree(int depth) {
            if (depth > 0) {
                return new TreeNode(
                    bottomUpTree(depth - 1),
                    bottomUpTree(depth - 1)
                );
            } else {
                return new TreeNode(null, null);
            }
        }
        
        TreeNode(TreeNode left, TreeNode right) {
            this.left = left;
            this.right = right;
        }
        
        private int itemCheck() {
            if (left == null) return 1;
            else return 1 + left.itemCheck() + right.itemCheck();
        }
    }
}

