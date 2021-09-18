```java
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class AsmPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        System.out.println("================ AsmPlugin =============");
        
        //添加Task任务
        project.task("taskTestSundu").doFirst(new Action<Task>() {
            @Override
            public void execute(Task task) {
                System.out.println("我是AsmPlugin 测试任务");
            }
        });
        
        //添加Task任务
 		project.getTasks().register("sundu", new Action<Task>() {
            @Override
            public void execute(Task task) {

            }
        });
```

```java

android gradle 插件代码
BasePlugin
    --apply()
    	|--AppPlugin
    		|-- creatTaskManager 
    			|-- new ApplicationTaskManager(
                variants, testComponents, hasFlavors, globalScope, extension)

    TaskManager
       |--AbstractAppTaskManager
    	|--ApplicationTaskManager
    		|--createCommonTasks 创建常用的task 任务
    
    实际调用 taskContainer.regist
     override fun register(name: String): TaskProvider<Task> = taskContainer.register(name)
    
```

### 基于android 3.1.3 插件版本

```


implementation 'com.android.tools.build:gradle:3.1.3'
```

```java

TASK 任务扩展类
public interface TaskInternal extends Task, Configurable<Task> {
    @Internal
    List<ContextAwareTaskAction> getTaskActions();

    @Internal
    boolean hasTaskActions();

    @Internal
    Spec<? super TaskInternal> getOnlyIf();

    /** @deprecated */
    @Deprecated
    void execute();

    @Internal
    StandardOutputCapture getStandardOutputCapture();

    /** @deprecated */
    @Deprecated
    @Internal
    TaskExecuter getExecuter();

    /** @deprecated */
    @Deprecated
    void setExecuter(TaskExecuter var1);

    TaskInputsInternal getInputs();

    TaskOutputsInternal getOutputs();

    /** @deprecated */
    @Deprecated
    @Internal
    List<TaskValidator> getValidators();

    /** @deprecated */
    @Deprecated
    void addValidator(TaskValidator var1);

    TaskStateInternal getState();

    @Internal
    boolean getImpliesSubProjects();

    void setImpliesSubProjects(boolean var1);

    @Internal
    Factory<File> getTemporaryDirFactory();

    void prependParallelSafeAction(Action<? super Task> var1);

    void appendParallelSafeAction(Action<? super Task> var1);

    @Internal
    boolean isHasCustomActions();

    @Internal
    Path getIdentityPath();
}

android 插件的一个继承关系 
    
StreamBasedTask --AndroidBuilderTask -- AndroidVariantTask -DefaultTask-AbstractTask
public abstract class AndroidVariantTask extends DefaultTask {

    @Nullable
    private String variantName;

    @Nullable
    @Internal("No influence on output, this is for our build stats reporting mechanism")
    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(@Nullable String variantName) {
        this.variantName = variantName;
    }
}



public class TransformTask extends StreamBasedTask implements Context {

    private Transform transform;
    private Recorder recorder;
    Collection<SecondaryFile> secondaryFiles = null;
    List<FileCollection> secondaryInputFiles = null;
    @NonNull private final WorkerExecutor workerExecutor;

    public Transform getTransform() {
        return transform;
    }
}

//添加transform
public class TransformManager extends FilterableStreamCollection {
    @NonNull
    public <T extends Transform> Optional<TransformTask> addTransform(
            @NonNull TaskFactory taskFactory,
            @NonNull TransformVariantScope scope,
            @NonNull T transform,
            @Nullable TransformTask.ConfigActionCallback<T> callback) {

        if (!validateTransform(transform)) {
            // validate either throws an exception, or records the problem during sync
            // so it's safe to just return null here.
            return Optional.empty();
        }

        List<TransformStream> inputStreams = Lists.newArrayList();
        String taskName = scope.getTaskName(getTaskNamePrefix(transform));

        // get referenced-only streams
        List<TransformStream> referencedStreams = grabReferencedStreams(transform);

        // find input streams, and compute output streams for the transform.
        IntermediateStream outputStream = findTransformStreams(
                transform,
                scope,
                inputStreams,
                taskName,
                scope.getGlobalScope().getBuildDir());

        if (inputStreams.isEmpty() && referencedStreams.isEmpty()) {
            // didn't find any match. Means there is a broken order somewhere in the streams.
            issueReporter.reportError(
                    Type.GENERIC,
                    String.format(
                            "Unable to add Transform '%s' on variant '%s': requested streams not available: %s+%s / %s",
                            transform.getName(),
                            scope.getFullVariantName(),
                            transform.getScopes(),
                            transform.getReferencedScopes(),
                            transform.getInputTypes()));
            return Optional.empty();
        }

        //noinspection PointlessBooleanExpression
        if (DEBUG && logger.isEnabled(LogLevel.DEBUG)) {
            logger.debug("ADDED TRANSFORM(" + scope.getFullVariantName() + "):");
            logger.debug("\tName: " + transform.getName());
            logger.debug("\tTask: " + taskName);
            for (TransformStream sd : inputStreams) {
                logger.debug("\tInputStream: " + sd);
            }
            for (TransformStream sd : referencedStreams) {
                logger.debug("\tRef'edStream: " + sd);
            }
            if (outputStream != null) {
                logger.debug("\tOutputStream: " + outputStream);
            }
        }

        transforms.add(transform);

        // create the task...
        TransformTask task =
                taskFactory.create(
                        new TransformTask.ConfigAction<>(
                                scope.getFullVariantName(),
                                taskName,
                                transform,
                                inputStreams,
                                referencedStreams,
                                outputStream,
                                recorder,
                                callback));

        return Optional.ofNullable(task);
    }

}

TaskManager{
    
     public void createPostCompilationTasks( @NonNull final VariantScope variantScope) {
         
         createNewDexTasks()
     }
    
    //添加DEX生成的tranfrom
    private void createNewDexTasks(
            @NonNull VariantScope variantScope,
            @Nullable TransformTask multiDexClassListTask,
            @NonNull DexingType dexingType) {
        TransformManager transformManager = variantScope.getTransformManager();

        DefaultDexOptions dexOptions;
        if (variantScope.getVariantData().getType().isForTesting()) {
            // Don't use custom dx flags when compiling the test FULL_APK. They can break the test FULL_APK,
            // like --minimal-main-dex.
            dexOptions = DefaultDexOptions.copyOf(extension.getDexOptions());
            dexOptions.setAdditionalParameters(ImmutableList.of());
        } else {
            dexOptions = extension.getDexOptions();
        }

        boolean minified = runJavaCodeShrinker(variantScope);
        FileCache userLevelCache = getUserDexCache(minified, dexOptions.getPreDexLibraries());
        DexArchiveBuilderTransform preDexTransform =
                new DexArchiveBuilderTransformBuilder()
                        .setAndroidJarClasspath(
                                () ->
                                        variantScope
                                                .getGlobalScope()
                                                .getAndroidBuilder()
                                                .getBootClasspath(false))
                        .setDexOptions(dexOptions)
                        .setMessageReceiver(variantScope.getGlobalScope().getMessageReceiver())
                        .setUserLevelCache(userLevelCache)
                        .setMinSdkVersion(variantScope.getMinSdkVersion().getFeatureLevel())
                        .setDexer(variantScope.getDexer())
                        .setUseGradleWorkers(
                                projectOptions.get(BooleanOption.ENABLE_GRADLE_WORKERS))
                        .setInBufferSize(projectOptions.get(IntegerOption.DEXING_READ_BUFFER_SIZE))
                        .setOutBufferSize(
                                projectOptions.get(IntegerOption.DEXING_WRITE_BUFFER_SIZE))
                        .setIsDebuggable(
                                variantScope
                                        .getVariantConfiguration()
                                        .getBuildType()
                                        .isDebuggable())
                        .setJava8LangSupportType(variantScope.getJava8LangSupportType())
                        .setEnableIncrementalDesugaring(
                                projectOptions.get(BooleanOption.ENABLE_INCREMENTAL_DESUGARING))
                        .setProjectVariant(getProjectVariantId(variantScope))
                        .createDexArchiveBuilderTransform();
        transformManager
                .addTransform(taskFactory, variantScope, preDexTransform)
                .ifPresent(variantScope::addColdSwapBuildTask);

        boolean isDebuggable = variantScope.getVariantConfiguration().getBuildType().isDebuggable();
        if (dexingType != DexingType.LEGACY_MULTIDEX
                && variantScope.getCodeShrinker() == null
                && extension.getTransforms().isEmpty()) {
            ExternalLibsMergerTransform externalLibsMergerTransform =
                    new ExternalLibsMergerTransform(
                            dexingType,
                            variantScope.getDexMerger(),
                            variantScope.getMinSdkVersion().getFeatureLevel(),
                            isDebuggable,
                            variantScope.getGlobalScope().getMessageReceiver(),
                            DexMergerTransformCallable::new);

            transformManager.addTransform(taskFactory, variantScope, externalLibsMergerTransform);
        }

        DexMergerTransform dexTransform =
                new DexMergerTransform(
                        dexingType,
                        dexingType == DexingType.LEGACY_MULTIDEX
                                ? project.files(variantScope.getMainDexListFile())
                                : null,
                        variantScope.getGlobalScope().getMessageReceiver(),
                        variantScope.getDexMerger(),
                        variantScope.getMinSdkVersion().getFeatureLevel(),
                        isDebuggable);
        Optional<TransformTask> dexTask =
                transformManager.addTransform(taskFactory, variantScope, dexTransform);
        // need to manually make dex task depend on MultiDexTransform since there's no stream
        // consumption making this automatic
        dexTask.ifPresent(
                t -> {
                    if (multiDexClassListTask != null) {
                        t.dependsOn(multiDexClassListTask);
                    }
                    variantScope.addColdSwapBuildTask(t);
                });
    }
}

//生成DEX文件
public class DexArchiveBuilderTransform extends Transform {

 public void transform(@NonNull TransformInvocation transformInvocation)
            throws TransformException, IOException, InterruptedException {
      convertToDexArchive(
                            transformInvocation.getContext(),
                            dirInput,
                            outputProvider,
                            isIncremental,
                            classFileProviderFactory,
                            bootclasspath,
                            classpath,
                            additionalPaths);
 }
    
    private List<File> convertToDexArchive(
            @NonNull Context context,
            @NonNull QualifiedContent input,
            @NonNull TransformOutputProvider outputProvider,
            boolean isIncremental,
            @NonNull ClassFileProviderFactory classFileProviderFactory,
            @NonNull List<String> bootClasspath,
            @NonNull List<String> classpath,
            @NonNull Set<File> additionalPaths)
            throws Exception {

        logger.verbose("Dexing %s", input.getFile().getAbsolutePath());

        ImmutableList.Builder<File> dexArchives = ImmutableList.builder();
        for (int bucketId = 0; bucketId < NUMBER_OF_BUCKETS; bucketId++) {

            File preDexOutputFile = getPreDexFile(outputProvider, input, bucketId);
            dexArchives.add(preDexOutputFile);
            DexConversionParameters parameters =
                    new DexConversionParameters(
                            input,
                            bootClasspath,
                            classpath,
                            preDexOutputFile,
                            NUMBER_OF_BUCKETS,
                            bucketId,
                            minSdkVersion,
                            dexOptions.getAdditionalParameters(),
                            inBufferSize,
                            outBufferSize,
                            dexer,
                            isDebuggable,
                            isIncremental,
                            classFileProviderFactory,
                            java8LangSupportType,
                            additionalPaths,
                            new SerializableMessageReceiver(messageReceiver));

            if (useGradleWorkers) {
                context.getWorkerExecutor()
                        .submit(
                                DexConversionWorkAction.class,
                                configuration -> {
                                    configuration.setIsolationMode(IsolationMode.NONE);
                                    configuration.setParams(parameters);
                                });
            } else {
                executor.execute(
                        () -> {
                            ProcessOutputHandler outputHandler =
                                    new ParsingProcessOutputHandler(
                                            new ToolOutputParser(
                                                    new DexParser(), Message.Kind.ERROR, logger),
                                            new ToolOutputParser(new DexParser(), logger),
                                            messageReceiver);
                            ProcessOutput output = null;
                            try (Closeable ignored = output = outputHandler.createOutput()) {
                                //生成DEX文件
                                launchProcessing(
                                        parameters,
                                        output.getStandardOutput(),
                                        output.getErrorOutput(),
                                        messageReceiver);
                            } finally {
                                if (output != null) {
                                    try {
                                        outputHandler.handleOutput(output);
                                    } catch (ProcessException e) {
                                        // ignore this one
                                    }
                                }
                            }
                            return null;
                        });
            }
        }
        return dexArchives.build();
    }
    
    
      private static void launchProcessing(
            @NonNull DexConversionParameters dexConversionParameters,
            @NonNull OutputStream outStream,
            @NonNull OutputStream errStream,
            @NonNull MessageReceiver receiver)
            throws IOException, URISyntaxException {
        DexArchiveBuilder dexArchiveBuilder =
                getDexArchiveBuilder(
                        dexConversionParameters.minSdkVersion,
                        dexConversionParameters.dexAdditionalParameters,
                        dexConversionParameters.inBufferSize,
                        dexConversionParameters.outBufferSize,
                        dexConversionParameters.bootClasspath,
                        dexConversionParameters.classpath,
                        dexConversionParameters.dexer,
                        dexConversionParameters.isDebuggable,
                        dexConversionParameters.classFileProviderFactory,
                        VariantScope.Java8LangSupport.D8
                                == dexConversionParameters.java8LangSupportType,
                        outStream,
                        errStream,
                        receiver);

        Path inputPath = dexConversionParameters.input.getFile().toPath();
        Predicate<String> bucketFilter = dexConversionParameters::belongsToThisBucket;

        boolean hasIncrementalInfo =
                dexConversionParameters.isDirectoryBased() && dexConversionParameters.isIncremental;
        Predicate<String> toProcess =
                hasIncrementalInfo
                        ? path -> {
                            File resolved = inputPath.resolve(path).toFile();
                            if (dexConversionParameters.additionalPaths.contains(resolved)) {
                                return true;
                            }
                            Map<File, Status> changedFiles =
                                    ((DirectoryInput) dexConversionParameters.input)
                                            .getChangedFiles();

                            Status status = changedFiles.get(resolved);
                            return status == Status.ADDED || status == Status.CHANGED;
                        }
                        : path -> true;

        bucketFilter = bucketFilter.and(toProcess);

        logger.verbose("Dexing '" + inputPath + "' to '" + dexConversionParameters.output + "'");

        try (ClassFileInput input = ClassFileInputs.fromPath(inputPath)) {
            //生成dex文件
            dexArchiveBuilder.convert(
                    input.entries(bucketFilter),
                    Paths.get(new URI(dexConversionParameters.output)),
                    dexConversionParameters.isDirectoryBased());
        } catch (DexArchiveBuilderException ex) {
            throw new DexArchiveBuilderException("Failed to process " + inputPath.toString(), ex);
        }
    }
}
//执行生成dex文件操作
class DxDexArchiveBuilder extends DexArchiveBuilder {
    public void convert(
            @NonNull Stream<ClassFileEntry> input, @NonNull Path output, boolean isIncremental)
            throws DexArchiveBuilderException {
         dex(classFileEntry.getRelativePath(), byteArray, outputDexArchive);
    }
    //创建dex文件
    public void dex(String relativePath, ByteArray classBytes, DexArchive output)
            throws IOException {

        // Copied from dx, from com.android.dx.command.dexer.Main
        DirectClassFile cf = new DirectClassFile(classBytes, relativePath, true);
        cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        cf.getMagic(); // triggers the actual parsing

        // starts the actual translation and writes the content to the dex file
        // specified
        DexFile dexFile = new DexFile(config.getDexOptions());

        // Copied from dx, from com.android.dx.command.dexer.Main
        ClassDefItem classDefItem =
                CfTranslator.translate(
                        config.getDxContext(),
                        cf,
                        null,
                        config.getCfOptions(),
                        config.getDexOptions(),
                        dexFile);
        dexFile.add(classDefItem);

        if (outStorage != null) {
            ByteArrayAnnotatedOutput byteArrayAnnotatedOutput = dexFile.writeTo(outStorage);
            output.addFile(
                    ClassFileEntry.withDexExtension(relativePath),
                    byteArrayAnnotatedOutput.getArray(),
                    0,
                    byteArrayAnnotatedOutput.getCursor());
        } else {
            byte[] bytes = dexFile.toDex(null, false);
            output.addFile(ClassFileEntry.withDexExtension(relativePath), bytes, 0, bytes.length);
        }
    }
    
}

```

android 插件添加Trask任务

![1631183506302](.art/ASM%E5%AD%97%E8%8A%82%E7%A0%81%E6%8F%92%E6%A1%A9/1631183506302.png)

### 基于插件com.android.tools.build:gradle:4.2.2

```
implementation "com.android.tools.build:gradle:4.2.2"
```

debug 和 release 都无法找到 class 转换为dex文件的任务，