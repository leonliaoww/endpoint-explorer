package com.endpointexplorer.scanner;

import com.endpointexplorer.model.ControllerNode;
import com.endpointexplorer.model.EndpointData;
import com.endpointexplorer.settings.EndpointExplorerSettings;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.*;

/**
 * Scans the project for Spring Boot REST controllers and extracts endpoint data.
 * Uses text-based annotation detection to work even without full dependency resolution.
 *
 * 改：现在支持自定义注解了。
 * 在构造函数里会去读取 EndpointExplorerSettings，
 * 把用户配置的自定义注解和默认 Spring 注解合并到一起检测。
 */
public class EndpointScanner {

    private final Project project;
    private int javaFileCount = 0;
    private int classCount = 0;

    // 默认支持的 Spring 注解
    private static final Set<String> DEFAULT_CONTROLLER_NAMES = Set.of("Controller", "RestController");
    private static final Map<String, String> DEFAULT_METHOD_MAPPING = new LinkedHashMap<>();
    static {
        DEFAULT_METHOD_MAPPING.put("GetMapping", "GET");
        DEFAULT_METHOD_MAPPING.put("PostMapping", "POST");
        DEFAULT_METHOD_MAPPING.put("PutMapping", "PUT");
        DEFAULT_METHOD_MAPPING.put("DeleteMapping", "DELETE");
        DEFAULT_METHOD_MAPPING.put("PatchMapping", "PATCH");
        DEFAULT_METHOD_MAPPING.put("RequestMapping", "");
    }

    // 实际使用的注解集合（默认 + 用户自定义）
    private final Set<String> effectiveControllerNames;
    private final Map<String, String> effectiveMethodMapping;

    public EndpointScanner(Project project) {
        this.project = project;

        // ── 读用户配置，合并到有效集合里 ──
        effectiveControllerNames = new LinkedHashSet<>(DEFAULT_CONTROLLER_NAMES);
        effectiveMethodMapping = new LinkedHashMap<>(DEFAULT_METHOD_MAPPING);

        try {
            EndpointExplorerSettings settings = EndpointExplorerSettings.getInstance();
            // 添加自定义 Controller 注解
            for (String ann : settings.getCustomControllerAnnotations()) {
                effectiveControllerNames.add(ann.trim());
            }
            // 添加自定义 Mapping 注解（格式：注解名:HTTP方法）
            for (String ann : settings.getCustomMappingAnnotations()) {
                String trimmed = ann.trim();
                if (trimmed.contains(":")) {
                    String[] parts = trimmed.split(":", 2);
                    effectiveMethodMapping.put(parts[0].trim(), parts[1].trim().toUpperCase());
                }
            }
        } catch (Exception e) {
            // 如果取设置失败（比如插件刚安装还没初始化），至少默认注解能用
            System.err.println("[EndpointExplorer] Failed to load settings: " + e.getMessage());
        }
    }

    public int getJavaFileCount() { return javaFileCount; }

    /**
     * Main entry point: scan the entire project for Spring endpoints.
     */
    public List<ControllerNode> scan(ProgressIndicator indicator) {
        List<ControllerNode> result = new ArrayList<>();
        Map<String, ControllerNode> controllerMap = new LinkedHashMap<>();
        javaFileCount = 0;
        classCount = 0;

        // ── 找所有 .java 文件 ──
        List<VirtualFile> allJavaFiles = new ArrayList<>();
        try {
            FileType javaFileType = FileTypeManager.getInstance().getFileTypeByExtension("java");
            GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
            if (javaFileType != null) {
                allJavaFiles.addAll(FileTypeIndex.getFiles(javaFileType, scope));
            }
        } catch (Exception e) {
            System.err.println("[EndpointExplorer] FileTypeIndex failed: " + e.getMessage());
        }

        // 如果 FileTypeIndex 没找到，兜底：遍历项目目录
        if (allJavaFiles.isEmpty()) {
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir != null) {
                VfsUtilCore.iterateChildrenRecursively(baseDir,
                        file -> !file.isDirectory() && file.getName().endsWith(".java"),
                        file -> {
                            String path = file.getPath();
                            if (path.contains("/target/") || path.contains("/build/")
                                    || path.contains("\\target\\") || path.contains("\\build\\"))
                                return true;
                            allJavaFiles.add(file);
                            return true;
                        });
            }
        }

        int total = Math.max(allJavaFiles.size(), 1);
        int fileIndex = 0;
        int endpointCount = 0;

        // ── 遍历所有 Java 文件 ──
        for (VirtualFile vf : allJavaFiles) {
            if (indicator != null && indicator.isCanceled()) break;
            if (indicator != null) {
                indicator.setFraction((double) fileIndex / total);
                indicator.setText2(vf.getPath());
            }

            try {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(vf);
                if (!(psiFile instanceof PsiJavaFile)) continue;
                PsiJavaFile javaFile = (PsiJavaFile) psiFile;
                javaFileCount++;

                for (PsiClass clazz : javaFile.getClasses()) {
                    classCount++;
                    PsiAnnotation[] annotations = clazz.getModifierList() != null
                            ? clazz.getModifierList().getAnnotations() : new PsiAnnotation[0];

                    // 检查类注解：是不是 @Controller / @RestController / 用户自定义
                    String controllerBasePath = "";
                    boolean isController = false;
                    for (PsiAnnotation ann : annotations) {
                        String text = ann.getText();
                        if (text == null) continue;
                        String shortName = getAnnotationShortName(text);

                        if (effectiveControllerNames.contains(shortName)) {
                            isController = true;
                        } else if ("RequestMapping".equals(shortName)) {
                            controllerBasePath = extractPathValue(ann);
                        }
                    }
                    if (!isController) continue;

                    String className = clazz.getName() != null ? clazz.getName() : "Unknown";
                    final String finalBasePath = controllerBasePath;
                    final String finalClassName = className;

                    ControllerNode node = controllerMap.computeIfAbsent(
                            className + "|" + controllerBasePath,
                            k -> new ControllerNode(finalClassName, finalBasePath)
                    );

                    // 遍历当前 Controller 里的所有方法
                    for (PsiMethod method : clazz.getMethods()) {
                        if (method.getBody() == null) continue;
                        PsiAnnotation[] methodAnnotations = method.getModifierList() != null
                                ? method.getModifierList().getAnnotations() : new PsiAnnotation[0];

                        String httpMethod = "";
                        String methodPath = "";
                        boolean hasMapping = false;

                        for (PsiAnnotation ann : methodAnnotations) {
                            String text = ann.getText();
                            if (text == null) continue;
                            String shortName = getAnnotationShortName(text);

                            if (effectiveMethodMapping.containsKey(shortName)) {
                                hasMapping = true;
                                String mappedMethod = effectiveMethodMapping.get(shortName);
                                if (!mappedMethod.isEmpty()) {
                                    httpMethod = mappedMethod;
                                }
                                String path = extractPathValue(ann);
                                if (!path.isEmpty()) methodPath = path;
                            }
                        }

                        if (!hasMapping) continue;
                        if (httpMethod.isEmpty()) httpMethod = "ANY";

                        String fullPath = normalizePath(controllerBasePath, methodPath);
                        endpointCount++;

                        EndpointData endpoint = buildEndpoint(method, httpMethod, fullPath, className, controllerBasePath);
                        node.addEndpoint(endpoint);
                    }
                }
            } catch (Exception e) {
                System.err.println("[EndpointExplorer] Error: " + e.getMessage());
            }
            fileIndex++;
        }

        result.addAll(controllerMap.values());
        result.sort(Comparator.comparing(ControllerNode::getControllerName));
        return result;
    }

    // ========== 工具方法 ==========

    private String getAnnotationShortName(String text) {
        if (text == null || text.isEmpty()) return "";
        text = text.trim();
        if (text.startsWith("@")) text = text.substring(1);
        int paren = text.indexOf('(');
        if (paren > 0) text = text.substring(0, paren);
        return text;
    }

    private String extractPathValue(PsiAnnotation annotation) {
        String val = extractAttribute(annotation, "value");
        if (val != null && !val.isEmpty()) return val;
        val = extractAttribute(annotation, "path");
        if (val != null && !val.isEmpty()) return val;

        String text = annotation.getText();
        if (text != null && text.contains("(\"")) {
            int start = text.indexOf('\"');
            int end = text.indexOf('\"', start + 1);
            if (start > 0 && end > start) {
                return text.substring(start + 1, end);
            }
        }
        return "";
    }

    private String extractAttribute(PsiAnnotation annotation, String attribute) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attribute);
        if (value == null) return null;
        String text = value.getText();
        if (text == null) return null;
        text = text.trim();
        if (text.startsWith("{")) text = text.substring(1).trim();
        if (text.endsWith("}")) text = text.substring(0, text.length() - 1).trim();
        if (text.startsWith("\"")) text = text.substring(1);
        if (text.endsWith("\"")) text = text.substring(0, text.length() - 1);
        return text;
    }

    private String normalizePath(String base, String method) {
        if (base.isEmpty()) return method.startsWith("/") ? method : "/" + method;
        if (method.isEmpty()) return base.startsWith("/") ? base : "/" + base;
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base)
                + (method.startsWith("/") ? method : "/" + method);
    }

    private EndpointData buildEndpoint(PsiMethod method, String httpMethod, String fullPath,
                                       String className, String basePath) {
        EndpointData.Builder builder = new EndpointData.Builder()
                .httpMethod(httpMethod)
                .fullPath(fullPath)
                .controllerName(className)
                .methodName(method.getName())
                .psiMethod(method)
                .controllerBasePath(basePath);

        List<String> params = new ArrayList<>();
        List<String> pathVars = new ArrayList<>();
        boolean hasBody = false;
        String returnType = "void";

        for (PsiParameter param : method.getParameterList().getParameters()) {
            String paramType = param.getType().getPresentableText();
            String paramName = param.getName() != null ? param.getName() : "arg";

            String annText = getParamAnnotationText(param);
            if (annText.contains("RequestParam")) {
                params.add(paramName + ": " + paramType);
            } else if (annText.contains("PathVariable")) {
                pathVars.add(paramName + ": " + paramType);
            } else if (annText.contains("RequestBody")) {
                hasBody = true;
                params.add("[Body] " + paramName + ": " + paramType);
            } else if (annText.contains("RequestHeader")) {
                params.add("[Header] " + paramName + ": " + paramType);
            } else {
                params.add(paramName + ": " + paramType);
            }
        }

        builder.parameters(params);
        builder.pathVariables(pathVars);
        builder.hasRequestBody(hasBody);

        PsiType returnPsiType = method.getReturnType();
        if (returnPsiType != null) {
            returnType = returnPsiType.getPresentableText();
        }
        builder.returnType(returnType);

        return builder.build();
    }

    private String getParamAnnotationText(PsiParameter param) {
        PsiModifierList modifiers = param.getModifierList();
        if (modifiers == null) return "";
        for (PsiAnnotation ann : modifiers.getAnnotations()) {
            String text = ann.getText();
            if (text != null) return text;
        }
        return "";
    }
}
