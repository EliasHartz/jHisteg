package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;

public class NonBoxedShort extends NonBoxed{
   
   public final short s;

   public NonBoxedShort(short s){
      this.s = s;
   }
   
   public static NonBoxedShort staticAccessToConstructor(short s){
      return new NonBoxedShort(s);
   }

   @Override public String getValueAsString(){
      return Short.toString(s);
   }

   @Override
   public String getTypeString() {
      return "short";
   }

   @Override
   public int hashCode(){
      return (int) s;
   }

}
