package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;


public class NonBoxedByte extends NonBoxed{
   
   public final byte b;
   
   public NonBoxedByte(byte b){
      this.b = b;
   }
   
   public static NonBoxedByte staticAccessToConstructor(byte b){
      return new NonBoxedByte(b);
   }

   @Override public String getValueAsString(){
      return Byte.toString(b);
   }

   @Override
   public String getTypeString() {
      return "byte";
   }

   @Override
   public int hashCode(){
      return (int) b;
   }

}
