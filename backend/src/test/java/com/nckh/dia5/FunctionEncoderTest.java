package com.nckh.dia5;

import org.junit.jupiter.api.Test;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Hash;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class FunctionEncoderTest {

    @Test
    public void testRegisterItemsSelector() {
        // Test what selector web3j generates for registerItems
        String functionName = "registerItems";
        
        // Expected selector calculation
        String signature = "registerItems(uint256,string[],bytes32[][])";
        byte[] hash = Hash.sha3(signature.getBytes(StandardCharsets.UTF_8));
        String expectedSelector = "0x" + bytesToHex(Arrays.copyOfRange(hash, 0, 4));
        
        System.out.println("=====================================");
        System.out.println("Function Signature: " + signature);
        System.out.println("Expected Selector: " + expectedSelector);
        System.out.println("=====================================");
        
        // Create a simple test function
        List<Utf8String> itemCodes = Arrays.asList(
            new Utf8String("ITEM-001"),
            new Utf8String("ITEM-002")
        );
        
        List<Bytes32> proof1 = Arrays.asList(
            new Bytes32(new byte[32]),
            new Bytes32(new byte[32])
        );
        List<Bytes32> proof2 = Arrays.asList(
            new Bytes32(new byte[32]),
            new Bytes32(new byte[32])
        );
        
        @SuppressWarnings({"unchecked", "rawtypes"})
        DynamicArray<DynamicArray<Bytes32>> proofs = new DynamicArray(
            DynamicArray.class,
            Arrays.<DynamicArray<Bytes32>>asList(
                new DynamicArray<>(Bytes32.class, proof1),
                new DynamicArray<>(Bytes32.class, proof2)
            )
        );
        
        Function function = new Function(
            functionName,
            Arrays.asList(
                new Uint256(BigInteger.valueOf(12345)),
                new DynamicArray<>(Utf8String.class, itemCodes),
                proofs
            ),
            Arrays.asList()
        );
        
        // Encode function
        String encoded = FunctionEncoder.encode(function);
        String actualSelector = encoded.substring(0, 10);
        
        System.out.println("Encoded Function:");
        System.out.println("  Length: " + encoded.length());
        System.out.println("  Actual Selector: " + actualSelector);
        System.out.println("  First 66 chars: " + encoded.substring(0, Math.min(66, encoded.length())));
        System.out.println("=====================================");
        System.out.println("Match: " + (expectedSelector.equals(actualSelector) ? "✅ YES" : "❌ NO"));
        
        if (!expectedSelector.equals(actualSelector)) {
            System.out.println("⚠️ WARNING: Selector mismatch!");
            System.out.println("Expected: " + expectedSelector);
            System.out.println("Actual:   " + actualSelector);
            
            // Print full encoded data for debugging
            System.out.println("\nFull encoded data:");
            System.out.println(encoded);
        }
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}

