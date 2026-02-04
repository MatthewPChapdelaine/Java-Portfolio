package com.example.packagemanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LockFile {
    private String version = "1.0";
    private Map<String, LockedDependency> dependencies;

    public LockFile() {
        this.dependencies = new HashMap<>();
    }

    public void addDependency(String key, LockedDependency dependency) {
        this.dependencies.put(key, dependency);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LockedDependency {
        private String groupId;
        private String artifactId;
        private String version;
        private String resolved;
        private String checksum;
    }
}
