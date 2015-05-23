package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;

public class NonBoxedBoolean extends NonBoxed{
   
   public final boolean b;

   public NonBoxedBoolean(boolean b){
      this.b = b;
   }
   
   public static NonBoxedBoolean staticAccessToConstructor(boolean b){
     return new NonBoxedBoolean(b);
   }
   
   @Override public String getValueAsString(){
      return Boolean.toString(b);
   }

   @Override
   public String getTypeString() {
      return "boolean";
   }

   @Override
   public int hashCode(){
      return b ? 1 : 0;
   }

}
