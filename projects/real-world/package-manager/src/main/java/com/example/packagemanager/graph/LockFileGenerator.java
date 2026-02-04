package com.example.packagemanager.graph;

import com.example.packagemanager.model.Dependency;
import com.example.packagemanager.model.LockFile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;

public class LockFileGenerator {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void generate(List<Dependency> dependencies, File outputFile) throws IOException {
        LockFile lockFile = new LockFile();

        for (Dependency dependency : dependencies) {
            LockFile.LockedDependency lockedDep = new LockFile.LockedDependency();
            lockedDep.setGroupId(dependency.getGroupId());
            lockedDep.setArtifactId(dependency.getArtifactId());
            lockedDep.setVersion(dependency.getVersion());
            lockedDep.setResolved(dependency.getCoordinates());
            lockedDep.setChecksum(generateChecksum(dependency));

            lockFile.addDependency(dependency.getKey(), lockedDep);
        }

        try (FileWriter writer = new FileWriter(outputFile)) {
            gson.toJson(lockFile, writer);
        }

        System.out.println("Lock file generated: " + outputFile.getAbsolutePath());
    }

    private String generateChecksum(Dependency dependency) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(dependency.getCoordinates().getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
