/*
 * To change this license header, choose License Headers in Project Properties. To change this template file, choose
 * Tools | Templates and open the template in the editor.
 */
package com.cludimpl.errorcode.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;


/**
 *
 * @author nuwansa
 */
@Mojo(name = "ErrorCodeGenerator",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresOnline = false, requiresProject = true,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    threadSafe = false)

public class ErrorCodeGenerator extends AbstractMojo {

  /**
   * @parameter expression="${project}"
   * @required
   */
  @Parameter(defaultValue = "${project}", required = true)
  protected MavenProject project;

  @Parameter(property = "errorCode.errorFileName", required = true, defaultValue = "${project.artifactId}")
  protected String errorFileName;

  @Parameter(property = "errorCode.package", required = true, defaultValue = "com.ustack.error")
  protected String errorPackage;

  @Parameter(property = "errorCode.enable", required = true, defaultValue = "false")
  protected boolean enableErrorCode;

  @Parameter(defaultValue = "${project.build.directory}/generated-sources/errorcodes", required = true)
  protected File errorOutputFolder;

  @Parameter(defaultValue = "${project.basedir}/src/main/java", required = true)
  protected File projectSourceDirectory;

  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  protected String projectTargetDirectory;

  private File errorFile;

  private Optional<String> errLibPath;

  private List<String> runtimeClasspathElements;

  public static void main(String[] args) {
    new ErrorCodeGenerator().init();
  }

  private void init() {

    if (this.project != null) {
      try {
        String regexPath = ".*(com).*(cloudimpl).*(error-lib).*";
        this.project.addCompileSourceRoot(this.errorOutputFolder.getAbsolutePath());
        runtimeClasspathElements = project.getRuntimeClasspathElements();
        runtimeClasspathElements.addAll(project.getCompileClasspathElements());
        errLibPath = project.getArtifacts()
            .stream()
            .map(s -> s.getFile().getAbsolutePath())
            .filter(s -> s.matches(regexPath))
            .findFirst();
        if (errLibPath.isPresent())
          runtimeClasspathElements.add(errLibPath.get());

      } catch (DependencyResolutionRequiredException ex) {
        Logger.getLogger(ErrorCodeGenerator.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    if (!this.errorOutputFolder.exists() && !this.errorOutputFolder.mkdirs()) {
      getLog().error("Could not create error directory!");
    }
    cleanGeneratedCodes(errorOutputFolder);
  }

  private void cleanGeneratedCodes(File directory) {
    File[] files = directory.listFiles();
    for (File f : files) {
      if (f.isDirectory()) {
        cleanGeneratedCodes(f);
      } else {
        f.delete();
      }
    }
  }

  private ClassLoader getClassLoader() {

    URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
    for (int i = 0; i < runtimeClasspathElements.size(); i++) {
      try {
        String element = (String) runtimeClasspathElements.get(i);
        runtimeUrls[i] = new File(element).toURI().toURL();
      } catch (MalformedURLException ex) {
        Logger.getLogger(ErrorCodeGenerator.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    URLClassLoader newLoader = new URLClassLoader(runtimeUrls,
        Thread.currentThread().getContextClassLoader());
    return newLoader;

  }

  private void compileErrorFile() {
    File[] javaFiles = new File[] {new File(errorFile.getAbsolutePath())};
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

    Iterable<? extends JavaFileObject> compilationUnits =
        fileManager.getJavaFileObjectsFromFiles(Arrays.asList(javaFiles));

    List<String> optionList = new ArrayList<>();
    if (errLibPath.isPresent()) {
      optionList.addAll(Arrays.asList("-classpath", errLibPath.get()));
    }
    File targetDir = new File(projectTargetDirectory);
    targetDir.mkdir();
    optionList.addAll(Arrays.asList("-d", projectTargetDirectory));
    JavaCompiler.CompilationTask task =
        compiler.getTask(null, fileManager, diagnostics, optionList, null, compilationUnits);
    try {
      boolean ok = task.call();
      getLog().info("error file compile status " + ok);
      diagnostics.getDiagnostics().stream().forEach(System.out::println);
    } catch (Exception ex) {
      getLog().error(ex);
    }
  }

  private <T> Class<T> getErrorClass() {
    compileErrorFile();
    ClassLoader classLoader = getClassLoader();
    try {
      return (Class<T>) classLoader.loadClass(errorPackage + "." + errorFileName);
    } catch (ClassNotFoundException ex) {
      getLog().error(ex);
      return null;
    }
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!enableErrorCode) {
      return;
    }
    try {
      errorFileName = makeFirstLetterCapital(errorFileName);
      init();
      createErrorFileIfNotExist();
      Class errorClazz = getErrorClass();
      if (errorClazz == null) {
        return;
      }
      Reflector ref = new Reflector(errorClazz);
      Object[] errorCodes = errorClazz.getEnumConstants();
      for (Object code : errorCodes) {
        System.out.println("name " + ref.getName(code));
        List<String> tags = ErrorCodeProcessor.getTags(ref.getErrorFormat(code));
        generateErrorBuilder(ref.getName(code), tags);
      }
      generateException(errorFileName + "Exception", Arrays.asList(errorCodes), ref);
    } catch (Exception ex) {
      getLog().error(ex);
      throw ex;
    }
  }

  private String makeFirstLetterCapital(String target) {
    return target.substring(0, 1).toUpperCase() + target.substring(1);
  }

  private MethodSpec createMethod(String name, String tag) {
    ParameterSpec parameterSpec = ParameterSpec.builder(Object.class, tag)
        .build();
    MethodSpec setter = MethodSpec.methodBuilder("set" + makeFirstLetterCapital(tag))
        .addParameter(parameterSpec)
        .returns(ClassName.get("", name))
        .addModifiers(Modifier.PUBLIC)
        .addCode("withTag(\"" + tag + "\", " + tag + ");\n return this;\n")
        .build();
    return setter;
  }

  private void generateErrorBuilder(String name, List<String> tags) {
    List<MethodSpec> methods = tags.stream().map(tag -> createMethod(name, tag)).collect(Collectors.toList());
    TypeSpec.Builder errorBuilder = TypeSpec.classBuilder(name)
        // .superclass(ErrorBuilder.class)
        .superclass(ClassName.get("com.cloudimpl.error.core", "ErrorBuilder"))
        .addModifiers(Modifier.PUBLIC);

    methods.forEach(method -> errorBuilder.addMethod(method));
    errorBuilder.addMethod(MethodSpec.constructorBuilder()
        .addStatement(" withCode($L)", errorPackage + "." + errorFileName + "." + name)
        .build());

    JavaFile javaFile = JavaFile.builder(errorPackage, errorBuilder.build())
        .skipJavaLangImports(true)
        .build();
    try {
      javaFile.writeTo(errorOutputFolder);
    } catch (IOException ex) {
      Logger.getLogger(ErrorCodeGenerator.class.getName()).log(Level.SEVERE, null, ex);
    }

  }

  private MethodSpec createExceptionMethod(String exceptionName, Object errCode, Reflector ref) {
    // TypeVariableName typeVariableName = TypeVariableName.get(ClassName.get("", name));
    List<String> tags = ErrorCodeProcessor.getTags(ref.getErrorFormat(errCode));
    ParameterizedTypeName paramterType = ParameterizedTypeName.get(ClassName.get("java.util.function", "Consumer"),
        ClassName.get("", ref.getName(errCode)));
    MethodSpec setter = MethodSpec.methodBuilder(ref.getName(errCode))
        .addParameter(paramterType, "consumer")
        .returns(ClassName.get("", exceptionName))
        .addModifiers(Modifier.PUBLIC)
        .addModifiers(Modifier.STATIC)
        .addCode(ref.getName(errCode) + " error = new " + ref.getName(errCode) + "();\n"
            + "consumer.accept(error);\n"
            + "return new " + exceptionName + "(error);")
        .addJavadoc("errorNo : $L \nformat : $S \ntags : $S\n@param consumer\n@return\n", ref.getErrorCode(errCode),
            ref.getErrorFormat(errCode), tags)
        .build();
    return setter;
  }

  private void generateException(String exceptionName, List<Object> errCodes, Reflector ref) {
    List<MethodSpec> list = errCodes.stream().map(errCode -> createExceptionMethod(exceptionName, errCode, ref))
        .collect(Collectors.toList());

    TypeSpec.Builder exceptionBuilder = TypeSpec.classBuilder(exceptionName)
        .superclass(ClassName.get("com.cloudimpl.error.core", "CloudImplException"))
        .addModifiers(Modifier.PUBLIC)
        .addMethod(MethodSpec.constructorBuilder()
            .addParameter(ClassName.get("com.cloudimpl.error.core", "ErrorBuilder"), "builder")
            .addStatement("super(builder)")
            .build());

    list.forEach(m -> exceptionBuilder.addMethod(m));
    JavaFile javaFile = JavaFile.builder(errorPackage, exceptionBuilder.build())
        .skipJavaLangImports(true)
        .build();
    try {
      javaFile.writeTo(errorOutputFolder);
    } catch (IOException ex) {
      Logger.getLogger(ErrorCodeGenerator.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void createErrorFileIfNotExist() {
    String packagePath = errorPackage.replaceAll("\\.", "/");
    errorFile = new File(projectSourceDirectory.getAbsolutePath() + "/" + packagePath + "/" + errorFileName + ".java");
    if (errorFile.exists()) {
      return;
    }
    FieldSpec errorNo = FieldSpec.builder(int.class, "errorNo")
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build();
    FieldSpec errorFormat = FieldSpec.builder(String.class, "errorFormat")
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build();

    MethodSpec errorNum = MethodSpec.methodBuilder("getErrorNo")
        .returns(int.class)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(AnnotationSpec.builder(Override.class).build())
        .addStatement("return errorNo", int.class)
        .build();
    MethodSpec format = MethodSpec.methodBuilder("getFormat")
        .returns(String.class)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(AnnotationSpec.builder(Override.class).build())
        .addStatement("return errorFormat", String.class)
        .build();

    TypeSpec error = TypeSpec.enumBuilder(errorFileName)
        .addModifiers(Modifier.PUBLIC)
        .addSuperinterface(ClassName.get("com.cloudimpl.error.core", "ErrorCode"))
        .addField(errorNo)
        .addField(errorFormat)
        .addMethod(errorNum)
        .addMethod(format)
        .addMethod(MethodSpec.constructorBuilder()
            .addParameter(int.class, "errorNo")
            .addParameter(String.class, "errorFormat")
            .addStatement("this.$N = $N", "errorNo", "errorNo")
            .addStatement("this.$N = $N", "errorFormat", "errorFormat")
            .build())
        .addEnumConstant("_", TypeSpec.anonymousClassBuilder("$L,$S", 0, "")
            .build())
        .build();

    JavaFile javaFile = JavaFile.builder(errorPackage, error).skipJavaLangImports(true)
        .build();
    try {
      javaFile.writeTo(projectSourceDirectory);
    } catch (IOException ex) {
      Logger.getLogger(ErrorCodeGenerator.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

}
