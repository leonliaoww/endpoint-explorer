package com.endpointexplorer.model;

import com.intellij.psi.PsiMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single API endpoint discovered in a Spring Boot controller.
 */
public class EndpointData {

    private final String httpMethod;    // GET, POST, PUT, DELETE, PATCH
    private final String fullPath;      // e.g. /api/users/{id}
    private final String controllerName; // e.g. UserController
    private final String methodName;    // e.g. getUserById
    private final String description;   // optional Javadoc / comment
    private final PsiMethod psiMethod;  // for source navigation
    private final List<String> parameters;
    private final List<String> pathVariables;
    private final String returnType;
    private final boolean hasRequestBody;
    private final String controllerBasePath;

    private EndpointData(Builder builder) {
        this.httpMethod = builder.httpMethod;
        this.fullPath = builder.fullPath;
        this.controllerName = builder.controllerName;
        this.methodName = builder.methodName;
        this.description = builder.description;
        this.psiMethod = builder.psiMethod;
        this.parameters = builder.parameters;
        this.pathVariables = builder.pathVariables;
        this.returnType = builder.returnType;
        this.hasRequestBody = builder.hasRequestBody;
        this.controllerBasePath = builder.controllerBasePath;
    }

    // ----- Getters -----

    public String getHttpMethod() { return httpMethod; }
    public String getFullPath() { return fullPath; }
    public String getControllerName() { return controllerName; }
    public String getMethodName() { return methodName; }
    public String getDescription() { return description; }
    public PsiMethod getPsiMethod() { return psiMethod; }
    public List<String> getParameters() { return parameters; }
    public List<String> getPathVariableNames() { return pathVariables; }
    public String getReturnType() { return returnType; }
    public boolean hasRequestBody() { return hasRequestBody; }
    public String getControllerBasePath() { return controllerBasePath; }

    /**
     * Display label shown in the tree: "GET /api/users"
     */
    public String getDisplayLabel() {
        return httpMethod + " " + fullPath;
    }

    /**
     * Detail text for tooltip or detail panel.
     */
    public String getDetailText() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(httpMethod).append("] ").append(fullPath).append("\n");
        sb.append("Controller: ").append(controllerName).append("\n");
        sb.append("Method: ").append(methodName).append("()\n");
        sb.append("Return: ").append(returnType).append("\n");
        if (!parameters.isEmpty()) {
            sb.append("Params: ").append(String.join(", ", parameters)).append("\n");
        }
        if (!pathVariables.isEmpty()) {
            sb.append("Path Vars: ").append(String.join(", ", pathVariables)).append("\n");
        }
        if (hasRequestBody) {
            sb.append("Request Body: Yes\n");
        }
        if (description != null && !description.isEmpty()) {
            sb.append("Description: ").append(description);
        }
        return sb.toString();
    }

    // ----- Builder -----

    public static class Builder {
        private String httpMethod;
        private String fullPath;
        private String controllerName;
        private String methodName;
        private String description;
        private PsiMethod psiMethod;
        private List<String> parameters = new ArrayList<>();
        private List<String> pathVariables = new ArrayList<>();
        private String returnType = "void";
        private boolean hasRequestBody;
        private String controllerBasePath = "";

        public Builder httpMethod(String httpMethod) { this.httpMethod = httpMethod; return this; }
        public Builder fullPath(String fullPath) { this.fullPath = fullPath; return this; }
        public Builder controllerName(String controllerName) { this.controllerName = controllerName; return this; }
        public Builder methodName(String methodName) { this.methodName = methodName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder psiMethod(PsiMethod psiMethod) { this.psiMethod = psiMethod; return this; }
        public Builder parameters(List<String> parameters) { this.parameters = parameters; return this; }
        public Builder pathVariables(List<String> pathVariables) { this.pathVariables = pathVariables; return this; }
        public Builder returnType(String returnType) { this.returnType = returnType; return this; }
        public Builder hasRequestBody(boolean hasRequestBody) { this.hasRequestBody = hasRequestBody; return this; }
        public Builder controllerBasePath(String controllerBasePath) { this.controllerBasePath = controllerBasePath; return this; }

        public EndpointData build() {
            return new EndpointData(this);
        }
    }
}
