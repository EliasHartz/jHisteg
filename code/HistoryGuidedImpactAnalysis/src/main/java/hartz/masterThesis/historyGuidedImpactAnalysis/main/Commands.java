package hartz.masterThesis.historyGuidedImpactAnalysis.main;

import java.io.PrintStream;

/**
 * Defines and explains how to use this program.
 */
public class Commands {
    //execution commands
    public static final String extractOnly = "extract";
    public static final String compile = "compile";
    public static final String instrument = "instrument";
    public static final String analyze = "analyze";
    public static final String compute = "compute";

    //supplying target version commands
    public static final String versionsShort = "-v";
    public static final String versions = "-versions";
    public static final String allVersions = "-allVersions";

    //repository commands
    public static final String clone = "-clone";
    public static final String cloneShort = "-cl";
    public static final String pathToLocalRepo = "-repo";
    public static final String pathToLocalRepoShort = "-r";
    public static final String branchToWorkOn = "-branch";
    public static final String nameOfRepo = "-name";
    public static final String nameOfRepoShort = "-n";

    //output and storage commands
    public static final String outputFolder = "-out";
    public static final String outputFolderShort = "-o";
    public static final String keepRepo = "--keepRepo";
    public static final String keepRepoShort = "--kr";
    public static final String forceOverwrite = "--override";
    public static final String forceOverwrite2 = "--overwrite";
    public static final String forceOverwriteShort = "--ow";
    public static final String forceOverwriteShort2 = "--over";
    public static final String coverRenaming = "--coverRenamed";
    public static final String coverRenamingShort = "--cr";

    //providing static resources
    public static final String relativePathToSources = "-sources";
    public static final String relativePathToSourcesShort = "-s";
    public static final String relativePathToCompiledSources = "-compiled";
    public static final String relativePathToCompiledSourcesShort = "-c";
    public static final String relativePathToAdditionalCompiledSources = "-also";
    public static final String buildFileShort = "-bs";
    public static final String buildSpot = "-buildSpot";
    public static final String buildCommandShort = "-bc";
    public static final String buildCommand = "-buildCmd";
    public static final String cleanBeforeCompile = "--cleanBuild";

    //controlling instrumentation
    public static final String excludeDirFromInstrumentation = "-excludeDir";
    public static final String excludeDirFromInstrumentationShort = "-exdir";
    public static final String excludeMethodFromInstrumentation = "-excludeMethod";
    public static final String excludeMethodFromInstrumentationShort = "-exmeth";
    public static final String ignoreSuffix = "-ignoreBySuffix";
    public static final String ignoreSuffixShort = "-igsuf";
    public static final String ignorePrefix = "-ignoreByPrefix";
    public static final String ignorePrefixShort = "-igpre";
    public static final String doNotExportSource = "--doNotExportBytecode";
    public static final String injectTwice = "--injectFuncIntoAdditional";

    //providing dynamic resources
    public static final String testsDir = "-tests";
    public static final String testsDirShort = "-t";
    public static final String executeTests = "-runTests";
    public static final String skipTests = "-skipTests";
    public static final String executeTestsShort = "-rt";
    public static final String executeMain = "-runMain";
    public static final String executeMainShort = "-rm";
    public static final String executeMainArgs = "-runMainArgs";
    public static final String executeMainArgsShort = "-rmarg";
    public static final String runCommand = "-runCommand";
    public static final String runCommandShort = "-rc";
    public static final String coverageBasedTraces = "--coverageBased";
    public static final String distanceBasedTraces = "--distanceBased";
    public static final String compatibleInstr ="--saferInstrumentation";
    public static final String keepTraces =    "--keepTraces";
    public static final String keepTracesShort = "--kt";
    public static final String flatDivergences = "--exportFlat";
    public static final String filterObjectCharacteristics ="--filterObjects";


    //misc
    public static final String loud = "--loud";
    public static final String toolOut = "--toolOutput";
    public static final String quiet = "--quiet";
    public static final String generateTargetsForSyntax = "--generateForSyntax";
    public static final String generateTargetsForDivergences = "--generateForDivergence";
    public static final String restrictSyntaxMapping = "--restrictSyntaxMapping";
    public static final String useManualTraceMatchFile = "--manualTraceMatch";
    public static final String noTargetDetails = "--noTargetDetails";

    //Undocumented flags that are not to be used by others!
    public static final String debug = "--debug"; //will cause more output (ignores quiet-command)
    public static final String skipVersionCheck = "--skipVersionCheck"; //will execute on version regardless of compatibility!
    public static final String noRepo = "--noRepo"; //will skip extraction, the output directory serves as ground truth!

    /**
     * Explains the usage of the command line and exits the program afterwards.
     */
    public static void showHelp(PrintStream s) {
        s.println(
                "\n"+
                "This tool implements functionality to extract specific versions of a project under a VCS\n"+
                "to generate 'testing targets' which point to parts of the code that should most likely\n"+
                "be checked/tested again because their behavior has been modified. This information is\n"+
                "obtained by combining a static syntax comparison with actual execution data in the form\n"+
                "of observed traces through the code of the project. Both sources and compiled sources\n"+
                "must be available for this process. \n\n"+

                "*** AVAILABLE MODES OF EXECUTION: ***\n\n"+
                        "    '"+Commands.extractOnly+"' = the tool will only extract and persist specified versions from\n"+
                        "                the repository. No compilation, instrumentation or testing target\n"+
                        "                computation will be performed, versions are extracted as-is. Use\n"+
                        "                this command if you just want to inspect a code base or perform\n"+
                        "                later steps, e.g. compilation, by external means.\n\n"+
                        "    '"+Commands.compile+"' = the tool will extract and subsequently compile specified\n"+
                        "                versions using the provided resources for execution (see\n"+
                        "                below). This code will *NOT* be not instrumented, it will\n"+
                        "                produce exactly the code that belongs to the corresponding\n"+
                        "                version, provided that code is compilable at all. Use this\n"+
                        "                if you require runnable versions of your code base but the\n"+
                        "                rest of this tool's functionality is of no interest to you.\n\n"+
                        "    '"+Commands.instrument+"' = in addition to the above steps, the instrumentation step\n"+
                        "                   will be performed. The compiled code from the\n"+
                        "                   previous step will be altered in-place, meaning\n"+
                        "                   that your project will behave differently than\n"+
                        "                   the original extracted and compiled version. The\n"+
                        "                   difference is that your code will now monitor its\n"+
                        "                   own execution and persist it as execution traces\n"+
                        "                   data on your drive.\n"+
                        "                   *NOTE*: Consequently, you can now generate traces\n"+
                        "                   by simply executing your program or a testsuite on\n"+
                        "                   this program. Traces are required by the final step!\n\n"+
                        "    '"+Commands.analyze +"' = Your code will be extracted, compiled and instrumented.\n"+
                        "                Afterwards, syntax analysis is performed and traces are\n"+
                        "                generated (if corresponding commands have been provided).\n\n"+
                        "    '"+Commands.compute +"' = Your code will be extracted, compiled and instrumented.\n"+
                        "                Afterwards, testing targets are computed by combining syntax\n"+
                        "                changes with present execution trace data (which is generated\n"+
                        "                if corresponding commands have been provided). Using this mode\n"+
                        "                can be interpreted as calling the tool's full functionality!\n\n"+

                        "*** PARAMETERS AND FLAGS: ***\n\n"+
                        "  ## OBTAINING DATA FROM REPOSITORIES: ##\n"+
                        "    '"+Commands.clone+" <link>' = tells this tool that the repository to mine has to be downloaded\n"+
                        "                      first, the parameter <link> is the corresponding link to that \n"+
                        "                      repository (read-only access is sufficient), which could be e.g.\n"+
                        "                      'git@git.assembla.com:test.git' to check out a repo called 'test'\n"+
                        "                      from assembla using Git's SSH protocol. Supported repository types\n"+
                        "                      are Git and SVN.\n"+
                        "                      A repo type can be enforced by adding the prefix !git! or !svn! to\n"+
                        "                      the provided address but normally, the type can be inferred!\n"+
                        "                      => short: '"+Commands.cloneShort+" <repo>'\n\n"+
                        "    '"+Commands.pathToLocalRepo+" <directory>' = instructs the program to use an existing repository directory\n"+
                        "                          on your harddrive in its *current* status. Very useful command\n"+
                        "                          for Git repositories since they support extracting versions\n"+
                        "                          without connecting any remote. Cannot be used together with\n"+
                        "                          '"+Commands.clone+"' but can obviously work on a repository created in a\n"+
                        "                          previous run using the '"+Commands.clone+" and "+Commands.keepRepo+"' commands.\n"+
                        "                          => short: '"+Commands.pathToLocalRepoShort+" <directory>'\n\n"+
                        "    '"+Commands.nameOfRepo+" <name>' = OPTIONAL, sets a name for the repository to be downloaded. Cannot\n"+
                        "                     be used together with '"+Commands.pathToLocalRepo+"'. *NOTE* that if not present, a\n"+
                        "                     placeholder directory name will be used for extracted versions from\n"+
                        "                     the repository.\n"+
                        "                     => short: '"+Commands.nameOfRepoShort+" <name>'\n\n"+
                        "    '"+Commands.keepRepo+"' = instructs the program to keep a functional version of the repository\n"+
                        "                   that was downloaded and put it into the output directory. *NOTE* that\n"+
                        "                   this command will be *ignored* if '"+Commands.clone+"' is not used.\n"+
                        "                   => short: '"+Commands.keepRepoShort+"'\n\n"+
                        "    '"+Commands.branchToWorkOn+" <name>' = specifies the branch that is to be used for extracting versions,\n"+
                        "                       the default value is 'master'. *NOTE* that this is currently only\n"+
                        "                       supported for Git repositories and is being ignored otherwise.\n\n\n"+
                        "    '"+Commands.versions+" [<identifier>,...]' = Use this parameter to specify the versions of the\n"+
                        "                                     repository that are to be compared against each other.\n"+
                        "                                     Versions are defined by their 'identifiers', e.g. a\n"+
                        "                                     hash for a Git repository or a revision number for\n"+
                        "                                     SVN. Your list should at least contain two entries if\n"+
                        "                                     you want to compute changes between versions of course!\n"+
                        "                                     As a *convenience* you may also just use one identifier\n"+
                        "                                     instead of the list, this causes the tool to perform\n"+
                        "                                     the requested operation based on the given version and\n"+
                        "                                     its immediate predecessor, which is automatically\n"+
                        "                                     selected using the repo's history.\n"+
                        "                                     => short: '"+Commands.versionsShort+" [<identifier>,...]'\n\n"+
                        "    '"+Commands.allVersions+"' = This command executes the '"+Commands.versions+"' sequentially on all versions\n"+
                        "                     inside the repository's history.\n\n"+

                        "  ## PROVIDING STATIC RESOURCES FOR TOOL EXECUTION: ##\n\n"+

                        "    To properly function, you must provide the tool with a variety of resources. Both compiled\n"+
                        "    and uncompiled sources must be available for each of your extracted versions.\n"+
                        "    *NOTE*: Each of the flags in this category is explained with a list as argument,\n"+
                        "    each entry being matched to the corresponding version provided by '"+Commands.versions+"'.\n"+
                        "    However, most of the time you actually do not have to, since the argument is\n"+
                        "    identical for all versions, e.g. the folder containing your source files. To\n"+
                        "    conveniently apply one single argument to all versions you specified, it is\n"+
                        "    supported to simply give the data directly and the value will be applied to\n"+
                        "    all versions. Note that for commands like '"+Commands.ignoreSuffix+"', the\n"+
                        "    arguments '[abc]' and 'abc' would have the same effect. All versions would\n"+
                        "    be configured with the value 'abc'. However, the list to be duplicated may\n"+
                        "    have other sizes as well, hence '[abc,def]' would work as well while giving\n"+
                        "    two arguments would not!\n\n"+

                        "    '"+Commands.relativePathToSources+" [<rel. path>, ...]' = use this command to specify the classpaths of\n"+
                        "                                   the source files for the different versions. This\n"+
                        "                                   command expects paths relative to the repository's\n"+
                        "                                   main directory, so if your repository would be at\n"+
                        "                                   '/files/repo' and your sources at '/files/repo/src',\n"+
                        "                                   your relative path would be only 'src'.\n"+
                        "                                   *NOTE* that if no location is provided, the source\n"+
                        "                                   path defaults to the main repository directory,\n"+
                        "                                   (relative path '.') which is likely not the\n"+
                        "                                   correct classpath for most projects.\n"+
                        "                                   => short: '"+Commands.relativePathToSourcesShort+" [<rel. path>, ...]'\n\n"+
                        "    '"+Commands.relativePathToCompiledSources+" [<rel. path>, ...]' = use this command to specify the classpaths of\n"+
                        "                                     compiled sources, which must in be a directory (not \n"+
                        "                                     a JAR-file). If you need your source code compiled\n"+
                        "                                     first, build your project using '"+Commands.buildCommand+"'\n"+
                        "                                     Similar to '"+Commands.relativePathToSources+"' this command expects a list\n"+
                        "                                     of relative paths as its parameter and defaults\n"+
                        "                                     to the repositories main directory.\n"+
                        "                                     => short: '"+Commands.relativePathToCompiledSourcesShort+" [<rel. path>, ...]'\n\n"+
                        "    '"+Commands.buildCommand+" [<data>, ...]' = use this command to define how your (ANT or Maven) build\n"+
                        "                                process is to be invoked. When working with a Apache Maven\n"+
                        "                                based repository, you could for example choose to provide\n"+
                        "                                '"+Commands.buildCommand+" \"clean install\"' to build your project from\n"+
                        "                                The build-system's type (here Maven) can usually be\n"+
                        "                                inferred automatically but you can still enforce a one of\n"+
                        "                                the supported systems by beginning your provided command\n" +
                        "                                with either the string 'ant' or 'mvn'. You can use other\n"+
                        "                                build systems as well but those are unsupported, hence\n"+
                        "                                you will need to state the full command at all times!\n"+
                        "                                *NOTE THAT* this tool cannot determine reliably if the\n"+
                        "                                compiled sources present are adequate (aka match the\n"+
                        "                                source code of the version) and will therefore always\n"+
                        "                                compile the sources if you provide this command unless\n"+
                        "                                the code has already been *instrumented*. Since said\n"+
                        "                                instrumentation is only executed on adequate code, the\n"+
                        "                                compilation step can be safely skipped. Consequently, if\n"+
                        "                                you do not want to compile your sources for some reason,\n"+
                        "                                then do not provide the '"+Commands.buildCommand+"' command!\n"+
                        "                                *NOTE ADDITIONALLY* that your command of choice should\n"+
                        "                                ideally produce fresh sources, deleting previously present\n"+
                        "                                code. See the '"+Commands.cleanBeforeCompile+"' for more details!\n"+
                        "                                *NOTE FURTHERMORE* that your chosen command must produce\n"+
                        "                                all compiled sources, that is for both your project's code\n"+
                        "                                base and your tests if you intend to run or instrument\n"+
                        "                                tests in a later step.\n"+
                        "                                => short: '"+Commands.buildCommandShort+" [<data>, ...]'\n\n"+
                        "    '"+Commands.buildSpot +" [<rel. path>, ...]' = this command defines where the build process is\n"+
                        "                                      invoked, so if your ANT or Maven xml-file is not\n"+
                        "                                      located in the main directory of the repository,\n"+
                        "                                      you can use this command to provide the locations.\n"+
                        "                                      You can point directly to the file or simply specify\n"+
                        "                                      a directory. Similar to '"+Commands.relativePathToSources+"' it expects a\n"+
                        "                                      list of relative paths for each version as its\n"+
                        "                                      parameter. If you are using another build system, you\n"+
                        "                                      should always point to the directory where your\n"+
                        "                                      command given by '"+Commands.buildCommand+" is to be executed.\n"+
                        "                                      => short: '"+Commands.buildFileShort+" [<rel. path>, ...]'\n\n"+
                        "    '"+Commands.ignoreSuffix+" [[<string>, ...], ...]' = use this command to exclude files from\n"+
                        "                                               instrumentation by classname suffix.\n"+
                        "                                               The primary use-case of this command is to\n"+
                        "                                               exclude JUnit test classes or other\n"+
                        "                                               sections of code that should not be taken\n"+
                        "                                               into account by the coverage analysis. The\n"+
                        "                                               check on the classname is *case-sensitive*,\n"+
                        "                                               thus a typical value to use might be 'Test'.\n"+
                        "                                               Take care when using this command, it will\n"+
                        "                                               obviously have an impact on all observed\n"+
                        "                                               traces, ignoring too much will make it all\n"+
                        "                                               but impossible to obtain sensible results!\n"+
                        "                                               => short: '"+Commands.ignoreSuffixShort+" [[<string>, ...], ...]'\n\n"+
                        "    '"+Commands.ignorePrefix+" [[<string>, ...], ...]' = this command is similar to the one above but\n"+
                        "                                               it excludes classnames using prefixes instead\n"+
                        "                                               *NOTE* that in the event that both commands\n"+
                        "                                               are used, a class is ignored if it matches to\n"+
                        "                                               *any* of the two conditions!\n"+
                        "                                               => short: '"+Commands.ignorePrefixShort+" [[<string>, ...], ...]'\n\n"+
                        "    '"+Commands.excludeDirFromInstrumentation+" [[<string>, ...], ...]' = this command another variant of instrumentation\n"+
                        "                                           exclusion control, this one takes relative paths to\n"+
                        "                                           directories (check '"+Commands.relativePathToSources+"' command for details).\n"+
                        "                                           All files contained in matching directories are ignored.\n"+
                        "                                           If your e.g. your tests share the classpath with the rest\n"+
                        "                                           of your code, you might use this command to exclude their\n"+
                        "                                           packages.\n" +
                        "                                           => short: '"+Commands.excludeDirFromInstrumentationShort+" [[<string>, ...], ...]'\n\n"+
                        "    '"+Commands.relativePathToAdditionalCompiledSources+" [<rel. path>, ...]' = there are projects which have  more than one classpath, for example\n"+
                        "                                 to separate tests from actual source code. If you want these classes\n"+
                        "                                 instrumented as well, make use  of this command, which works in a\n"+
                        "                                 similar fashion as '"+Commands.relativePathToCompiledSources+"'.\n"+
                        "                                 *NOTE* that this is intended as an advanced option to control\n"+
                        "                                 instrumentation, not as a main feature. While the additional classes\n"+
                        "                                 will be back-upped and instrumented, they will NOT be included in\n"+
                        "                                 the version's call graph and there is no option to provide additional\n"+
                        "                                 uncompiled sources on purpose! A versions primary source code should\n"+
                        "                                 be under '"+Commands.relativePathToCompiledSources+"', that is its semantic!\n"+
                        "                                 => short: -------unavailable-------\n"+
                        "    '"+Commands.injectTwice+" [<rel. path>, ...]' = if '"+Commands.relativePathToAdditionalCompiledSources+" is used, you can furthermore provide\n"+
                        "                                                      this flag to inject the Coverage Observer\n"+
                        "                                                      functionality into the second classpath as well. \n"+
                        "                                                      Use this only if you face problems during trace\n"+
                        "                                                      generation, normally you do not need this!\n"+"" +
                        "                                                      Flag is ignored if no second classpath is given!\n"+
                        "                                                      => short: -------unavailable-------\n"+
                        "    '"+Commands.excludeMethodFromInstrumentation +" [[<string>, ...], ...]' = To avoid bloating your observed traces with data\n"+
                        "                                              generated by internal calls like the frequent calls\n"+
                        "                                              to 'hashCode()I' by a HashMap. You can ignore\n"+
                        "                                              specific methods in all classes with this command.\n"+
                        "                                              To ignore a method, you need to provide a method\n"+
                        "                                              header as defined by the JVM, give for instance\n"+
                        "                                              'main([Ljava/lang/String;)V' to ignore main-methods.\n"+
                        "                                              *NOTE* that in contrast to the two commands above,\n"+
                        "                                              this one has default value: Unless you do not provide\n"+
                        "                                              a list of your own, 'hashCode', 'equals' and 'toString'\n"+
                        "                                              will be ignored for all classes.\n"+
                        "                                              => short: '"+Commands.excludeMethodFromInstrumentationShort +" [[<string>, ...], ...]'\n"+
                        "    '"+Commands.compatibleInstr+"' = if this is provided, a safer and more compatible instrumentation method\n"+
                        "                               will be used. Resulting data is unaffected, use this if you have problems!\n"+
                        "                               => short: -------unavailable-------\n\n"+


                        "  ## GENERATING TRACES FOR CHANGE APPROXIMATION: ##\n\n"+

                        "    To properly function, you must provide the tool with data about concrete\n"+
                        "    executions of each of the versions, the so called traces. Once the code has\n"+
                        "    been instrumented, you are able to simply generate these traces by running\n"+
                        "    your program, which will then monitor its own execution and persist the\n"+
                        "    results on the drive. For convenience however, you may also use the\n"+
                        "    following commands instead of executing each version manually. Note that\n"+
                        "    if any of these execution commands are used, all present traces will be\n"+
                        "    overwritten (unless '"+Commands.keepTraces+"'is given) while manually creating a\n"+
                        "    trace will always add a new 'observedTrace' file to the version's directory.\n"+
                        "    Commands listed here also support the convenience list-wrap, similar to the\n"+
                        "    ones in the section above.\n\n"+

                        "    '"+Commands.testsDir +" [<rel. path>, ...]' = use this command to specify a directory containing\n"+
                        "                                  *COMPILED* JUnit tests for the different versions.\n"+
                        "                                  All tests found inside this directory will be executed!\n"+
                        "                                  The default value for this parameter is the value\n"+
                        "                                  of '"+Commands.relativePathToCompiledSources+"', in this case *NO* tests will be\n" +
                        "                                  executed unless you explicitly state which ones to\n"+
                        "                                  execute using '"+Commands.executeTests+" (see below).\n" +
                        "                                  *NOTE*: Running tests from inside this tool requires\n"+"" +
                        "                                  lots of memory! Remember, you can all generate traces\n"+
                        "                                  manually outside this tool once instrumentation has been\n"+
                        "                                  performed on a version!\n"+
                        "                                  => short: '"+Commands.testsDirShort +" [<rel. path>, ...]'\n\n"+
                        "    '"+Commands.executeTests+" [[<classn./path>, ...], ...]' = use this command to specify exactly which\n"+
                        "                                               JUnit tests are to be executed for each\n"+
                        "                                               version. You may use either fully qualified\n"+
                        "                                               class names or relative paths to directories\n"+
                        "                                               to execute all tests inside the that directory.\n"+
                        "                                               *NOTE* that whatever you intend to execute\n"+
                        "                                               must be inside a sub-directory of the value\n"+
                        "                                               of '"+Commands.testsDir +"', otherwise it will be ignored!\n"+
                        "                                               => short: '"+Commands.executeTestsShort+" [[<rel. path>, ...], ...]'\n\n"+
                        "    '"+Commands.skipTests+" [[<classname>, ...], ...]' = the opposite of '"+Commands.executeTests+", can be used\n"+
                        "                                             to exclude specific tests. Takes *ONLY*\n"+
                        "                                             fully qualified class names!\n"+
                        "                                             => short: -------unavailable-------\n\n"+
                        "    '"+Commands.executeMain+" [[<classname>, ...], ...]' = by providing fully qualified class names,\n"+
                        "                                           e.g. 'Package.MyClass', and this command,\n"+
                        "                                           you can also generate traces. It will be\n"+
                        "                                           attempted to execute the given class' main\n"+
                        "                                           method with the arguments provided by the\n"+
                        "                                           next command below.\n"+
                        "                                           => short: '"+Commands.executeMainShort +" [[<classname>, ...], ...]'\n\n"+
                        "    '"+Commands.executeMainArgs+" [[<data>, ...], ...]' = a supplementary command to '"+Commands.executeMain+"' to\n"+
                        "                                          provide the program arguments. It defaults\n"+
                        "                                          to an empty list, meaning no arguments are\n"+
                        "                                          provided. If '"+Commands.executeMain+"' is not used, this\n"+
                        "                                          command and its arguments will be ignored.\n"+
                        "                                          => short: '"+Commands.executeMainArgsShort +" [[<data>, ...], ...]'\n\n"+
                        "    '"+Commands.runCommand+" [[<data>, ...], ...]' = finally, you can also simply state a command,\n"+
                        "                                         similar to '"+Commands.buildCommand+"' that is to be executed.\n"+
                        "                                         The typical use case is to call a testing\n"+
                        "                                         target of your build system, e.g. 'ant test' if\n"+
                        "                                         that would be defined. *NOTE* that your command\n"+
                        "                                         MUST NOT recompile the source code, since the\n"+
                        "                                         instrumentation would be lost and no traces would\n"+
                        "                                         be generated. This tool not able to detect that\n"+
                        "                                         such an overwrite occurred, so be careful!\n"+
                        "                                         => short: '"+Commands.runCommandShort +" [[<data>, ...], ...]'\n\n"+
                        "    '"+Commands.coverageBasedTraces+"' = provide this flag to enable a coverage-based approach for\n"+
                        "                        comparison of execution traces. Instead of trying to\n"+
                        "                        determine at which points two traces start to differ,\n"+
                        "                        the coverage-based approach directly compares how often\n"+
                        "                        which bytecode instructions are executed. The order of\n"+
                        "                        resulting testing targets will therefore be affected\n"+
                        "                        stronger by quantity of behavioral difference while\n"+"" +
                        "                        loosing precision in regards to locating the factually\n"+
                        "                        different behavior.\n"+
                        "                        => short: -------unavailable-------\n\n"+
                        "    '"+Commands.distanceBasedTraces+"' = provide this flag to enable a distance-based approach for\n"+
                        "                        comparison of execution traces. Like '"+Commands.coverageBasedTraces+"',\n"+
                        "                        this causes the location of the different behavior to be \n"+
                        "                        deemphasized. This basically just runs the quantitative\n"+
                        "                        aspect of the normal trace comparison algorithm. This flag\n"+
                        "                        can be combined with '"+Commands.coverageBasedTraces+"' to include both\n"+
                        "                        quantitative metrics in the final computation.\n"+
                        "                        => short: -------unavailable-------\n\n"+
                        "    '"+Commands.keepTraces+"' = provide this flag to disable the purging of traces before creating\n"+
                        "                     new ones. Existing traces inside the version's directory will be taken\n"+
                        "                     into account by the testing target computation just like newly created\n"+
                        "                     ones and there is no duplicate checking! Use this flag with caution!\n"+
                        "                     => short: '"+Commands.keepTracesShort +"'\n\n"+

                        "  ## MISC FUNCTIONALITY: ##\n"+
                        "    '"+Commands.forceOverwrite+"' = if supplied, a run will overwrite existing data instead of\n"+
                        "                   skipping operations. Use this to ensure fresh data or if the\n"+
                        "                   integrity of existing data is uncertain. *NOTE* that this flag\n"+
                        "                   includes the effect of the '"+Commands.cleanBeforeCompile+"' flag!\n"+
                        "                   As such, make sure that your input is well-formed and points to\n"+
                        "                   the correct directories! In general, use with care!\n"+
                        "                   => short: '"+Commands.forceOverwriteShort+"'\n\n"+
                        "    '"+Commands.outputFolder+" <directory>' = specifies the output location for all extracted or generated\n"+
                        "                         data, standard behavior is creating a new folder in the\n"+
                        "                         execution directory of the tool (named 'output').\n"+
                        "                         => short: '"+Commands.outputFolderShort+" <directory>'\n\n"+
                        "    '"+Commands.coverRenaming+"' = causes the renaming of variables and files to be treated as syntax\n"+
                        "                       changes during the corresponding analysis phase. If this flag is not\n"+
                        "                       given, they are ignored. Renaming detection is *not sound*, so enable\n"+
                        "                       this feature if your result does not contain the data you expect.\n"+
                        "                       => short: '"+Commands.coverRenamingShort+"'\n"+
                        "    '"+Commands.cleanBeforeCompile+"' = if you provide this flag, the compiled sources directory \n"+
                        "                     will be deleted before any build command is executed. This\n"+
                        "                     may be necessary if your build command supplied through '"+Commands.buildCommand+"'\n"+
                        "                     does not produce a fresh compilation. As a result, already\n"+
                        "                     instrumented code might get re-instrumented, causing corrupted\n"+
                        "                     results and ultimately leading to 'to large method' exceptions\n"+
                        "                     as your code gets larger with every tool run! Be *careful* with\n"+
                        "                     this command, as it does delete a directory entered by you, the\n" +
                        "                     user! Make *SURE* your input is correct!\n"+
                        "                     If provided, the additional compiled source code directory is\n"+
                        "                     deleted as well.\n"+
                        "                     => short: -------unavailable-------\n"+
                        "    '"+Commands.doNotExportSource+"' = due to the modular, step-wise nature of this tool, the\n"+
                        "                              bytecode of all instrumented classes is exported\n"+
                        "                              during the instrumentation phase, persisted in\n"+
                        "                              human-readable form into each version's folder. This\n"+
                        "                              information is used to make source-lookups during test\n"+
                        "                              target generation. If you provide this flag, no sources\n"+
                        "                              are exported and these lookups are skipped. Consequently,\n"+
                        "                              you might obtain different results as diverging traces\n"+
                        "                              cannot be put into context anymore.\n"+
                        "                              => short: -------unavailable-------\n"+
                        "    '"+Commands.flatDivergences+"' = provide this flag to switch the export format for\n"+
                        "                     detected trace divergences from the default tree-based\n"+
                        "                     structure to a flat, list-based output format. Might\n"+
                        "                     make it easier to construct a parser for these results.\n"+
                        "                     This command has *NO* effect on the actual execution!\n"+
                        "                     => short: -------unavailable-------\n"+
                        "    '"+Commands.generateTargetsForSyntax+"' = provide this flag to generate additional testing\n"+
                        "                            targets in a separate file that are only based\n"+
                        "                            on syntactic change information.\n"+
                        "                            => short: -------unavailable-------\n"+
                        "    '"+Commands.generateTargetsForDivergences+"' = provide this flag to generate additional testing\n"+
                        "                                targets in a separate file that are only based on\n"+
                        "                                trace divergences.\n"+
                        "                                => short: -------unavailable-------\n"+
                        "    '"+Commands.useManualTraceMatchFile+"' = if you provide this flag, each version directory will\n"+
                        "                           be scanned for a 'traceMatching' file first before\n"+
                        "                           the automatic method based on entry points is called.\n"+
                        "                           You *MUST* create these files yourself! Its content\n"+
                        "                           must be a JSON Array containing JSON Objects matching\n"+
                        "                           traces by their identifiers:\n"+
                        "                              [\n"+
                        "                                {\n"+
                        "                                 \"traceInThis\": \"file0_trace1\",\n"+
                        "                                 \"matchTo\": \"file0_trace2\"\n" +
                        "                                }, (...)\n"+
                        "                              ]\n" +
                        "                           => short: -------unavailable-------\n"+
                        "    '"+Commands.restrictSyntaxMapping+"' = Normally, a divergent trace section inside a\n"+
                        "                                syntactically unmodified section is mapped to all\n"+"" +
                        "                                syntactically modified sections higher in its call\n" +
                        "                                chain. Provide this flag to restrict this mapping to\n"+
                        "                                the closest of those modified sections. This will\n"+
                        "                                obliviously influence your final results.\n"+
                        "                                => short: -------unavailable-------\n"+
                        "    '"+Commands.filterObjectCharacteristics+"' = Provide this flag to consider any observed object for\n"+
                        "                        which the string representation contains the character\n"+"" +
                        "                        '@' to have an equal toString() and hashCode() value\n"+
                        "                        during trace divergence analysis. This will of course\n"+
                        "                        gravely impact your final results!\n"+
                        "                        => short: -------unavailable-------\n"+
                        "    '"+Commands.noTargetDetails+"' = provide this flag to prune all additional data from\n"+
                        "                         your testing targets.\n"+"" +
                        "                         => short: -------unavailable-------\n\n\n"+


                        "  ## LOGGING OUTPUT CONTROL: ##\n"+
                        "    '"+Commands.toolOut+"' = Causes the output of external tools (such as your build system) to\n"+
                        "                     be included in the tool's output. Useful for debugging your provided\n"+
                        "                     arguments, e.g. a build command or a repository location.\n\n"+
                        "    '"+Commands.loud+"' = Causes more output and feedback, helpful when something is going wrong.\n"+
                        "               This does not include the effects of the '"+Commands.toolOut+"' command!\n\n"+
                        "    '"+Commands.quiet+"' = Suppresses most of the output. *NOTE* that errors will still be shown.\n"+
                        "                Furthermore, this will nullify the effects of other output commands!\n"+
                        "\n"+

                        "*** EXAMPLES: ***\n\n"+
                        "1.)'"+Commands.extractOnly+" "+Commands.clone+" \"git://git.code.net/myProject\" "+Commands.keepRepo+" "+Commands.outputFolder+" \"./mining/output\"'\n"+
                        "   will export a list of all commits on this GIT repository's master branch alongside a\n"+
                        "   functional copy of the repository itself into the specified directory. Since no\n"+
                        "   name was specified, a placeholder name will be used for both resulting files.\n"+
                        "\n"+
                        "2.)'"+Commands.extractOnly+" "+Commands.nameOfRepo+" \"personalProject\" "+Commands.pathToLocalRepo+" \"./git/myRepo\"' also results in 1.) but\n"+
                        "   the output folder will be named \"personalProject\" and an existing local copy will\n"+
                        "   be used for mining data. Because the target is a git repository, no network will be\n"+
                        "   required for the extraction, all required data is present inside the repository.\n"+
                        "   A local repository on your is of course never deleted afterwards, so a "+Commands.keepRepo+"\n"+
                        "   is unnecessary and would have no effect.\n"+
                        " \n"+
                        "3.)'"+Commands.analyze +" "+Commands.nameOfRepo+" \"myRepo\" "+Commands.pathToLocalRepo+" \"projectsToMine/myRepo\" "+Commands.relativePathToSources+" project/src \n"+
                        "   "+Commands.relativePathToCompiledSources+" project/build "+Commands.versions+" [1017187158c02e5bf4934629c29f221593832e6c,\n"+
                        "   4103c4b7d2e80d0d3423b2f28989dda9a2878355] "+Commands.buildCommand+" \"clean compile\"' will use\n"+
                        "   the command \"clean compile\" to compile each of the two versions individual.y\n"+
                        "   The syntactically differences between two will be analyzed but traces are not\n" +
                        "   generated as no execution-commands have been given. The user can do this manually by\n" +
                        "   executing the program now, as the compiled code has been instrumented.\n"+
                        "   *NOTE* that just like above, the location of both the uncompiled sources and the\n"+
                        "   compiled sources are given not as a list but as a single argument, using again\n"+
                        "   the convenience input feature. Like the build command, these arguments are being\n"+
                        "   applied to all versions specified using '"+Commands.versions+"'. Furthermore, if\n"+
                        "   '4103c4b7d2e80d0d3423b2f28989dda9a2878355' would be the direct successor of the\n"+
                        "   version with the identifier '1017187158c02e5bf4934629c29f221593832e6c', we could\n"+
                        "   have only given the former and its predecessor version would be determined\n"+
                        "   automatically.\n"+
                        " \n"+
                        "4.)'"+Commands.compute +" "+Commands.clone+" \"git://git.code.net/myProject\" "+Commands.buildCommand+" \"clean compile\"\n"+
                        "   "+Commands.relativePathToSources+" project/src "+Commands.relativePathToCompiledSources+" project/build "+Commands.testsDir+" project/tests\n"+
                        "   "+Commands.versions+" [1017187158c02e5bf4934629c29f221593832e6c,\n" +
                        "   4103c4b7d2e80d0d3423b2f28989dda9a2878355]' will  perform a full execution\n"+
                        "   will result in testing targets based on syntactic changes and execution\n"+
                        "   traces derived from JUnit tests contained in the 'tests' directory inside\n"+
                        "   your repository. Your targets will be exported to each version's directory.\n"+
                        "");
    }
}
