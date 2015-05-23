package hartz.masterThesis.historyGuidedImpactAnalysis.core.targets.special;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChangeType;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.targets.ImpactBasedTestingTarget;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.targets.TestingTarget;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Special target used when one is interested in syntax changes only! */
public class SyntaxChangeTestingTarget extends TestingTarget {
    protected final boolean doesNotInvolveMethod;
    protected final boolean newClassChange;
    protected final SyntaxChange[] changes;
    protected final String identifier;

    public SyntaxChangeTestingTarget(List<SyntaxChange> syntaxChanges) {
        String s = null;
        ArrayList<SyntaxChange> tmp = new ArrayList<>();

        boolean thisIsAboutSomeNewClass = false;
        boolean thisIsNotAboutSomeMethod = false;
        boolean thisIsNotAboutSomeMethodUnassignedFlag = true;

        for (SyntaxChange change : syntaxChanges){
            if (s == null) s = change.getUniqueAccessString();
            assert(change.getUniqueAccessString().equals(s)); //else the construction of the set is wrong!

            tmp.add(change); //store!

            if (change.onMethodLevel()){
                assert(thisIsNotAboutSomeMethodUnassignedFlag || !thisIsNotAboutSomeMethod);
                //by construction, either the if or the else is executed for ALL!

                thisIsNotAboutSomeMethod = false;
                thisIsNotAboutSomeMethodUnassignedFlag = false;
            } else {
                assert(thisIsNotAboutSomeMethodUnassignedFlag || thisIsNotAboutSomeMethod);
                //by construction, either the if or the else is executed for ALL!

                thisIsNotAboutSomeMethod = true;
                thisIsNotAboutSomeMethodUnassignedFlag = false;
            }

            if (change.getTypeOfSyntaxChange() == SyntaxChangeType.NEW_CLASS){
                assert(thisIsNotAboutSomeMethod == true /* 'else' above must have triggered before */);
                thisIsAboutSomeNewClass = true;
                assert(syntaxChanges.size() == 1 /* no other changes exist if this class is brand new!*/);
                break;
            }
        }

        newClassChange = thisIsAboutSomeNewClass;
        doesNotInvolveMethod = thisIsNotAboutSomeMethod;
        identifier = s;
        changes = tmp.toArray(new SyntaxChange[tmp.size()]);
    }


    @Override
    public String getIdentifier() {
        return identifier;
    }

    public JSONObject toJSON(){
        JSONObject result = new JSONObject();
        result.put("-> testingTarget", identifier);
        result.put("targetType", doesNotInvolveMethod?"class":"method");

        if (includeDetails) {
            JSONArray array = new JSONArray();
            for (SyntaxChange change : changes) {
                array.put(change.toJSONObjectWithoutIdentifier());
            }
            result.put("testBecause", array);
        }
        return result;
    }

    @Override
    public int compareTo(TestingTarget testingTarget) {
        if (testingTarget instanceof ImpactBasedTestingTarget || testingTarget instanceof ImpactBasedTestingTarget)
            return 1; //impact based targets are the real deal! Should actually never be compared to those!

        if (testingTarget instanceof TraceDivergenceTestingTarget) return -1; //targets from trace divergences are always less important
        assert(testingTarget instanceof SyntaxChangeTestingTarget);
        SyntaxChangeTestingTarget t = (SyntaxChangeTestingTarget) testingTarget;

        if (newClassChange && !t.newClassChange) return -1; //new class stuff is always more important
        if (!newClassChange && t.newClassChange) return 1; //not-a-new-class is always less important
        if (doesNotInvolveMethod && !t.doesNotInvolveMethod) return -1; //method stuff is always more important
        if (!doesNotInvolveMethod && t.doesNotInvolveMethod) return 1; //non-method stuff is always less important



        return t.changes.length - changes.length; //if I have more, then the number is negative!
    }
}
