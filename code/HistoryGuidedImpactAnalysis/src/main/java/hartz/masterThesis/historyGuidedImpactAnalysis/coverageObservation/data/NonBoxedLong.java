package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;

public class NonBoxedLong extends NonBoxed{
   
   public final long l;

   public NonBoxedLong(long l){
      this.l = l;
   }
   
   public static NonBoxedLong staticAccessToConstructor(long l){
      return new NonBoxedLong(l);
   }

   @Override public String getValueAsString(){
      return Long.toString(l);
   }

   @Override
   public String getTypeString() {
      return "long";
   }

   @Override
   public int hashCode(){
      return (int) l;
   }

}
