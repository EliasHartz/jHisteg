package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;

public class NonBoxedInteger extends NonBoxed{
   
   public final int i;

   public NonBoxedInteger(int i){
      this.i = i;
   }
   
   public static NonBoxedInteger staticAccessToConstructor(int i){
      return new NonBoxedInteger(i);
   }

   @Override public String getValueAsString(){
      return Integer.toString(i);
   }

   @Override
   public String getTypeString() {
      return "int";
   }

   @Override
   public int hashCode(){
      return i;
   }
}
