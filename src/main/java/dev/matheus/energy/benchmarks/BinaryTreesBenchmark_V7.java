package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * BinaryTrees benchmark variant #7 from the Computer Language Benchmarks Game.
 * Tests GC performance by allocating and deallocating binary trees with parallel execution.
 * 
 * Source: benchmarksgame/binarytrees/binarytrees.java-7.java
 * Based on Jarkko Miettinen's Java program, contributed by Tristan Dupont
 */
public class BinaryTreesBenchmark_V7 implements BenchmarkAlgorithm {
    
    private static final int MIN_DEPTH = 4;
    
    @Override
    public String getName() {
        return "binarytrees_v7";
    }
    
    @Override
    public String getDescription() {
        return "Binary trees allocation/deallocation (parallel variant 7)";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        // Binary trees: depth 16
        return 16;
    }
    
    @Override
    public Object execute(int n) throws Exception {
        final int maxDepth = n < (MIN_DEPTH + 2) ? MIN_DEPTH + 2 : n;
        final int stretchDepth = maxDepth + 1;

        int stretchCheck = bottomUpTree(stretchDepth).itemCheck();

        final TreeNode longLivedTree = bottomUpTree(maxDepth);

        final String[] results = new String[(maxDepth - MIN_DEPTH) / 2 + 1];
        final ExecutorService executorService = 
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int d = MIN_DEPTH; d <= maxDepth; d += 2) {
            final int depth = d;
            executorService.execute(() -> {
                int check = 0;

                final int iterations = 1 << (maxDepth - depth + MIN_DEPTH);
                for (int i = 1; i <= iterations; ++i) {
                    final TreeNode treeNode1 = bottomUpTree(depth);
                    check += treeNode1.itemCheck();
                }
                results[(depth - MIN_DEPTH) / 2] = String.valueOf(check);
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(120L, TimeUnit.SECONDS);

        int totalCheck = stretchCheck;
        for (int i = 0; i < results.length; i++) {
            if (results[i] != null) {
                totalCheck += Integer.parseInt(results[i]);
            }
        }
        totalCheck += longLivedTree.itemCheck();
        
        return totalCheck;
    }

    private static TreeNode bottomUpTree(final int depth) {
        if (0 < depth) {
            return new TreeNode(bottomUpTree(depth - 1), bottomUpTree(depth - 1));
        }
        return new TreeNode();
    }

    private static final class TreeNode {

        private final TreeNode left;
        private final TreeNode right;

        private TreeNode(final TreeNode left, final TreeNode right) {
            this.left = left;
            this.right = right;
        }

        private TreeNode() {
            this(null, null);
        }

        private int itemCheck() {
            if (null == left) {
                return 1;
            }
            return 1 + left.itemCheck() + right.itemCheck();
        }
    }
}

