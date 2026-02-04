package com.example.packagemanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dependency {
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;
    private List<Exclusion> exclusions;
    private boolean optional;

    public Dependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = "compile";
        this.exclusions = new ArrayList<>();
        this.optional = false;
    }

    public String getCoordinates() {
        return groupId + ":" + artifactId + ":" + version;
    }

    public String getKey() {
        return groupId + ":" + artifactId;
    }

    @Override
    public String toString() {
        return getCoordinates();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Exclusion {
        private String groupId;
        private String artifactId;
    }
}
