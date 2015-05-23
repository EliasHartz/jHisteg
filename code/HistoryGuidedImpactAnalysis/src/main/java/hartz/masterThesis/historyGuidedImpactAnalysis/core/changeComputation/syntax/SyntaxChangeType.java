package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax;

public enum SyntaxChangeType {
    CODE_ADDITION,
    CODE_MODIFICATION,
    RENAMING,
    CODE_MOVE,
    CODE_REMOVAL,
    NEW_CLASS, /* this is not an implemented change type in ChangeDistiller but my very own marker for the new
                  class file detection code, which I also encode as a syntax change */
    REMOVED_CLASS /* dito */
}
