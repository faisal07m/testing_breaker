package de.upb.bibifi.verybest.bank.cli;

import de.upb.bibifi.verybest.atm.cli.CommandLineArgs;
import de.upb.bibifi.verybest.atm.cli.CommandLineReader;
import de.upb.bibifi.verybest.atm.cli.CommandLineReaderImpl;
import de.upb.bibifi.verybest.common.Constants;
import de.upb.bibifi.verybest.common.util.InputValidationUtil;
import okio.BufferedSink;
import okio.Okio;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

import static de.upb.bibifi.verybest.common.util.InputValidationUtil.isCharValidForFilename;
import static de.upb.bibifi.verybest.common.util.InputValidationUtil.isValidFilename;
import static org.junit.jupiter.api.Assertions.*;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AtmCliTest {

    private static final int ATM_ERROR_CODE = Constants.ERROR_CODE_USER;
    private static final String DEFAULT_BANK_AUTH_FILE = "bank.auth";
    private static final String DEFAULT_ATM_CARD_FILE_EXTENSION = ".card";
    private static final String DEFAULT_ATM_IP = "127.0.0.1";
    private static final int DEFAULT_ATM_PORT = 3000;

    private PrintStream systemOut;
    private OutputStream out;

    private CommandLineReader reader;

    private String authFileName;
    private String cardFileName;

    @BeforeAll
    void init() {
        systemOut = System.out;
        authFileName = generateRandomFileName();
        File authFile = new File(authFileName);
        try (BufferedSink sink = Okio.buffer(Okio.sink(authFile))) {
            sink.writeUtf8("sdfdsf");
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        File defaultAuthFile = new File(DEFAULT_BANK_AUTH_FILE);
        try (BufferedSink sink = Okio.buffer(Okio.sink(defaultAuthFile))) {
            sink.writeUtf8("sdfdsf");
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        cardFileName = generateRandomFileName();
        File cardFile = new File(cardFileName);
        try (BufferedSink sink = Okio.buffer(Okio.sink(cardFile))) {
            sink.writeUtf8("sdfdsf");
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        File bobCardFile = new File("bob.card");
        try (BufferedSink sink = Okio.buffer(Okio.sink(bobCardFile))) {
            sink.writeUtf8("sdfdsf");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @AfterAll
    void cleanUp() {
        System.setOut(systemOut);
        File authFile = new File(authFileName);
        authFile.delete();

        File defaultAuthFile = new File(DEFAULT_BANK_AUTH_FILE);
        defaultAuthFile.delete();

        File cardFile = new File(cardFileName);
        cardFile.delete();

        File bobCardFile = new File("bob.card");
        bobCardFile.delete();
    }

    @BeforeEach
    void initEach() {
        reader = new CommandLineReaderImpl();
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    @Test
    void testInvalidFileNameRegex() {
        StringBuilder builder = new StringBuilder(4097);
        for (int i = 0; i < 4097; i++) {
            builder.append("a");
        }
        String[] invalidFiles = new String[]{
                ".",
                "..",
                "",
                "\t   \n  ",
                builder.toString()
        };
        for (String s : invalidFiles) {
            assertFalse(isValidFilename(s), s + " should be considered invalid!");
        }
    }

    @RepeatedTest(20)
    void testValidRandomInput() {
        String randomCardFileName = cardFileName;
        String randomIp = generateRandomIp();

        Random r = new Random();
        String randomAccountName = generateRandomFileName(r, r.nextInt(122) + 1);

        int randomPort = generateRandomPort();

        String[] args = new String[]{
                "-s", authFileName,
                "-c", randomCardFileName,
                "-p", "" + randomPort,
                "-i", randomIp,
                "-a", randomAccountName,
                "-g"};
        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(args);

        assertEquals(0, cmdArgs.getRight().intValue(),
                "The parser should have accepted the valid input \"" + Arrays.toString(args) + "\"");
        assertNotNull(cmdArgs.getLeft().authFile(), "The parser should have set an auth-file");
        assertNotNull(cmdArgs.getLeft().cardFile(), "The parser should have set an card-file");
        assertNotNull(cmdArgs.getLeft().account(), "The parser should have set an account-name");
        assertNotNull(cmdArgs.getLeft().ipAddress(), "The parser should have set an ip-address");


        assertEquals(authFileName, cmdArgs.getLeft().authFile().getName(),
                "The parser should have set the correct auth-file");
        assertEquals(randomCardFileName, cmdArgs.getLeft().cardFile().getName(),
                "The parser should have set the correct card-file");
        assertEquals(randomAccountName, cmdArgs.getLeft().account(),
                "The parser should have set the correct account-name");
        assertEquals(randomIp, cmdArgs.getLeft().ipAddress(),
                "The parser should have set the correct ip-address");
        assertEquals(randomPort, cmdArgs.getLeft().port(),
                "The parser should have set the correct port");

        assertEquals("", out.toString(), "There should not be any output from the atm!");
    }

    private String generateRandomIp() {
        int[] ipParts = new int[4];
        Random r = new Random();
        for (int i = 0; i < 4; i++) {
            ipParts[i] = r.nextInt(256);
        }
        return String.format("%d.%d.%d.%d", ipParts[0], ipParts[1], ipParts[2], ipParts[3]);
    }

    private int generateRandomPort() {
        return generateRandomPort(new Random());
    }

    private int generateRandomPort(Random r) {
        return r.nextInt(InputValidationUtil.MAX_VALID_PORT + InputValidationUtil.MIN_VALID_PORT) - InputValidationUtil.MIN_VALID_PORT;
    }

    private String generateRandomFileName(Random r, int length) {
        StringBuilder builder = new StringBuilder(length);

        while (builder.length() < length) {
            char c = (char) r.nextInt('z' + 1);
            if (isCharValidForFilename(c)) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private String generateRandomFileName() {
        Random r = new Random();
        int length = r.nextInt(100) + 1;
        String randomFileName;
        do {
            randomFileName = generateRandomFileName(r, length);
        } while (!InputValidationUtil.isValidFilename(randomFileName));
        return randomFileName;
    }


    @Test
    void testInvalidInputEmpty() {
        String[] argsEmptyAuthFile = new String[]{"-a", "bob", "-s", "-g"};
        String[] argsEmptyCardFile = new String[]{"-a", "bob", "-c", "-g"};
        String[] argsEmptyPort = new String[]{"-a", "bob", "-p", "-g"};
        String[] argsEmptyMode = new String[]{"-a", "bob"};
        String[] argsEmptyAccount = new String[]{"-ga"};
        String[] argsEmpty = new String[0];

        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(argsEmptyAuthFile);
        assertEquals(ATM_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should not accept invalid input: empty auth-file");

        cmdArgs = reader.parseArgs(argsEmptyCardFile);
        assertEquals(ATM_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should not accept invalid input: empty card-file");


        cmdArgs = reader.parseArgs(argsEmptyPort);
        assertEquals(ATM_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should not accept invalid input: empty port");

        cmdArgs = reader.parseArgs(argsEmptyMode);
        assertEquals(ATM_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should not accept invalid input: no mode of operation");

        cmdArgs = reader.parseArgs(argsEmptyAccount);
        assertEquals(ATM_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should not accept invalid input: empty account");

        cmdArgs = reader.parseArgs(argsEmpty);
        assertEquals(ATM_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should not accept invalid input: no input");

        assertEquals("", out.toString(), "There should not be any output from the atm!");
    }

    @Test
    void testInputMissingParameter() {
        String[] argsOnlyFile = new String[]{
                "-ga", "bob"};

        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(argsOnlyFile);
        assertEquals(0, cmdArgs.getRight().intValue(),
                "The parser should accept valid input: only account name and mode of operation");

        assertEquals(DEFAULT_ATM_PORT, cmdArgs.getLeft().port(),
                "The parser should set the default port");
        assertEquals(DEFAULT_ATM_IP, cmdArgs.getLeft().ipAddress(),
                "The parser should set the default ip-address");
        assertEquals("bob" + DEFAULT_ATM_CARD_FILE_EXTENSION, cmdArgs.getLeft().cardFile().getName(),
                "The parser should set the default card-file name");
        assertEquals(DEFAULT_BANK_AUTH_FILE, cmdArgs.getLeft().authFile().getName(),
                "The parser should set the default auth-file");

        assertEquals("", out.toString(), "There should not be any output from the atm!");
    }

    @Test
    void testInvalidInputDuplicatedParameter() {
        String[] argsDupAuthFile = new String[]{
                "-ga", "bob",
                "-s", authFileName,
                "-s", authFileName};
        String[] argsDupCardFile = new String[]{
                "-ga", "bob",
                "-s", authFileName,
                "-s", authFileName};

        String[] argsDupIP = new String[]{
                "-ga", "bob",
                "-i", generateRandomIp(),
                "-i", generateRandomIp()};


        String[] argsDupPort = new String[]{
                "-ga", "bob",
                "-p", "" + generateRandomPort(),
                "-p", "" + generateRandomPort()};

        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(argsDupAuthFile);
        assertEquals(ATM_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should no accept invalid input: duplicated auth-file");
        cmdArgs = reader.parseArgs(argsDupCardFile);
        assertEquals(ATM_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should no accept invalid input: duplicated card-file");
        cmdArgs = reader.parseArgs(argsDupIP);
        assertEquals(ATM_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should no accept invalid input: duplicated ip");
        cmdArgs = reader.parseArgs(argsDupPort);
        assertEquals(ATM_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should no accept invalid input: duplicated port");

        assertEquals("", out.toString(), "There should not be any output from the atm!");
    }

    @Test
    void testPosixComplianceMaxArgLength() {
        StringBuilder builder = new StringBuilder(4097);
        for (int i = 0; i < 4097; i++) {
            builder.append("a");
        }
        String[] args = new String[]{"-ga", "bob", "-s", builder.toString()};
        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(args);
        assertEquals(ATM_ERROR_CODE, cmdArgs.getRight().intValue(),
                "Providing args of size > 4096 should have returned error code Constants.ERROR_CODE_USER!");

        assertEquals("", out.toString(), "There should not be any output in case of a rejected input!");
    }

    @Test
    void testPosixComplianceMissingWhiteSpaces() {
        String randomCardFileName = cardFileName;
        String randomAuthFileName = authFileName;
        String randomIp = generateRandomIp();

        Random r = new Random();
        String randomAccountName = generateRandomFileName(r, r.nextInt(122) + 1);

        int randomPort = generateRandomPort();

        String[] args = new String[]{
                "-s" + randomAuthFileName,
                "-c" + randomCardFileName,
                "-p" + randomPort,
                "-i" + randomIp,
                "-a" + randomAccountName,
                "-g"};

        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(args);
        assertEquals(0, cmdArgs.getRight().intValue());

        assertEquals("", out.toString(), "There should not be any output by the atm!");
    }

}
