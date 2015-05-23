package tests;

import mainPackage.HelloWorldMain;
import org.junit.Test;
import otherPackage.IhateTheWorld;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestSuite2 {

   @Test
   public void testSomething() {
      HelloWorldMain.main(null);
      assertEquals("A","A");
   }

   @Test
   public void testSomethingElse() {
      IhateTheWorld a = new IhateTheWorld();
      a.say();
      assertTrue(true);
   }

}
