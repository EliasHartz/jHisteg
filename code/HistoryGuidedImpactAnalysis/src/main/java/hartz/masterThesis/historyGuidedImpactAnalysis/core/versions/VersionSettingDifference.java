package hartz.masterThesis.historyGuidedImpactAnalysis.core.versions;

import hartz.masterThesis.historyGuidedImpactAnalysis.main.Commands;

/** Models the various ways in which a version can be incompatible with another */
public enum VersionSettingDifference {
   SOURCE{@Override public String toString(){return "The source code directory you are using now is a different one!";}},
   COMPILED{@Override public String toString(){return "Your current call provides a different location for the compiled sources!";}},
   ADDITIONAL_COMPILED{@Override public String toString(){return "A different directory for additional compiled sources has been stated!";}},
   IGNORESUFFIX{@Override public String toString(){return "The classname-suffix for ignoring classes is different in the version present on the drive!";}},
   IGNOREPREFIX{@Override public String toString(){return "The classname-prefix for ignoring classes is different in the version present on the drive!";}},
   BUILDFILE{@Override public String toString(){return "Current and previous configurations differ in the build file's location or name!";}},
   BUILDCOMMAND{@Override public String toString(){return "Present version on disc used a different build command!";}},
   PREDECESSOR{@Override public String toString(){return "The version to compare against now does not match with what was extracted before! This will produce different results!";}},
   EXCLUDEDMETHODS{@Override public String toString(){return "The list of ignored methods is different in the version present on the drive!";}},
   EXCLUDEDDIRECTORIES{@Override public String toString(){return "The list of ignored directories is different in the version present on the drive!";}},
   RENAMING_SETTINGS{@Override public String toString(){return "Results of syntax analysis might be different since the value of the '"+ Commands.coverRenaming+"' flag is different in the present version!";}},
   BYTECODE_EXPORT{@Override public String toString(){return "Results of testing target computation might be different since the value of the '"+ Commands.doNotExportSource+"' flag is different in the present version!";}},
   MISSING{@Override public String toString(){return "The previously extracted version is corrupt, it is likely that the folder was manipulated by hand!";}}
}
