package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;

/**
 * BinaryTrees benchmark variant #3 from the Computer Language Benchmarks Game.
 * Tests GC performance by allocating and deallocating binary trees.
 * 
 * Source: benchmarksgame/binarytrees/binarytrees.java-3.java
 * Contributed by Jarkko Miettinen, modified by Daryl Griffith
 */
public class BinaryTreesBenchmark_V3 implements BenchmarkAlgorithm {
    
    @Override
    public String getName() {
        return "binarytrees_v3";
    }
    
    @Override
    public String getDescription() {
        return "Binary trees allocation/deallocation (variant 3)";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        // Binary trees: depth 16
        return 16;
    }
    
    @Override
    public Object execute(int maxDepth) throws Exception {
        int stretchDepth = maxDepth + 1;
        checkTree(createTree(stretchDepth));
        
        TreeNode longLastingNode = createTree(maxDepth);
        int depth = 4;
        int totalCheck = 0;

        do {
            int iterations = 16 << (maxDepth - depth);
            totalCheck += loops(iterations, depth);
            depth += 2;
        } while (depth <= maxDepth);
        
        totalCheck += checkTree(longLastingNode);
        return totalCheck;
    }
    
    private int loops(int iterations, int depth) {
        int check = 0;
        int item = 0;

        do {
            check += checkTree(createTree(depth));
            item++;
        } while (item < iterations);
        
        return check;
    }
    
    private TreeNode createTree(int depth) {
        TreeNode node = new TreeNode();

        if (depth > 0) {
            depth--;
            node.left = createTree(depth);
            node.right = createTree(depth);
        }
        return node;
    }
    
    private int checkTree(TreeNode node) {
        if (node.left == null) {
            return 1;
        }
        return checkTree(node.left) + checkTree(node.right) + 1;
    }

    private static class TreeNode {
        private TreeNode left, right;
    }
}

