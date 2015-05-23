package tests;

import mainPackage.SomeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AnotherMinimalTest {

   @Test
   public void unitTest() {
      assertTrue(SomeClass.echoABool(true)==true);
   }

}
