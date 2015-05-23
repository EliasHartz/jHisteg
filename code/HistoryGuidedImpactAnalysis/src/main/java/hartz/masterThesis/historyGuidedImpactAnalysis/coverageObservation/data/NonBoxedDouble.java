package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;

public class NonBoxedDouble extends NonBoxed{

   public final double d;

   public NonBoxedDouble(double d){
      this.d = d;
   }

   public static NonBoxedDouble staticAccessToConstructor(double d){
      return new NonBoxedDouble(d);
   }

   @Override public String getValueAsString(){
      return Double.toString(d);
   }

   @Override
   public String getTypeString() {
      return "double";
   }

   @Override
   public int hashCode(){
      return (int) d;
   }
}
