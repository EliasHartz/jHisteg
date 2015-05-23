package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;

public class VoidSingleton extends SpecialReturnValue {
   
   private static final VoidSingleton instance = new VoidSingleton();
   private VoidSingleton() { /* nothing of course */}
   
   public static VoidSingleton getInstance() {
      return instance;
   }

   public String toString(){
      return "void";
   }

   @Override public int hashCode() {return -1; }
}
