package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;

public class NonBoxedChar extends NonBoxed{
   
   public final char c;
   
   public NonBoxedChar(char c){
      this.c = c;
   }
   
   public static NonBoxedChar staticAccessToConstructor(char c){
      return new NonBoxedChar(c);
   }

   @Override public String getValueAsString(){
      return Character.toString(c);
   }

   @Override
   public String getTypeString() {
      return "char";
   }

   @Override
   public int hashCode(){
      return (int) c;
   }

}
