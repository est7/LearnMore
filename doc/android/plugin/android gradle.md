android 插件引用：

com.android.tools.build:gradle:4.2.2 ： 这个东西其实就是一个插件，也可以作为一个依赖包，可以查看源码

正常使用：

```groovy
buildscript {
    repositories {
        google()
        jcenter() 
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:4.2.2'
    }
}
```



```groovy
plugins {
    id 'com.android.application'
}

apply plugin:'com.asm.plugin'



android {
    compileSdkVersion 28
    buildToolsVersion "29.0.2"

    defaultConfig {
        applicationId "com.example.asmtest"
        minSdkVersion 14
        targetSdkVersion 28
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }

        debug {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

android 插件源码：

```groovy
/** Base class for all Android plugins */
public abstract class BasePlugin<
                AndroidComponentsT extends
                        AndroidComponentsExtension<? extends ComponentBuilder, ? extends Variant>,
                VariantBuilderT extends VariantBuilderImpl,
                VariantT extends VariantImpl>
        implements Plugin<Project>, LintModelModuleLoaderProvider {
            ..........................
     //入口 
    @Override
    public final void apply(@NonNull Project project) {
        CrashReporting.runAction(
                () -> {
                    basePluginApply(project);//主要
                    pluginSpecificApply(project);
                });
    }       
}
```

```groovy

private void basePluginApply(@NonNull Project project) {
        // We run by default in headless mode, so the JVM doesn't steal focus.
        System.setProperty("java.awt.headless", "true");

        this.project = project;

        new AnalyticsService.RegistrationAction(project).execute();

        configuratorService
                = new AnalyticsConfiguratorService.RegistrationAction(project).execute().get();

        optionService = new ProjectOptionService.RegistrationAction(project).execute().get();

        createProjectServices(project);

        ProjectOptions projectOptions = projectServices.getProjectOptions();

        DependencyResolutionChecks.registerDependencyCheck(project, projectOptions);

        project.getPluginManager().apply(AndroidBasePlugin.class);


        checkPathForErrors();
        checkModulesForErrors();

        AgpVersionChecker.enforceTheSamePluginVersions(project);

        String attributionFileLocation =
                projectOptions.get(StringOption.IDE_ATTRIBUTION_FILE_LOCATION);
        if (attributionFileLocation != null) {
            new BuildAttributionService.RegistrationAction(project).execute();
            BuildAttributionService.Companion.init(
                    project, attributionFileLocation, listenerRegistry);
        }

        configuratorService.createAnalyticsService(project, listenerRegistry);

        configuratorService.getProjectBuilder(project.getPath())
                .setAndroidPluginVersion(Version.ANDROID_GRADLE_PLUGIN_VERSION)
                .setAndroidPlugin(getAnalyticsPluginType())
                .setPluginGeneration(GradleBuildProject.PluginGeneration.FIRST)
                .setOptions(AnalyticsUtil.toProto(projectOptions));

        configuratorService.recordBlock(
                ExecutionType.BASE_PLUGIN_PROJECT_CONFIGURE,
                project.getPath(),
                null,
                this::configureProject);

        configuratorService.recordBlock(
                ExecutionType.BASE_PLUGIN_PROJECT_BASE_EXTENSION_CREATION,
                project.getPath(),
                null,
                this::configureExtension);//配置信息

        configuratorService.recordBlock(
                ExecutionType.BASE_PLUGIN_PROJECT_TASKS_CREATION,
                project.getPath(),
                null,
                this::createTasks);//组装task任务
    }
```

//配置信息

```java
private void configureExtension() {
        DslServices dslServices = globalScope.getDslServices();

        final NamedDomainObjectContainer<BaseVariantOutput> buildOutputs =
                project.container(BaseVariantOutput.class);

        project.getExtensions().add("buildOutputs", buildOutputs);

        variantFactory = createVariantFactory(projectServices, globalScope);

        variantInputModel =
                new LegacyVariantInputManager(
                        dslServices,
                        variantFactory.getVariantType(),
                        new SourceSetManager(
                                project,
                                isPackagePublished(),
                                dslServices,
                                new DelayedActionsExecutor()));

        extension =
                createExtension(
                        dslServices, globalScope, variantInputModel, buildOutputs, extraModelInfo);// 配置信息在这里

        globalScope.setExtension(extension);

        VariantApiOperationsRegistrar<VariantBuilderT, VariantT> variantApiOperations =
                new VariantApiOperationsRegistrar<>();
        androidComponentsExtension = createComponentExtension(dslServices, variantApiOperations);

        variantManager =
                new VariantManager(
                        globalScope,
                        project,
                        projectServices.getProjectOptions(),
                        extension,
                        variantApiOperations,
                        variantFactory,
                        variantInputModel,
                        projectServices);

        registerModels(
                registry,
                globalScope,
                variantInputModel,
                extension,
                extraModelInfo);

        // create default Objects, signingConfig first as its used by the BuildTypes.
        variantFactory.createDefaultComponents(variantInputModel);

        createAndroidTestUtilConfiguration();
    }
```



```java
public class AppPlugin extends AbstractAppPlugin<ApplicationAndroidComponentsExtension,ApplicationVariantBuilderImpl,ApplicationVariantImpl> {
                ...
     @NonNull
    @Override
    protected BaseExtension createExtension(
            @NonNull DslServices dslServices,
            @NonNull GlobalScope globalScope,
            @NonNull
                    DslContainerProvider<DefaultConfig, BuildType, ProductFlavor, SigningConfig>
                            dslContainers,
            @NonNull NamedDomainObjectContainer<BaseVariantOutput> buildOutputs,
            @NonNull ExtraModelInfo extraModelInfo) {
        if (globalScope.getProjectOptions().get(BooleanOption.USE_NEW_DSL_INTERFACES)) {
            return (BaseExtension)
                    project.getExtensions()
                            .create(
                                    ApplicationExtension.class,//参考android 配置
                                    "android",
                                    BaseAppModuleExtension.class,
                                    dslServices,
                                    globalScope,
                                    buildOutputs,
                                    dslContainers.getSourceSetManager(),
                                    extraModelInfo,
                                    new ApplicationExtensionImpl(dslServices, dslContainers));
        }
        return project.getExtensions()
                .create(
                        "android",
                        BaseAppModuleExtension.class,
                        dslServices,
                        globalScope,
                        buildOutputs,
                        dslContainers.getSourceSetManager(),
                        extraModelInfo,
                        new ApplicationExtensionImpl(dslServices, dslContainers));
    }
    ...
}
```



```java
@Incubating
interface ApplicationExtension<
        AndroidSourceSetT : AndroidSourceSet,
        BuildTypeT : ApplicationBuildType<SigningConfigT>,
        DefaultConfigT : ApplicationDefaultConfig<SigningConfigT>,
        ProductFlavorT : ApplicationProductFlavor<SigningConfigT>,
        SigningConfigT : SigningConfig> :
    CommonExtension<
            AndroidSourceSetT,
            ApplicationBuildFeatures,
            BuildTypeT,
            DefaultConfigT,
            ProductFlavorT,
            SigningConfigT,
            ApplicationVariantBuilder,
            ApplicationVariant>,
    ApkExtension,
    TestedExtension {
    // TODO(b/140406102)

    /** Specify whether to include SDK dependency information in APKs and Bundles. */
    val dependenciesInfo: DependenciesInfo

    /** Specify whether to include SDK dependency information in APKs and Bundles. */
    fun dependenciesInfo(action: DependenciesInfo.() -> Unit)

    val bundle: Bundle

    fun bundle(action: Bundle.() -> Unit)

    val dynamicFeatures: MutableSet<String>

    /**
     * Set of asset pack subprojects to be included in the app's bundle.
     */
    val assetPacks: MutableSet<String>
}
```



```java
@Incubating
interface ApplicationProductFlavor<SigningConfigT : SigningConfig> :
    ApplicationBaseFlavor<SigningConfigT>,
    ProductFlavor {
    /** Whether this product flavor should be selected in Studio by default  */
    var isDefault: Boolean
}
```



```java
@Incubating
interface ApplicationBaseFlavor<SigningConfigT : SigningConfig> :
    BaseFlavor,
    ApplicationVariantDimension<SigningConfigT> {
    /**
     * The application ID.
     *
     * See [Set the Application ID](https://developer.android.com/studio/build/application-id.html)
     */
    var applicationId: String?

    /**
     * Version code.
     *
     * See [Versioning Your Application](http://developer.android.com/tools/publishing/versioning.html)
     */
    var versionCode: Int?

    /**
     * Version name.
     *
     * See [Versioning Your Application](http://developer.android.com/tools/publishing/versioning.html)
     */
    var versionName: String?

    /**
     * The target SDK version.
     * Setting this it will override previous calls of [targetSdk] and [targetSdkPreview] setters.
     * Only one of [targetSdk] and [targetSdkPreview] should be set.
     *
     * See [uses-sdk element documentation](http://developer.android.com/guide/topics/manifest/uses-sdk-element.html).
     */
    var targetSdk: Int?

    /**
     * The target SDK version.
     * Setting this it will override previous calls of [targetSdk] and [targetSdkPreview] setters.
     * Only one of [targetSdk] and [targetSdkPreview] should be set.
     *
     * See [uses-sdk element documentation](http://developer.android.com/guide/topics/manifest/uses-sdk-element.html).
     */
    var targetSdkPreview: String?

    /**
     * The maxSdkVersion, or null if not specified. This is only the value set on this produce
     * flavor.
     *
     * See [uses-sdk element documentation](http://developer.android.com/guide/topics/manifest/uses-sdk-element.html).
     */
    var maxSdk: Int?
}
```

Task 信息：

```java
  private void createTasks() {
        configuratorService.recordBlock(
                ExecutionType.TASK_MANAGER_CREATE_TASKS,
                project.getPath(),
                null,
                () ->
                        TaskManager.createTasksBeforeEvaluate(
                                globalScope,
                                variantFactory.getVariantType(),
                                extension.getSourceSets()));

        project.afterEvaluate(
                CrashReporting.afterEvaluate(
                        p -> {
                            variantInputModel.getSourceSetManager().runBuildableArtifactsActions();

                            configuratorService.recordBlock(
                                    ExecutionType.BASE_PLUGIN_CREATE_ANDROID_TASKS,
                                    project.getPath(),
                                    null,
                                    this::createAndroidTasks);
                        }));
    }
```

```java
final void createAndroidTasks() {
 ...
     //编译配置
         configuratorService.getProjectBuilder(project.getPath())
                .setCompileSdk(extension.getCompileSdkVersion())
                .setBuildToolsVersion(extension.getBuildToolsRevision().toString())
                .setSplits(AnalyticsUtil.toProto(extension.getSplits()));

     //kotlin
        String kotlinPluginVersion = getKotlinPluginVersion();
        if (kotlinPluginVersion != null) {
            configuratorService.getProjectBuilder(project.getPath())
                    .setKotlinPluginVersion(kotlinPluginVersion);
        }
        AnalyticsUtil.recordFirebasePerformancePluginVersion(project);

        // create the build feature object that will be re-used everywhere
     //扩展功能 viewBinding  dataBinding
        BuildFeatureValues buildFeatureValues =
                variantFactory.createBuildFeatureValues(
                        extension.getBuildFeatures(), projectServices.getProjectOptions());

     	//创建变体
        variantManager.createVariants(buildFeatureValues, extension.getNamespace());

        List<ComponentInfo<VariantBuilderT, VariantT>> variants =
                variantManager.getMainComponents();

     //创建Task
        TaskManager<VariantBuilderT, VariantT> taskManager =
                createTaskManager(
                        variants,
                        variantManager.getTestComponents(),
                        !variantInputModel.getProductFlavors().isEmpty(),
                        globalScope,
                        extension);

        taskManager.createTasks(variantFactory.getVariantType(), buildFeatureValues);
     
 }
```

```java
  public class AppPlugin
        extends AbstractAppPlugin<
                ApplicationAndroidComponentsExtension,
                ApplicationVariantBuilderImpl,
                ApplicationVariantImpl> {
                    ....
    @Inject
  @NonNull
    @Override //app Task 任务
    protected ApplicationTaskManager createTaskManager(
            @NonNull
                    List<ComponentInfo<ApplicationVariantBuilderImpl, ApplicationVariantImpl>>
                            variants,
            @NonNull
                    List<ComponentInfo<TestComponentBuilderImpl, TestComponentImpl>> testComponents,
            boolean hasFlavors,
            @NonNull GlobalScope globalScope,
            @NonNull BaseExtension extension) {
        return new ApplicationTaskManager(
                variants, testComponents, hasFlavors, globalScope, extension);
    }
                    ...
    }
```



创建通用Task任务

```groovy
protected void createCommonTasks(
            @NonNull ComponentInfo<VariantBuilderT, VariantT> variant,
            @NonNull
                    List<? extends ComponentInfo<VariantBuilderT, VariantT>>
                            allComponentsWithLint) {
        VariantT appVariantProperties = variant.getVariant();
        ApkCreationConfig apkCreationConfig = (ApkCreationConfig) appVariantProperties;

        createAnchorTasks(appVariantProperties);

        taskFactory.register(new ExtractDeepLinksTask.CreationAction(appVariantProperties));

        // Create all current streams (dependencies mostly at this point) 依赖包
        createDependencyStreams(appVariantProperties);

        // Add a task to publish the applicationId.
        // TODO remove case once TaskManager's type param is based on BaseCreationConfig
        createApplicationIdWriterTask(apkCreationConfig);

        // Add a task to check the manifest
        taskFactory.register(new CheckManifest.CreationAction(appVariantProperties));

        // Add a task to process the manifest(s)//合并APK 清单
        createMergeApkManifestsTask(appVariantProperties);

        // Add a task to create the res values
        createGenerateResValuesTask(appVariantProperties);

        // Add a task to compile renderscript files.
        createRenderscriptTask(appVariantProperties);

        // Add a task to merge the resource folders //资源文件
        createMergeResourcesTasks(appVariantProperties);

        // Add tasks to compile shader
        createShaderTask(appVariantProperties);

        // Add a task to merge the asset folders
        createMergeAssetsTask(appVariantProperties);

        taskFactory.register(new CompressAssetsTask.CreationAction(apkCreationConfig));

        // Add a task to create the BuildConfig class
        createBuildConfigTask(appVariantProperties);

        // Add a task to process the Android Resources and generate source files
        createApkProcessResTask(appVariantProperties);

        registerRClassTransformStream(appVariantProperties);

        // Add a task to process the java resources
        createProcessJavaResTask(appVariantProperties);

        createAidlTask(appVariantProperties);

        // Add external native build tasks
        createExternalNativeBuildTasks(appVariantProperties);

        maybeExtractProfilerDependencies(apkCreationConfig);

        // Add a task to merge the jni libs folders
        createMergeJniLibFoldersTasks(appVariantProperties);

        // Add data binding tasks if enabled
        createDataBindingTasksIfNecessary(appVariantProperties);

        // Add a task to auto-generate classes for ML model files.
        createMlkitTask(appVariantProperties);

        // Add a compile task --- 会创建dex Task
        createCompileTask(appVariantProperties);

        taskFactory.register(new StripDebugSymbolsTask.CreationAction(appVariantProperties));

        taskFactory.register(
                new ExtractNativeDebugMetadataTask.FullCreationAction(appVariantProperties));
        taskFactory.register(
                new ExtractNativeDebugMetadataTask.SymbolTableCreationAction(appVariantProperties));

        createPackagingTask(apkCreationConfig);

        maybeCreateLintVitalTask(appVariantProperties, allComponentsWithLint);

        // Create the lint tasks, if enabled
        createLintTasks(appVariantProperties, allComponentsWithLint);

        taskFactory.register(
                new PackagedDependenciesWriterTask.CreationAction(appVariantProperties));

        taskFactory.register(new ApkZipPackagingTask.CreationAction(appVariantProperties));
    }
```







```java
private void createCompileTask(@NonNull VariantImpl variant) {
    ApkCreationConfig apkCreationConfig = (ApkCreationConfig) variant;

    TaskProvider<? extends JavaCompile> javacTask = createJavacTask(variant);
    addJavacClassesStream(variant);
    setJavaCompilerTask(javacTask, variant);
    createPostCompilationTasks(apkCreationConfig);
}
```



重要方法：先走transform  后有 dex

```java
  fun createPostCompilationTasks(creationConfig: ApkCreationConfig) {
  		..............
            
        // ----- External Transforms -----
        // apply all the external transforms.
        val customTransforms = extension.transforms//这里就是插件注册transforms
        val customTransformsDependencies = extension.transformsDependencies
        var registeredLegacyTransform = false
        var i = 0
        while (i < customTransforms.size) {
            val transform = customTransforms[i]
            val deps = customTransformsDependencies[i]
            registeredLegacyTransform = registeredLegacyTransform or transformManager
                    .addTransform(
                            taskFactory,
                            creationConfig,
                            transform,
                            null,
                            object: TaskConfigAction<TransformTask> {
                                override fun configure(task: TransformTask) {
                                    if (deps.isNotEmpty()) {
                                        task.dependsOn(deps)
                                    }
                                }

                            },
                            object: TaskProviderCallback<TransformTask> {
                                override fun handleProvider(taskProvider: TaskProvider<TransformTask>) {
                                    // if the task is a no-op then we make assemble task depend
                                    // on it.
                                    if (transform.scopes.isEmpty()) {
                                        creationConfig
                                                .taskContainer
                                                .assembleTask.dependsOn<Task>(taskProvider)
                                    }                                }

                            }
                    )
                    .isPresent
            i++
        }

        // Add a task to create merged runtime classes if this is a dynamic-feature,
        // or a base module consuming feature jars. Merged runtime classes are needed if code
        // minification is enabled in a project with features or dynamic-features.
        if (creationConfig.variantType.isDynamicFeature
                || variantScope.consumesFeatureJars()) {
            taskFactory.register(MergeClassesTask.CreationAction(creationConfig))
        }
      	...............
            
         //组装DEX文件
   		createDexTasks(creationConfig, dexingType, registeredLegacyTransform)
        maybeCreateResourcesShrinkerTasks(creationConfig)
        maybeCreateDexSplitterTask(creationConfig)
  }
```



任务真实创建：

eg：

![image-20211214171526113](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112141715303.png)

![image-20211214171604919](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112141716978.png)

![image-20211214171729432](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112141717530.png)

![image-20211214171806499](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112141718592.png)



![image-20211214171957613](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112141719725.png)