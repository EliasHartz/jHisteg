package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.syntaxChange;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChangeType;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;


public class ClassLevelSyntaxChange extends SyntaxChange {

    protected final String affectedMethod;
    protected final String fullyQualifiedClassNameOld;
    protected final String fullyQualifiedClassNameNew;
    protected final String oldCode;
    protected final String newCode;

    /**
     * Creates a new Syntax Change instance modelling a change on class-level, that
     * is NOT inside or in the header of a method, from one version to another.
     *
     * @param typeOfChangedSourceCode : string-representation of the code that
     *                                  was modified
     * @param changeType : type of this change
     * @param fullyQualifiedClassNameOld : classname in previous version, may be 'null'
     * @param fullyQualifiedClassNameNew : classname in current version, may be 'null'
     * @param affectedMethod : which method was affected, e.g. by a change to
     *                         accessibility. May be 'null', for example if a field
     *                         of a class is modified.
     * @param codeOfOld : the actual statement in previous version, may be 'null'
     * @param codeOfNew : the actual statement in current version, may be 'null'
     */
    public ClassLevelSyntaxChange(String typeOfChangedSourceCode, SyntaxChangeType changeType,
                                  String fullyQualifiedClassNameOld, String fullyQualifiedClassNameNew,
                                  String affectedMethod, String codeOfOld, String codeOfNew){
        super(changeType, typeOfChangedSourceCode);
        this.fullyQualifiedClassNameOld = fullyQualifiedClassNameOld;
        this.fullyQualifiedClassNameNew = fullyQualifiedClassNameNew;
        this.affectedMethod = affectedMethod;
        this.oldCode = codeOfOld;
        this.newCode = codeOfNew;
    }

    /**
     * Creates a new Syntax Change instance modelling a change on class-level, that
     * is NOT inside or in the header of a method, from one version to another.
     *
     * @param toImportFrom : JSONObject modelling this change
     */
    public ClassLevelSyntaxChange(JSONObject toImportFrom) {
        super(SyntaxChangeType.valueOf(toImportFrom.getString("type")), toImportFrom.getString("codeType"));
        this.fullyQualifiedClassNameOld = toImportFrom.optString("classInPreviousVersion", null);
        this.fullyQualifiedClassNameNew = toImportFrom.optString("class", null);
        String oldCodeTmp = toImportFrom.optString("codeRemoved", null);
        String newCodeTmp = toImportFrom.optString("codeAdded", null);
        this.oldCode = toImportFrom.optString("codeBefore", oldCodeTmp);
        this.newCode = toImportFrom.optString("code", newCodeTmp);

        this.affectedMethod = toImportFrom.optString("affectedMethod", null);
    }

    @Override
    public String getUniqueAccessString() {
        String s = getFullyQualifiedClassName();
        if (affectedMethod != null) s+= "."+affectedMethod;
        return s;
    }

    @Override
    public boolean onMethodLevel() {
        return affectedMethod!=null;
    }

    @Override
    public JSONObject toJSONObject() {
        String level = this.getClass().getSimpleName();
        level = level.substring(0,level.indexOf("SyntaxChange")).replace("L","_L").toUpperCase();

        JSONObject o = new JSONObject();
        if (fullyQualifiedClassNameNew != null) o.put("class", fullyQualifiedClassNameNew);
        if (fullyQualifiedClassNameOld != null && !fullyQualifiedClassNameOld.equals(fullyQualifiedClassNameNew))
            o.put("classInPreviousVersion", fullyQualifiedClassNameOld);
        if (affectedMethod != null) o.put("affectedMethod", affectedMethod);
        o.put("codeType", typeOfChangedSourceCode);
        o.put("level", level);
        o.put("type", changeType);

        if (oldCode != null && !oldCode.equals(newCode))
            if (changeType == SyntaxChangeType.RENAMING || changeType == SyntaxChangeType.CODE_MODIFICATION ||
                    changeType == SyntaxChangeType.CODE_REMOVAL){
                assert(oldCode != null);
                o.put(changeType == SyntaxChangeType.CODE_REMOVAL?"codeRemoved":"codeBefore", oldCode);
            }

        if (changeType == SyntaxChangeType.RENAMING || changeType == SyntaxChangeType.CODE_MODIFICATION ||
                changeType == SyntaxChangeType.CODE_ADDITION){
            assert(newCode != null);
            o.put(changeType == SyntaxChangeType.CODE_ADDITION?"codeAdded":"code", newCode);
        }
        if (changeType == SyntaxChangeType.NEW_CLASS)
            //for new classes, never store code (as there cannot be entries...)
            assert(newCode == null && oldCode == null);

        return o;
    }

    @Override
    public JSONObject toJSONObjectWithoutIdentifier() {
        JSONObject o = new JSONObject();

        o.put("typeOfAffectedCode", typeOfChangedSourceCode);
        o.put("type", changeType);

        if (changeType == SyntaxChangeType.CODE_MOVE && fullyQualifiedClassNameNew != null && fullyQualifiedClassNameOld != null){
            o.put("classNow", fullyQualifiedClassNameOld);
            o.put("classBefore", fullyQualifiedClassNameNew);
        }

        if (changeType == SyntaxChangeType.RENAMING || changeType == SyntaxChangeType.CODE_MODIFICATION ||
                changeType == SyntaxChangeType.CODE_ADDITION){
            assert(newCode != null);
            o.put(changeType == SyntaxChangeType.CODE_ADDITION?"codeAdded":"code", newCode);
        }

        if (oldCode != null && !oldCode.equals(newCode))
            if (changeType == SyntaxChangeType.RENAMING || changeType == SyntaxChangeType.CODE_MODIFICATION ||
                    changeType == SyntaxChangeType.CODE_REMOVAL){
                assert(oldCode != null);
                o.put(changeType == SyntaxChangeType.CODE_REMOVAL?"codeRemoved":"codeBefore", oldCode);
            }
        return o;
    }

    @Override
    public int hashCode(){
        return ( (fullyQualifiedClassNameOld != null ? fullyQualifiedClassNameOld.hashCode() : 1 ) +
                (fullyQualifiedClassNameNew != null ? fullyQualifiedClassNameNew.hashCode() : 10 ) +
                (affectedMethod != null ? affectedMethod.hashCode() : 100 ) +
                (typeOfChangedSourceCode.hashCode()) +
                (changeType.ordinal()) +
                (oldCode != null ? oldCode.hashCode() : 5000 )+
                (newCode != null ? newCode.hashCode() : 50000) );
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof ClassLevelSyntaxChange){
            ClassLevelSyntaxChange c = (ClassLevelSyntaxChange) o;
            return  (fullyQualifiedClassNameOld != null ? fullyQualifiedClassNameOld.equals(c.fullyQualifiedClassNameOld) : (c.fullyQualifiedClassNameOld==null)) &&
                    (fullyQualifiedClassNameNew != null ? fullyQualifiedClassNameNew.equals(c.fullyQualifiedClassNameNew) : (c.fullyQualifiedClassNameNew==null)) &&
                    (affectedMethod != null ? affectedMethod.equals(c.affectedMethod) : (c.affectedMethod==null)) &&
                    (oldCode != null ? oldCode.equals(c.oldCode) : (c.oldCode==null)) &&
                    (newCode != null ? newCode.equals(c.newCode) : (c.newCode==null)) &&
                    (typeOfChangedSourceCode.equals(c.typeOfChangedSourceCode)) &&
                    (changeType == c.changeType);
        }
        else return false;
    }

    @Override
    public String toString(){
        if (changeType == SyntaxChangeType.CODE_REMOVAL){
            return  getFullyQualifiedClassName()+": "+ //class name
                    changeType.toString()+" of "+typeOfChangedSourceCode //what was detected
                    +" {"+(oldCode != null ? oldCode : "...")+"}"; //actual code
        }
        return  getFullyQualifiedClassName()+": "+ //class name
                changeType.toString()+" of "+typeOfChangedSourceCode //what was detected
                +" {"+(newCode != null ? newCode : (oldCode != null ? oldCode : "..."))+"}"; //actual code
    }

    public String getCodeInNewerVersion() {
        return newCode;
    }

    public String getAffectedMethodInNewerVersion() {
        return affectedMethod;
    }

    public String getFullyQualifiedClassNameInOlderVersion() {
        return fullyQualifiedClassNameOld;
    }

    public String getFullyQualifiedClassNameInNewerVersion() {
        return fullyQualifiedClassNameNew;
    }

    /**
     * @return most relevant fully qualified class name for this change, usually the one
     *         in the newer version
     */
    public String getFullyQualifiedClassName() {
        return (fullyQualifiedClassNameNew != null ? fullyQualifiedClassNameNew : fullyQualifiedClassNameOld);
    }

    public String getCodeInOlderVersion() {
        return oldCode;
    }
}
