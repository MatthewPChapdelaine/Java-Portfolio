package com.example.packagemanager.parser;

import com.example.packagemanager.model.Dependency;
import com.example.packagemanager.model.Project;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PomParser {
    
    public Project parse(File pomFile) throws Exception {
        SAXReader reader = new SAXReader();
        Document document = reader.read(pomFile);
        Element root = document.getRootElement();

        String groupId = getElementText(root, "groupId");
        String artifactId = getElementText(root, "artifactId");
        String version = getElementText(root, "version");
        String packaging = getElementText(root, "packaging", "jar");
        String name = getElementText(root, "name");
        String description = getElementText(root, "description");

        Project project = new Project(groupId, artifactId, version);
        project.setPackaging(packaging);
        project.setName(name);
        project.setDescription(description);

        Element dependenciesElement = root.element("dependencies");
        if (dependenciesElement != null) {
            List<Element> dependencyElements = dependenciesElement.elements("dependency");
            for (Element depElement : dependencyElements) {
                Dependency dependency = parseDependency(depElement);
                project.addDependency(dependency);
            }
        }

        return project;
    }

    private Dependency parseDependency(Element element) {
        String groupId = getElementText(element, "groupId");
        String artifactId = getElementText(element, "artifactId");
        String version = getElementText(element, "version");
        String scope = getElementText(element, "scope", "compile");
        boolean optional = Boolean.parseBoolean(getElementText(element, "optional", "false"));

        Dependency dependency = new Dependency(groupId, artifactId, version);
        dependency.setScope(scope);
        dependency.setOptional(optional);

        Element exclusionsElement = element.element("exclusions");
        if (exclusionsElement != null) {
            List<Element> exclusionElements = exclusionsElement.elements("exclusion");
            for (Element exElement : exclusionElements) {
                String exGroupId = getElementText(exElement, "groupId");
                String exArtifactId = getElementText(exElement, "artifactId");
                dependency.getExclusions().add(new Dependency.Exclusion(exGroupId, exArtifactId));
            }
        }

        return dependency;
    }

    private String getElementText(Element parent, String elementName) {
        Element element = parent.element(elementName);
        return element != null ? element.getTextTrim() : null;
    }

    private String getElementText(Element parent, String elementName, String defaultValue) {
        String value = getElementText(parent, elementName);
        return value != null ? value : defaultValue;
    }
}
