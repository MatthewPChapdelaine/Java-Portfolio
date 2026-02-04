package com.example.packagemanager.parser;

import com.example.packagemanager.model.Dependency;
import com.example.packagemanager.model.Project;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.FileReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JsonManifestParser {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Project parse(File jsonFile) throws Exception {
        JsonObject root = gson.fromJson(new FileReader(jsonFile), JsonObject.class);

        String groupId = root.get("groupId").getAsString();
        String artifactId = root.get("artifactId").getAsString();
        String version = root.get("version").getAsString();

        Project project = new Project(groupId, artifactId, version);

        if (root.has("name")) {
            project.setName(root.get("name").getAsString());
        }
        if (root.has("description")) {
            project.setDescription(root.get("description").getAsString());
        }
        if (root.has("packaging")) {
            project.setPackaging(root.get("packaging").getAsString());
        }

        if (root.has("dependencies")) {
            JsonArray depsArray = root.getAsJsonArray("dependencies");
            for (int i = 0; i < depsArray.size(); i++) {
                JsonObject depObj = depsArray.get(i).getAsJsonObject();
                Dependency dep = new Dependency(
                    depObj.get("groupId").getAsString(),
                    depObj.get("artifactId").getAsString(),
                    depObj.get("version").getAsString()
                );
                if (depObj.has("scope")) {
                    dep.setScope(depObj.get("scope").getAsString());
                }
                if (depObj.has("optional")) {
                    dep.setOptional(depObj.get("optional").getAsBoolean());
                }
                project.addDependency(dep);
            }
        }

        return project;
    }
}
