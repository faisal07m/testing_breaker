package de.upb.bibifi2018.kaffeeklatsch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class StrictArgParserTest {

  @Test
  void simple() throws Exception {
    StrictArgParser strictArgParser = new StrictArgParser(
        new String[]{"-i", "127.0.0.1", "-gagoob"},
        "guh",
        "aim");

    strictArgParser.hasAll("iga");
    strictArgParser.hasExactlyOneOf("guh");


    assertTrue(strictArgParser.getFlag('g'));
    assertFalse(strictArgParser.getFlag('p'));
    assertFalse(strictArgParser.getFlag('u'));
    assertFalse(strictArgParser.getFlag('h'));

    String i = strictArgParser.getParameter('i', "1.1.1.1");
    assertEquals("127.0.0.1", i);


    String def = strictArgParser.getParameter('m', "1.1.1.1");
    assertEquals("1.1.1.1", def);

    assertEquals("goob", strictArgParser.getParameter('a'));


    try {
      strictArgParser.getParameter('m');
      fail();
    } catch (Exception e){

    }


    try {
      strictArgParser.hasAll("mi");
      fail();
    } catch (Exception e){

    }

    try {
      strictArgParser.hasExactlyOneOf("m");
      fail();
    } catch (Exception e){

    }

    try {
      strictArgParser.hasExactlyOneOf("ia");
      fail();
    } catch (Exception e){

    }
  }


  @Test
  void errorMissingArg() {
    try {
      StrictArgParser strictArgParser = new StrictArgParser(
          new String[]{"-gaNoob", "-i"},
          "guh",
          "aim");

      fail();
    } catch (Exception e) {

    }
  }

  @Test
  void errorDuplicate() {

    try {
      StrictArgParser strictArgParser = new StrictArgParser(
          new String[]{"-gaNoob", "-a", "blob"},
          "guh",
          "aim");

      fail();
    } catch (Exception e) {

    }
  }
}
