package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.syntaxChange;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChangeType;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;

/**
 * Models a syntax change INSIDE a method! Has actually no additional data at the moment
 * but could be used to hold more specific data like information about branches and the like.
 */
public class MethodLevelSyntaxChange extends ClassLevelSyntaxChange {

    public MethodLevelSyntaxChange(String typeOfChangedSourceCode, SyntaxChangeType changeType, String codeOfOld,
                                   String codeOfNew, String fullyQualifiedClassName, String fullyQualifiedMethodName){
        super(typeOfChangedSourceCode, changeType, null, fullyQualifiedClassName, fullyQualifiedMethodName, codeOfOld, codeOfNew);
    }

    public MethodLevelSyntaxChange(JSONObject toImportFrom) {
        super(toImportFrom);
    }
}
