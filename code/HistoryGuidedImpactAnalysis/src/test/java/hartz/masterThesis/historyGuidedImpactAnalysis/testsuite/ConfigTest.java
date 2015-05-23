package hartz.masterThesis.historyGuidedImpactAnalysis.testsuite;

import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.Configuration;
import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Behavior;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Commands;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import hartz.masterThesis.historyGuidedImpactAnalysis.testsuite.utils.TestOnTestRepo;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class ConfigTest extends TestOnTestRepo {

    @Test
    public void testConfigSimple() throws IOException {
        Tool.print("-----------  testConfigSimple ------------------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.pathToLocalRepo, pathToTestRepo,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[",
                "4103c4b7d2e80d0d3423b2f28989dda9a2878355",
                "8239a8e8e8a601347802a486758498f0db321fd7",
                "]"
        });


        //verify correct number of versions
        assertEquals(2, c.getVersionIdentifiers().length);
        assertEquals(expectedNumberOfVersions, c.getAllVersionsOfRepo().getAmountOfCommits());

        //test correct default directories
        assertEquals(testingOutputFolder.getAbsolutePath(), c.getOutputDirectory().getAbsolutePath());
        assertEquals("",c.getRelativePathToSources(0));
        assertEquals("",c.getRelativePathToSources(1));
        assertEquals("",c.getRelativePathToCompiledSources(0));
        assertEquals("",c.getRelativePathToCompiledSources(1));
        assertEquals("",c.getRelativePathToBuildFile(0));
        assertEquals("",c.getRelativePathToBuildFile(1));

        //test correct default exclusion commands
        assertEquals(0,c.getIgnoreBySuffix(0).length);
        assertEquals(0,c.getIgnoreBySuffix(1).length);
        assertEquals(0,c.getIgnoreByPrefix(0).length);
        assertEquals(0,c.getIgnoreByPrefix(1).length);
        assertEquals("toString()Ljava/lang/String;",c.getExcludeMethodsFromInstrumentationBySignature(0)[0]);
        assertEquals("hashCode()I",c.getExcludeMethodsFromInstrumentationBySignature(0)[1]);
        assertEquals("equals(Ljava/lang/Object;)Z",c.getExcludeMethodsFromInstrumentationBySignature(0)[2]);
        assertEquals("toString()Ljava/lang/String;",c.getExcludeMethodsFromInstrumentationBySignature(1)[0]);
        assertEquals("hashCode()I",c.getExcludeMethodsFromInstrumentationBySignature(1)[1]);
        assertEquals("equals(Ljava/lang/Object;)Z",c.getExcludeMethodsFromInstrumentationBySignature(1)[2]);

        //test correct default trace generation commands
        assertEquals(true, c.areTracesToBeDeleted());
        assertEquals(0,c.getRunCommands(0).length);
        assertEquals(0,c.getRunCommands(1).length);
        assertEquals(0,c.getExecuteMainArguments(0).length);
        assertEquals(0,c.getExecuteMainArguments(1).length);
        assertEquals(0,c.getExecuteMainClassnames(0).length);
        assertEquals(0,c.getExecuteMainClassnames(1).length);
        assertEquals(0,c.getTestsToRun(0).length);
        assertEquals(0,c.getTestsToRun(1).length);
        assertEquals("",c.getRelativePathToTests(0));
        assertEquals("",c.getRelativePathToTests(1));

        //test correct behavior
        assertEquals(Behavior.EXTRACT, c.getBehavior());
        assertEquals("",c.getBuildCommand(0));
        assertEquals("",c.getBuildCommand(1));

        //verify boolean behavior flags
        assertFalse(c.areRenamedElementsToBeTreatedAsSyntaxChanges());
        assertFalse(c.isPresentDataToBeOverwritten());
        assertTrue(c.areTracesToBeDeleted());
        assertFalse(c.isRepoToBeDeletedAtTheEnd()); //local repos are never deleted
    }

    @Test
    public void testRepoConfigUglyListCase() throws IOException {
        Tool.print("-----------  testRepoConfigUglyListCase ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.pathToLocalRepo, pathToTestRepo,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[4103c4b7d2e80d0d3423b2f28989dda9a2878355,",
                "8239a8e8e8a601347802a486758498f0db321fd7",
                ",855ef919b3b94566e7873ef154a4c9abb9e36a2f",
                ","+
                        "f31bfd4f9b6b55083cf0102dea13143c7364c204]"
        });


        //verify correct number of versions
        assertEquals(4, c.getVersionIdentifiers().length);
        assertEquals(expectedNumberOfVersions, c.getAllVersionsOfRepo().getAmountOfCommits());
        assertEquals("4103c4b7d2e80d0d3423b2f28989dda9a2878355", c.getVersionIdentifiers()[0]);
        assertEquals("8239a8e8e8a601347802a486758498f0db321fd7", c.getVersionIdentifiers()[1]);
        assertEquals("855ef919b3b94566e7873ef154a4c9abb9e36a2f", c.getVersionIdentifiers()[2]);
        assertEquals("f31bfd4f9b6b55083cf0102dea13143c7364c204", c.getVersionIdentifiers()[3]);
    }

    @Test
    public void testRepoConfigPrefixAndSuffix() throws IOException {
        Tool.print("-----------  testRepoConfigPrefixAndSuffix ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[855ef919b3b94566e7873ef154a4c9abb9e36a2f,f31bfd4f9b6b55083cf0102dea13143c7364c204]",
                Commands.ignorePrefixShort,
                "[",
                "[myPrefix,",
                "myOtherPrefix",   //first list
                "]",
                "[",
                "mySecondPrefix]", //second list
                "]",
                Commands.ignoreSuffix,
                "[",
                "duplicatedValue1,duplicatedValue2",  //supposed to be duplicated and used for both!
                "]",
                Commands.pathToLocalRepo, pathToTestRepo,
        });

        //verify correct number of versions
        assertEquals(2, c.getVersionIdentifiers().length);
        assertEquals(expectedNumberOfVersions, c.getAllVersionsOfRepo().getAmountOfCommits());
        assertEquals("855ef919b3b94566e7873ef154a4c9abb9e36a2f", c.getVersionIdentifiers()[0]);
        assertEquals("f31bfd4f9b6b55083cf0102dea13143c7364c204", c.getVersionIdentifiers()[1]);

        //verify first prefix
        assertEquals(2, c.getIgnoreByPrefix(0).length);
        assertEquals("myPrefix", c.getIgnoreByPrefix(0)[0]);
        assertEquals("myOtherPrefix", c.getIgnoreByPrefix(0)[1]);

        //verify second prefix
        assertEquals(1, c.getIgnoreByPrefix(1).length);
        assertEquals("mySecondPrefix", c.getIgnoreByPrefix(1)[0]);

        //verify value duplication, suffixes used here
        assertEquals(2, c.getIgnoreBySuffix(0).length);
        assertEquals(2, c.getIgnoreBySuffix(1).length);
        assertEquals("duplicatedValue1", c.getIgnoreBySuffix(0)[0]);
        assertEquals("duplicatedValue1", c.getIgnoreBySuffix(1)[0]);
        assertEquals("duplicatedValue2", c.getIgnoreBySuffix(0)[1]);
        assertEquals("duplicatedValue2", c.getIgnoreBySuffix(1)[1]);
    }

    @Test
    public void testRepoConfigPrefixAndSuffixSpecialCases() throws IOException {
        Tool.print("-----------  testRepoConfigPrefixAndSuffixSpecialCases ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[855ef919b3b94566e7873ef154a4c9abb9e36a2f,f31bfd4f9b6b55083cf0102dea13143c7364c204," +
                        "8239a8e8e8a601347802a486758498f0db321fd7]",
                Commands.ignorePrefix,
                "duplicateThisForEVERYONE",
                Commands.ignoreSuffix,
                "[[duplicateThisForEveryone1,duplicateThisForEveryone2]]",
                Commands.pathToLocalRepo, pathToTestRepo,
        });

        //verify correct number of versions
        assertEquals(3, c.getVersionIdentifiers().length);

        //verify value duplication, prefixes used here
        assertEquals(1, c.getIgnoreByPrefix(0).length);
        assertEquals(1, c.getIgnoreByPrefix(1).length);
        assertEquals(1, c.getIgnoreByPrefix(2).length);
        assertEquals("duplicateThisForEVERYONE", c.getIgnoreByPrefix(0)[0]);
        assertEquals("duplicateThisForEVERYONE", c.getIgnoreByPrefix(1)[0]);
        assertEquals("duplicateThisForEVERYONE", c.getIgnoreByPrefix(2)[0]);

        //verify that empty list has been set correctly!
        assertEquals(2, c.getIgnoreBySuffix(0).length);
        assertEquals(2, c.getIgnoreBySuffix(1).length);
        assertEquals(2, c.getIgnoreBySuffix(2).length);
        assertEquals("duplicateThisForEveryone1", c.getIgnoreBySuffix(0)[0]);
        assertEquals("duplicateThisForEveryone1", c.getIgnoreBySuffix(1)[0]);
        assertEquals("duplicateThisForEveryone1", c.getIgnoreBySuffix(2)[0]);
        assertEquals("duplicateThisForEveryone2", c.getIgnoreBySuffix(0)[1]);
        assertEquals("duplicateThisForEveryone2", c.getIgnoreBySuffix(1)[1]);
        assertEquals("duplicateThisForEveryone2", c.getIgnoreBySuffix(2)[1]);
    }

    @Test
    public void testRepoConfigPrefixAndSuffixUglyEmptyListCase() throws IOException {
        Tool.print("-----------  testRepoConfigPrefixAndSuffixUglyEmptyListCase ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[855ef919b3b94566e7873ef154a4c9abb9e36a2f,8239a8e8e8a601347802a486758498f0db321fd7,f31bfd4f9b6b55083cf0102dea13143c7364c204]",
                Commands.ignorePrefix,
                "[",
                "[", /** very empty list case */
                "],[",
                "]",
                "[",
                "]",
                "]",
                Commands.ignoreSuffix,
                "[[",
                "],[]",
                ",",
                "[ ",
                " ]",
                "]",
                Commands.pathToLocalRepo, pathToTestRepo,
        });

        //verify that empty list has been set correctly!
        assertEquals(0, c.getIgnoreByPrefix(0).length);
        assertEquals(0, c.getIgnoreBySuffix(1).length);
        assertEquals(0, c.getIgnoreBySuffix(2).length);
    }

    @Test
    public void testRepoConfigRunAndTestCommandsWithUglyLists() throws IOException {
        Tool.print("-----------  testRepoConfigPrefixAndSuffixUglyEmptyListCase ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[a,b,c,d,e,f,g,h]",
                Commands.runCommand,
                       "[[ant test], [ant test],"+
                        "[ant test], [ant test, ant testy, ant testy test],"+
                        "[ant test test], [testit!],"+
                        "[], [mvn clean install -DskipTests=true]]",
                Commands.executeTests,
                        "[[],[],"+
                        "[tests.TestSuite], [tests.TestSuite, tests.TestSuite2],"+
                        "[blub/blubber/blub,/blubblub/blub], [   ],"+
                        "[tests.TestSuite2], []]",
                Commands.pathToLocalRepo, pathToTestRepo,
        });

        assertEquals(1, c.getRunCommands(0).length);
        assertEquals("ant test", c.getRunCommands(0)[0]);

        assertEquals(1, c.getRunCommands(1).length);
        assertEquals("ant test", c.getRunCommands(1)[0]);

        assertEquals(1, c.getRunCommands(2).length);
        assertEquals("ant test", c.getRunCommands(2)[0]);

        assertEquals(3, c.getRunCommands(3).length);
        assertEquals("ant test", c.getRunCommands(3)[0]);
        assertEquals("ant testy", c.getRunCommands(3)[1]);
        assertEquals("ant testy test", c.getRunCommands(3)[2]);

        assertEquals(1, c.getRunCommands(4).length);
        assertEquals("ant test test", c.getRunCommands(4)[0]);

        assertEquals(1, c.getRunCommands(5).length);
        assertEquals("testit!", c.getRunCommands(5)[0]);

        assertEquals(0, c.getRunCommands(6).length);
        //nothing obviously

        assertEquals(1, c.getRunCommands(7).length);
        assertEquals("mvn clean install -DskipTests=true", c.getRunCommands(7)[0]);






        assertEquals(0, c.getTestsToRun(0).length);
        //nothing obviously

        assertEquals(0, c.getTestsToRun(1).length);
        //nothing obviously

        assertEquals(1, c.getTestsToRun(2).length);
        assertEquals("tests.TestSuite", c.getTestsToRun(2)[0]);

        assertEquals(2, c.getTestsToRun(3).length);
        assertEquals("tests.TestSuite", c.getTestsToRun(3)[0]);
        assertEquals("tests.TestSuite2", c.getTestsToRun(3)[1]);

        assertEquals(2, c.getTestsToRun(4).length);
        assertEquals("blub/blubber/blub", c.getTestsToRun(4)[0]);
        assertEquals("blubblub/blub", c.getTestsToRun(4)[1]);

        assertEquals(0, c.getTestsToRun(5).length);
        //nothing obviously

        assertEquals(1, c.getTestsToRun(6).length);
        assertEquals("tests.TestSuite2", c.getTestsToRun(6)[0]);

        assertEquals(0, c.getTestsToRun(7).length);
        //nothing obviously

    }




    @Test
    public void testRepoConfigAutomaticPredecessorCase() throws IOException {
        Tool.print("-----------  testRepoConfigSingleVersionCase ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "8239a8e8e8a601347802a486758498f0db321fd7",
                Commands.pathToLocalRepo, pathToTestRepo,
        });


        //verify correct number of versions
        assertEquals(1, c.getVersionIdentifiers().length); //at this point, no automatic predecessor computation has been executed yet
        assertEquals("", c.getRelativePathToSources(0)); //nothing has been set, should be the default!
        assertEquals(expectedNumberOfVersions, c.getAllVersionsOfRepo().getAmountOfCommits());
        assertEquals("8239a8e8e8a601347802a486758498f0db321fd7", c.getVersionIdentifiers()[0]);
    }

    @Test
    public void testConfigInitialVersionCase() throws IOException {
        Tool.print("-----------  testConfigSingleAndInitialVersionCase ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[1017187158c02e5bf4934629c29f221593832e6c]",
                Commands.pathToLocalRepo, pathToTestRepo,
        });

        assertEquals(1, c.getVersionIdentifiers().length);
        assertEquals(expectedNumberOfVersions, c.getAllVersionsOfRepo().getAmountOfCommits());
        assertEquals("1017187158c02e5bf4934629c29f221593832e6c", c.getVersionIdentifiers()[0]);
    }

    @Test
    public void testConfigWithAdditionalParameters() throws IOException {
        Tool.print("-----------  testConfigWithAdditionalParameters ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.compile,
                Commands.pathToLocalRepo, pathToTestRepo,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.loud,
                Commands.nameOfRepo,
                "myNameForTheRepo",
                Commands.forceOverwrite,
                Commands.cleanBeforeCompile,
                Commands.coverageBasedTraces,
                Commands.coverRenamingShort,
                Commands.branchToWorkOn,
                "notMaster",
                Commands.versions,
                "[4103c4b7d2e80d0d3423b2f28989dda9a2878355,",
                "8239a8e8e8a601347802a486758498f0db321fd7,",
                "855ef919b3b94566e7873ef154a4c9abb9e36a2f]"
        });


        //verify correct number of versions
        assertEquals(3, c.getVersionIdentifiers().length);
        assertEquals(expectedNumberOfVersions, c.getAllVersionsOfRepo().getAmountOfCommits());

        //verify name and branch
        File historyFile = null;
        for (File f : testingOutputFolder.listFiles()){
            if (f.isFile()){
                if (historyFile == null)
                    historyFile = f;
                else fail("found unexpected file "+f.getName());
            }
        }
        assertTrue(historyFile.getName().contains("myNameForTheRepo"));
        assertEquals("notMaster", c.getRepositoryMiner().getBranchName());

        //test correct behavior
        assertEquals(Behavior.COMPILE, c.getBehavior());
        assertEquals("",c.getBuildCommand(0));
        assertEquals("",c.getBuildCommand(1));

        //verify boolean behavior flags
        assertTrue(c.areRenamedElementsToBeTreatedAsSyntaxChanges());
        assertTrue(c.isPresentDataToBeOverwritten());
        assertFalse(c.isRepoToBeDeletedAtTheEnd()); //local repos are never deleted
    }

    @Test
    public void testConfigWithSameSrcAndBuildCommands() throws IOException {
        Tool.print("-----------  testConfigWithSameSrcAndBuildCommands ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.relativePathToSources, "src",
                Commands.relativePathToCompiledSources, "build",
                Commands.buildCommand, /*ant*/ "compile",
                Commands.pathToLocalRepo, pathToTestRepo,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[",
                "4103c4b7d2e80d0d3423b2f28989dda9a2878355",",",
                "8239a8e8e8a601347802a486758498f0db321fd7,",
                "855ef919b3b94566e7873ef154a4c9abb9e36a2f",
                "]",
                Commands.testsDir, "tests",
        });

        assertEquals("src",c.getRelativePathToSources(0));
        assertEquals("src",c.getRelativePathToSources(1));
        assertEquals("src",c.getRelativePathToSources(2));
        assertEquals("", c.getRelativePathToSources(99)); //the invalid entry, expected default!

        assertEquals("build",c.getRelativePathToCompiledSources(0));
        assertEquals("build",c.getRelativePathToCompiledSources(1));
        assertEquals("build",c.getRelativePathToCompiledSources(2));
        assertEquals("", c.getRelativePathToCompiledSources(99)); //the invalid entry, expected default!

        assertEquals("",c.getRelativePathToBuildFile(0));//all return the default value because nothing has been set!
        assertEquals("",c.getRelativePathToBuildFile(1));
        assertEquals("",c.getRelativePathToBuildFile(2));
        assertEquals("", c.getRelativePathToBuildFile(99));

        assertEquals("tests",c.getRelativePathToTests(0));
        assertEquals("tests",c.getRelativePathToTests(1));
        assertEquals("tests",c.getRelativePathToTests(2));
        assertEquals("", c.getRelativePathToTests(99)); //the invalid entry, expected default!

        assertEquals("compile",c.getBuildCommand(0));
        assertEquals("compile",c.getBuildCommand(1));
        assertEquals("compile",c.getBuildCommand(2));
        assertEquals("", c.getBuildCommand(99)); //the invalid entry, expected default!
    }

    @Test
    public void testRepoConfigExcludedDirs() throws IOException {
        Tool.print("-----------  testRepoConfigExcludedMethods ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[4103c4b7d2e80d0d3423b2f28989dda9a2878355",
                "f31bfd4f9b6b55083cf0102dea13143c7364c204]",
                Commands.excludeDirFromInstrumentation,
                "[[src/bla],[/src/blablub,src/blub]]",
                Commands.pathToLocalRepo, pathToTestRepo,
        });
        assertEquals(2, c.getVersionIdentifiers().length);
        assertEquals(1, c.getDirectoriesExcludedFromInstrumentation(0).length);
        assertEquals(2, c.getDirectoriesExcludedFromInstrumentation(1).length);

        assertEquals("src/bla",c.getDirectoriesExcludedFromInstrumentation(0)[0]);
        assertEquals("src/blablub",c.getDirectoriesExcludedFromInstrumentation(1)[0]);
        assertEquals("src/blub",c.getDirectoriesExcludedFromInstrumentation(1)[1]);
    }

    @Test
    public void testRepoConfigExcludeNoMethods() throws IOException {
        Tool.print("-----------  testRepoConfigExcludeNoMethods ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[",
                "855ef919b3b94566e7873ef154a4c9abb9e36a2f",
                "f31bfd4f9b6b55083cf0102dea13143c7364c204]",
                Commands.excludeMethodFromInstrumentation,
                "[]", //empty list case!",
                Commands.pathToLocalRepo, pathToTestRepo,
        });
        assertEquals(2, c.getVersionIdentifiers().length);
        assertEquals(0, c.getExcludeMethodsFromInstrumentationBySignature(0).length);
        assertEquals(0, c.getExcludeMethodsFromInstrumentationBySignature(1).length);
    }

    @Test
    public void testRepoConfigExcludedMethods() throws IOException {
        Tool.print("-----------  testRepoConfigExcludedMethods ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[4103c4b7d2e80d0d3423b2f28989dda9a2878355",
                "855ef919b3b94566e7873ef154a4c9abb9e36a2f",
                "f31bfd4f9b6b55083cf0102dea13143c7364c204]",
                Commands.excludeMethodFromInstrumentation,
                "[[myExcludedMethod,myOtherExcludedMethod],[someOtherExcludedMethod],[]]",
                Commands.pathToLocalRepo, pathToTestRepo
        });
        assertEquals(3, c.getVersionIdentifiers().length);
        assertEquals(2, c.getExcludeMethodsFromInstrumentationBySignature(0).length);
        assertEquals(1, c.getExcludeMethodsFromInstrumentationBySignature(1).length);
        assertEquals(0, c.getExcludeMethodsFromInstrumentationBySignature(2).length);

        assertEquals("myExcludedMethod",c.getExcludeMethodsFromInstrumentationBySignature(0)[0]);
        assertEquals("myOtherExcludedMethod",c.getExcludeMethodsFromInstrumentationBySignature(0)[1]);
        assertEquals("someOtherExcludedMethod",c.getExcludeMethodsFromInstrumentationBySignature(1)[0]);
    }

    @Test
    public void testRepoConfigExcludedMethodsDefaultValue() throws IOException {
        Tool.print("-----------  testRepoConfigExcludedMethodsDefaultValue ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "f31bfd4f9b6b55083cf0102dea13143c7364c204",
                Commands.pathToLocalRepo, pathToTestRepo,
        });

        //we expect the default values!
        assertEquals(1, c.getVersionIdentifiers().length);
        assertEquals(3, c.getExcludeMethodsFromInstrumentationBySignature(0).length);
        assertEquals("toString()Ljava/lang/String;",c.getExcludeMethodsFromInstrumentationBySignature(0)[0]);
        assertEquals("hashCode()I",c.getExcludeMethodsFromInstrumentationBySignature(0)[1]);
        assertEquals("equals(Ljava/lang/Object;)Z",c.getExcludeMethodsFromInstrumentationBySignature(0)[2]);
    }

    @Test
    public void testRepoConfigExcludedMethodsWithArrayType() throws IOException {
        Tool.print("-----------  testRepoConfigExcludedMethodsWithArrayType ------------------------------");
        Configuration c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.excludeMethodFromInstrumentation,
                "main([Ljava/lang/String;)V",
                Commands.versions,
                "f31bfd4f9b6b55083cf0102dea13143c7364c204",
                Commands.pathToLocalRepo, pathToTestRepo,
        });
        assertEquals(1, c.getVersionIdentifiers().length);
        assertEquals(1, c.getExcludeMethodsFromInstrumentationBySignature(0).length);
        assertEquals("main([Ljava/lang/String;)V",c.getExcludeMethodsFromInstrumentationBySignature(0)[0]);


        c = Tool.makeConfigAndPrepareData(new String[] {
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[f31bfd4f9b6b55083cf0102dea13143c7364c204",
                "855ef919b3b94566e7873ef154a4c9abb9e36a2f",
                "]",
                Commands.excludeMethodFromInstrumentation,
                "[",
                "[main([Ljava/lang/String;)V]]",
                Commands.pathToLocalRepo, pathToTestRepo,
        });
        assertEquals(2, c.getVersionIdentifiers().length);
        assertEquals(1, c.getExcludeMethodsFromInstrumentationBySignature(0).length);
        assertEquals(1, c.getExcludeMethodsFromInstrumentationBySignature(1).length);
        assertEquals("main([Ljava/lang/String;)V",c.getExcludeMethodsFromInstrumentationBySignature(0)[0]);
        assertEquals("main([Ljava/lang/String;)V",c.getExcludeMethodsFromInstrumentationBySignature(1)[0]);
    }

    @Test
    public void testMissingData() throws IOException {
        Tool.print("-----------  testMissingData ------------------------------");
        try {
            Tool.makeConfigAndPrepareData(new String[]{
                    //no behavior!
                    Commands.versions,
                    "[",
                    "4103c4b7d2e80d0d3423b2f28989dda9a2878355",
                    "8239a8e8e8a601347802a486758498f0db321fd7",
                    "855ef919b3b94566e7873ef154a4c9abb9e36a2f",
                    "]",
                    Commands.outputFolder, testingOutputFolder.getAbsolutePath()
            });
            fail("this is an illegal case, we expect an exception!");
        } catch (IllegalArgumentException e) { /* that is good */}

        try {
            Tool.makeConfigAndPrepareData(new String[]{
                    Commands.extractOnly,
                    Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                    Commands.versions, //no repo given!
                    "[",
                    "4103c4b7d2e80d0d3423b2f28989dda9a2878355",
                    "8239a8e8e8a601347802a486758498f0db321fd7",
                    "855ef919b3b94566e7873ef154a4c9abb9e36a2f",
                    "]"
            });
            fail("this is an illegal case, we expect an exception!");
        } catch (IllegalArgumentException e) { /* that is good */}

        try {
            Tool.makeConfigAndPrepareData(new String[]{
                    Commands.extractOnly,
                    Commands.pathToLocalRepo, pathToTestRepo,
                    Commands.outputFolder, testingOutputFolder.getAbsolutePath()
                    //no version!
            });
            fail("this is an illegal case, we expect an exception!");
        } catch (IllegalArgumentException e) { /* that is good */}
    }

    @Test
    public void testConfigWithInvalidAmountOfPaths() throws IOException {
        Tool.print("-----------  testConfigWithInvalidAmountOfPaths ------------------------------");
        try{
            Tool.makeConfigAndPrepareData(new String[] {
                    Commands.extractOnly,
                    Commands.pathToLocalRepo, pathToTestRepo,
                    Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                    Commands.relativePathToSources, "[src,src]", //this is invalid because either you give 0, 1 or 3 here!
                    Commands.versions,
                    "[",
                    "4103c4b7d2e80d0d3423b2f28989dda9a2878355",
                    "8239a8e8e8a601347802a486758498f0db321fd7",
                    "855ef919b3b94566e7873ef154a4c9abb9e36a2f",
                    "]"
            });
            fail("this is an illegal case, we expect an exception!");
        }
        catch (IllegalArgumentException e){ /* that is good */}

        try{
            Tool.makeConfigAndPrepareData(new String[] {
                    Commands.extractOnly,
                    Commands.pathToLocalRepo, pathToTestRepo,
                    Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                    Commands.versions,
                    "[",
                    "4103c4b7d2e80d0d3423b2f28989dda9a2878355",
                    "8239a8e8e8a601347802a486758498f0db321fd7",
                    "855ef919b3b94566e7873ef154a4c9abb9e36a2f",
                    "]",
                    Commands.relativePathToSources, "[src,src]" //this is invalid because either you give 0, 1 or 3 here!
            });
            fail("this is an illegal case, we expect an exception!");
        }
        catch (IllegalArgumentException e){ /* that is good */}

        try{
            Tool.makeConfigAndPrepareData(new String[] {
                    Commands.extractOnly,
                    Commands.pathToLocalRepo, pathToTestRepo,
                    Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                    Commands.versions,
                    "[",
                    "8239a8e8e8a601347802a486758498f0db321fd7",
                    "855ef919b3b94566e7873ef154a4c9abb9e36a2f",
                    "]",
                    Commands.relativePathToSources, "[src,src]",
                    Commands.relativePathToCompiledSourcesShort, "[a,b,c]" //this is invalid because either you give 0, 1 or 3 here!
            });
            fail("this is an illegal case, we expect an exception!");
        }
        catch (IllegalArgumentException e){ /* that is good */}

        try{
            Tool.makeConfigAndPrepareData(new String[] {
                    Commands.extractOnly,
                    Commands.pathToLocalRepo, pathToTestRepo,
                    Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                    Commands.relativePathToSources, "[","src",",src", "]",
                    Commands.relativePathToCompiledSourcesShort, "[a,b,c]", //this is invalid because either you give 0, 1 or 3 here!
                    Commands.versions,
                    "[",
                    "8239a8e8e8a601347802a486758498f0db321fd7",
                    "855ef919b3b94566e7873ef154a4c9abb9e36a2f",
                    "]"
            });
            fail("this is an illegal case, we expect an exception!");
        }
        catch (IllegalArgumentException e){ /* that is good */}

        try{
            Tool.makeConfigAndPrepareData(new String[] {
                    Commands.extractOnly,
                    Commands.pathToLocalRepo, pathToTestRepo,
                    Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                    Commands.testsDir, "[tests1,tests2]", //no many!
                    Commands.versions,
                    "855ef919b3b94566e7873ef154a4c9abb9e36a2f",
            });
            fail("this is an illegal case, we expect an exception!");
        }
        catch (IllegalArgumentException e){ /* that is good */}
    }

    @Test
    public void testConfigWithTypoInList() throws IOException {
        Tool.print("-----------  testConfigWithTypoInList ------------------------------");
        try{
            Tool.makeConfigAndPrepareData(new String[]{
                    Commands.extractOnly,
                    Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                    Commands.versions,
                    "[ 1017187158c02e5bf4934629c29f221593832e6c", //user has forgotten ']' character!
                    Commands.pathToLocalRepo, pathToTestRepo
            });
            fail("this is an illegal case, we expect an exception!");
        }
        catch (IllegalArgumentException e){ /* that is good */}


        Configuration c =  Tool.makeConfigAndPrepareData(new String[]{
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "1017187158c02e5bf4934629c29f221593832e6c]", //user has forgotten '[' character but this is valid!
                Commands.pathToLocalRepo, pathToTestRepo
        });
        assertEquals("1017187158c02e5bf4934629c29f221593832e6c]",c.getVersionIdentifiers()[0]);
    }


    @Test
    public void testRepoConfigWithMissingPreOrSuffixes() throws IOException {
        Tool.print("-----------  testRepoConfigWithMissingPreOrSuffixes ------------------------------");

        boolean suffixMode = false;
        while (true){
            try {
                Tool.makeConfigAndPrepareData(new String[]{
                        Commands.extractOnly,
                        Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                        Commands.versions,
                        "[",
                        "855ef919b3b94566e7873ef154a4c9abb9e36a2f,f31bfd4f9b6b55083cf0102dea13143c7364c204",
                        "1017187158c02e5bf4934629c29f221593832e6c]",
                        suffixMode?Commands.ignoreSuffix:Commands.ignorePrefix,
                        "[",
                        "[first],",
                        "[second]", /* no third, it has been forgotten*/
                        "]",
                        Commands.pathToLocalRepo, pathToTestRepo
                });
                fail("this is an illegal case, we expect an exception!");
            } catch (IllegalArgumentException e){ /* that is good */}

            //verify that it works both ways!
            try {
                Tool.makeConfigAndPrepareData(new String[]{
                        Commands.extractOnly,
                        Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                        suffixMode?Commands.ignoreSuffix:Commands.ignorePrefix,
                        "[",
                        "[first],[second]", /* no third, it has been forgotten*/
                        "]",
                        Commands.pathToLocalRepo, pathToTestRepo,
                        Commands.versions,
                        "[",
                        "855ef919b3b94566e7873ef154a4c9abb9e36a2f,f31bfd4f9b6b55083cf0102dea13143c7364c204",
                        "1017187158c02e5bf4934629c29f221593832e6c]"
                });
                fail("this is an illegal case, we expect an exception!");
            } catch (IllegalArgumentException e){ /* that is good */}

            try {
                Tool.makeConfigAndPrepareData(new String[]{
                        Commands.extractOnly,
                        Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                        Commands.versions,
                        "[",
                        "855ef919b3b94566e7873ef154a4c9abb9e36a2f,f31bfd4f9b6b55083cf0102dea13143c7364c204",
                        "1017187158c02e5bf4934629c29f221593832e6c]",
                        suffixMode?Commands.ignoreSuffix:Commands.ignorePrefix,
                        "[",
                        "[first],[second]", /* no third, it has been forgotten*/
                        "]",
                        Commands.pathToLocalRepo, pathToTestRepo
                });
                fail("this is an illegal case, we expect an exception!");
            } catch (IllegalArgumentException e){ /* that is good */}

            if (suffixMode) break; else suffixMode = true;
        }

    }
}
