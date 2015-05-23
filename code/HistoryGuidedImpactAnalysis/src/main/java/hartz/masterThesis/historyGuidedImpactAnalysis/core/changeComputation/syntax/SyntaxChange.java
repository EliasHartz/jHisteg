package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.syntaxChange.ClassLevelSyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.syntaxChange.MethodLevelSyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;

/** Models a syntactic change to source code */
public abstract class SyntaxChange{

    protected final String typeOfChangedSourceCode;
    protected final SyntaxChangeType changeType;

    protected SyntaxChange(SyntaxChangeType type, String descriptionOfChangedCodeType){
        changeType = type;
        typeOfChangedSourceCode = descriptionOfChangedCodeType;
    }

    /** Factory Constructor for import operation */
    public static SyntaxChange fromJSON(JSONObject toImportFrom){
        if (toImportFrom.getString("level").startsWith("METHOD"))
            return new MethodLevelSyntaxChange(toImportFrom);
        else return new ClassLevelSyntaxChange(toImportFrom);
    }

    /** @return the string that is used for accessing these objects in HashMaps.*/
    public abstract String getUniqueAccessString();

    public abstract boolean onMethodLevel();

    public String getDescriptionOfWhatKindOfCodeChanged() {
        return typeOfChangedSourceCode;
    }

    public SyntaxChangeType getTypeOfSyntaxChange() {
        return changeType;
    }

    public abstract JSONObject toJSONObject();

    public abstract JSONObject toJSONObjectWithoutIdentifier();
}
