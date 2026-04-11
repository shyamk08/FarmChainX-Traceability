package com.farmchainX.farmchainX.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.farmchainX.farmchainX.model.SupplyChainLog;

public class HashUtil {

    public static String computeHash(SupplyChainLog log, String previousHash) {
        try {
            String data = log.getProductId() + "|" +
                          log.getFromUserId() + "|" +
                          log.getToUserId() + "|" +
                          log.getTimestamp().toString() + "|" +
                          (log.getLocation() == null ? "" : log.getLocation()) + "|" +
                          (log.getNotes() == null ? "" : log.getNotes()) + "|" +
                          (previousHash == null ? "" : previousHash);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating SHA-256 hash", e);
        }
    }
}