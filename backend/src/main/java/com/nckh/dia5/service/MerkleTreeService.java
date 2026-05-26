package com.nckh.dia5.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service để tạo và verify Merkle Tree
 * Dùng để tiết kiệm gas khi ghi nhiều items lên blockchain
 */
@Service
@Slf4j
public class MerkleTreeService {

    /**
     * Tạo Merkle Tree từ danh sách item codes
     * @param itemCodes Danh sách item codes
     * @return MerkleTree object
     */
    public MerkleTree createMerkleTree(List<String> itemCodes) {
        if (itemCodes == null || itemCodes.isEmpty()) {
            throw new IllegalArgumentException("Item codes list cannot be empty");
        }

        log.debug("Creating Merkle tree for {} items", itemCodes.size());

        // Create leaf nodes (hash of each item code)
        List<String> leaves = new ArrayList<>();
        for (String itemCode : itemCodes) {
            String leaf = hashLeaf(itemCode);
            leaves.add(leaf);
        }

        // Sort leaves for consistent ordering
        leaves.sort(String::compareTo);

        // Build tree
        List<List<String>> levels = new ArrayList<>();
        levels.add(leaves);

        List<String> currentLevel = leaves;
        while (currentLevel.size() > 1) {
            currentLevel = buildNextLevel(currentLevel);
            levels.add(currentLevel);
        }

        String root = currentLevel.get(0);
        
        MerkleTree tree = new MerkleTree(root, leaves, levels);
        log.info("Created Merkle tree with root: {}", root);
        
        return tree;
    }

    /**
     * Build next level của Merkle Tree
     */
    private List<String> buildNextLevel(List<String> currentLevel) {
        List<String> nextLevel = new ArrayList<>();
        
        for (int i = 0; i < currentLevel.size(); i += 2) {
            if (i + 1 < currentLevel.size()) {
                // Combine two nodes
                String left = currentLevel.get(i);
                String right = currentLevel.get(i + 1);
                String parent = hashPair(left, right);
                nextLevel.add(parent);
            } else {
                // Odd number of nodes, promote the last one
                nextLevel.add(currentLevel.get(i));
            }
        }
        
        return nextLevel;
    }

    /**
     * Hash một leaf node (item code)
     */
    private String hashLeaf(String itemCode) {
        return Hash.sha3String(itemCode);
    }

    /**
     * Hash một cặp nodes
     */
    private String hashPair(String left, String right) {
        // Sort để đảm bảo consistent ordering
        String concatenated;
        if (left.compareTo(right) < 0) {
            concatenated = left + right;
        } else {
            concatenated = right + left;
        }
        
        return Numeric.toHexString(Hash.sha3(concatenated.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Generate Merkle proof cho một item code
     * @param tree MerkleTree
     * @param itemCode Item code cần prove
     * @return List of proof hashes
     */
    public List<String> generateProof(MerkleTree tree, String itemCode) {
        String leaf = hashLeaf(itemCode);
        
        int index = tree.getLeaves().indexOf(leaf);
        if (index == -1) {
            throw new IllegalArgumentException("Item code not found in tree: " + itemCode);
        }

        List<String> proof = new ArrayList<>();
        List<List<String>> levels = tree.getLevels();
        
        int currentIndex = index;
        
        for (int level = 0; level < levels.size() - 1; level++) {
            List<String> currentLevel = levels.get(level);
            
            // Get sibling
            int siblingIndex;
            if (currentIndex % 2 == 0) {
                // Left node, sibling is on the right
                siblingIndex = currentIndex + 1;
            } else {
                // Right node, sibling is on the left
                siblingIndex = currentIndex - 1;
            }
            
            if (siblingIndex < currentLevel.size()) {
                proof.add(currentLevel.get(siblingIndex));
            }
            
            // Move to parent in next level
            currentIndex = currentIndex / 2;
        }
        
        log.debug("Generated proof for item: {}, proof size: {}", itemCode, proof.size());
        return proof;
    }

    /**
     * Verify Merkle proof
     * @param itemCode Item code to verify
     * @param proof Merkle proof
     * @param root Merkle root
     * @return true if valid
     */
    public boolean verifyProof(String itemCode, List<String> proof, String root) {
        String computedHash = hashLeaf(itemCode);
        
        for (String proofElement : proof) {
            computedHash = hashPair(computedHash, proofElement);
        }
        
        boolean isValid = computedHash.equals(root);
        log.debug("Verified proof for item: {}, valid: {}", itemCode, isValid);
        
        return isValid;
    }

    /**
     * MerkleTree data structure
     */
    public static class MerkleTree {
        private final String root;
        private final List<String> leaves;
        private final List<List<String>> levels;

        public MerkleTree(String root, List<String> leaves, List<List<String>> levels) {
            this.root = root;
            this.leaves = leaves;
            this.levels = levels;
        }

        public String getRoot() {
            return root;
        }

        public List<String> getLeaves() {
            return leaves;
        }

        public List<List<String>> getLevels() {
            return levels;
        }

        public int getSize() {
            return leaves.size();
        }

        public int getHeight() {
            return levels.size();
        }
    }
}

