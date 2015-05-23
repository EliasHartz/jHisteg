package tests;

import mainPackage.SomeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IgnoredMethodsTest {

   @Test
   public void unitTest() {
      SomeClass c = new SomeClass();
      assertTrue(c.equals(c));
      assertFalse(c.toString().isEmpty());
      assertTrue(c.hashCode() >= 0);
   }

}
