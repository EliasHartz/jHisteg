package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;

public class NullSingleton extends SpecialReturnValue {

   private static final NullSingleton instance = new NullSingleton();
   private NullSingleton() { /* nothing of course */}
   
   public static NullSingleton getInstance() {
      return instance;
   }

   @Override public String toString(){
      return "null";
   }
   @Override public int hashCode() {return -2; }
}
