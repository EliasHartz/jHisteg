package tests;

import org.junit.Test;
import otherPackage.IhateTheWorld;

public class TestSuite {

   @Test
   public void testHateWorld() {
      IhateTheWorld a = new IhateTheWorld();
      a.say();
      a.setWhatToSay("In fact, I love the world");
      a.say();
   }

}
