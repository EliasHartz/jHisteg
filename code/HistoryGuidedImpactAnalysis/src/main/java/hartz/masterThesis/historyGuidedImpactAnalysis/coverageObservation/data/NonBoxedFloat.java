package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;

public class NonBoxedFloat extends NonBoxed{

   public final float f;

   public NonBoxedFloat(float f){
      this.f = f;
   }

   public static NonBoxedFloat staticAccessToConstructor(float f){
      return new NonBoxedFloat(f);
   }

   @Override public String getValueAsString(){
      return Float.toString(f);
   }

   @Override
   public String getTypeString() {
      return "float";
   }

   @Override
   public int hashCode(){
      return (int) f;
   }

}
