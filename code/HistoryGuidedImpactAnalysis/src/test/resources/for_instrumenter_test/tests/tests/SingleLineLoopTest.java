package tests;

import mainPackage.SomeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SingleLineLoopTest {

   @Test
   public void unitTest() {
      SomeClass c = new SomeClass();
      assertTrue(c.doSingleLineLoop(10)==0);
   }

}
