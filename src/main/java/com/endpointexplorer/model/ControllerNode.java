package com.endpointexplorer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree node representing a Spring controller (grouping node).
 */
public class ControllerNode {

    private final String controllerName;
    private final String basePath;
    private final List<EndpointData> endpoints = new ArrayList<>();

    public ControllerNode(String controllerName, String basePath) {
        this.controllerName = controllerName;
        this.basePath = basePath;
    }

    public String getControllerName() { return controllerName; }
    public String getBasePath() { return basePath; }
    public List<EndpointData> getEndpoints() { return endpoints; }

    public void addEndpoint(EndpointData endpoint) {
        endpoints.add(endpoint);
    }

    /**
     * Display label: "UserController (/api/users)"
     */
    public String getDisplayLabel() {
        if (basePath != null && !basePath.isEmpty()) {
            return controllerName + " (" + basePath + ")";
        }
        return controllerName;
    }

    public int getEndpointCount() {
        return endpoints.size();
    }
}
