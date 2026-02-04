package com.example.packagemanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    private String groupId;
    private String artifactId;
    private String version;
    private String packaging;
    private String name;
    private String description;
    private List<Dependency> dependencies;

    public Project(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = "jar";
        this.dependencies = new ArrayList<>();
    }

    public String getCoordinates() {
        return groupId + ":" + artifactId + ":" + version;
    }

    public void addDependency(Dependency dependency) {
        this.dependencies.add(dependency);
    }
}
