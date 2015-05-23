package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;

public class NotReturnedYetSingleton extends SpecialReturnValue {
   
   private static final NotReturnedYetSingleton instance = new NotReturnedYetSingleton();
   private NotReturnedYetSingleton() { /* nothing of course */}
   
   public static NotReturnedYetSingleton getInstance() {
      return instance;
   }
   
   @Override public String toString(){
      return "\\-return value was never stored-/";
   }

   @Override public int hashCode() {return Integer.MIN_VALUE; }

}
