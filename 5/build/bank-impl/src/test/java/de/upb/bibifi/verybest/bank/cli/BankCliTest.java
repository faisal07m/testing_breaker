package de.upb.bibifi.verybest.bank.cli;

import de.upb.bibifi.verybest.common.Constants;
import de.upb.bibifi.verybest.common.util.InputValidationUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Random;

import static de.upb.bibifi.verybest.common.util.InputValidationUtil.*;
import static org.junit.jupiter.api.Assertions.*;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BankCliTest {

    private static final int BANK_ERROR_CODE = Constants.ERROR_CODE_USER;
    private static final String DEFAULT_BANK_AUTH_FILE = "bank.auth";
    private static final int BANK_DEFAULT_PORT = 3000;

    private CommandLineReader reader;

    private PrintStream systemOut;
    private PrintStream systemErr;
    private OutputStream out;
    private OutputStream err;


    @BeforeAll
    void init() {
        systemOut = System.out;
        systemErr = System.err;
    }

    @AfterAll
    void cleanUp() {
        System.setOut(systemOut);
        System.setErr(systemErr);
    }

    @BeforeEach
    void initEach() {
        reader = new CommandLineReaderImpl();

        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
    }

    @Test
    void testInvalidFileNameRegex() {
        StringBuilder builder = new StringBuilder(4097);
        for (int i = 0; i <= MAX_FILENAME_LENGTH; i++) {
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
        String randomFileName = generateRandomFileName();

        int randomPort = generateRandomPort();

        systemOut.println("Filename: " + randomFileName);

        String[] args = new String[]{
                "-s", randomFileName,
                "-p", "" + randomPort};
        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(args);
        assertEquals(0, cmdArgs.getRight().intValue(), "The parser should accept valid");
        assertNotNull(cmdArgs.getLeft().authFile(), "The parser should have an output file");
        assertEquals(randomFileName, cmdArgs.getLeft().authFile().getName(),
                "The parser should have set the correct output file");
        assertEquals(randomPort, cmdArgs.getLeft().port(),
                "The parser should have set the correct port");
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
        int length = r.nextInt(MAX_FILENAME_LENGTH) + 1;
        systemOut.println("Creating filename of size " + length);
        String randomFileName;
        do {
            randomFileName = generateRandomFileName(r, length);
        } while (!InputValidationUtil.isValidFilename(randomFileName));
        return randomFileName;
    }


    @Test
    void testInvalidInputEmpty() {
        String[] argsEmptyFile = new String[]{"-s", "-p", "" + generateRandomPort()};
        String[] argsEmptyPort = new String[]{"-p", "-s", generateRandomFileName()};

        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(argsEmptyFile);
        assertEquals(BANK_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should not accept invalid input: empty file");

        cmdArgs = reader.parseArgs(argsEmptyPort);
        assertEquals(BANK_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should not accept invalid input: empty port");
    }

    @Test
    void testInputMissingParameterEmpty() {
        String[] argsEmpty = new String[0];

        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(argsEmpty);
        assertEquals(0, cmdArgs.getRight().intValue(),
                "The parser should accept valid input: only auth file");
        assertEquals(BANK_DEFAULT_PORT, cmdArgs.getLeft().port(),
                "The parser should accept valid input: only auth file");
        assertEquals(DEFAULT_BANK_AUTH_FILE, cmdArgs.getLeft().authFile().getName(),
                String.format("The parser should set auth file to default value \"%s\"", DEFAULT_BANK_AUTH_FILE));
    }

    @Test
    void testInputMissingParameterOnlyAuthfile() {
        String[] argsOnlyFile = new String[]{
                "-s", generateRandomFileName()};


        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(argsOnlyFile);
        assertEquals(0, cmdArgs.getRight().intValue(),
                "The parser should accept valid input: only auth file");
        assertEquals(BANK_DEFAULT_PORT, cmdArgs.getLeft().port(),
                "The parser should accept valid input: only auth file");
    }

    @Test
    void testInputMissingParameterOnlyPort() {
        String[] argsOnlyPort = new String[]{
                "-p", "" + generateRandomPort()};


        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(argsOnlyPort);
        assertEquals(0, cmdArgs.getRight().intValue(),
                "The parser should accept valid input: only port");
        assertEquals(DEFAULT_BANK_AUTH_FILE, cmdArgs.getLeft().authFile().getName(),
                String.format("The parser should set auth file to default value \"%s\"", DEFAULT_BANK_AUTH_FILE));
    }

    @Test
    void testInvalidInputDuplicatedParameterAuthfile() {
        String[] argsDupFile = new String[]{
                "-s", generateRandomFileName(),
                "-s", generateRandomFileName(),
                "-p", "" + generateRandomPort()};

        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(argsDupFile);
        assertEquals(BANK_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should no accept invalid input: duplicated file");
    }

    @Test
    void testInvalidInputDuplicatedParameterPort() {
        String[] argsDupPort = new String[]{
                "-p", "" + generateRandomPort(),
                "-p", "" + generateRandomPort(),
                "-s", generateRandomFileName()};

        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(argsDupPort);
        assertEquals(BANK_ERROR_CODE, cmdArgs.getRight().intValue(),
                "The parser should no accept invalid input: duplicated port");
    }

    @Test
    void testPosixComplianceMaxArgLength() {
        StringBuilder builder = new StringBuilder(4097);
        for (int i = 0; i < 4097; i++) {
            builder.append("a");
        }
        String[] args = new String[]{"-s", builder.toString()};
        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(args);
        assertEquals(BANK_ERROR_CODE, cmdArgs.getRight().intValue(),
                "Providing args of size > 4096 should have returned error code Constants.ERROR_CODE_USER!");
    }

    @Test
    void testPosixComplianceWhiteSpacesPort() {
        int port = generateRandomPort();
        String[] args1 = new String[]{"-p" + port};
        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(args1);
        assertEquals(0, cmdArgs.getRight().intValue(), "-p" + port + " should not fail");

        assertEquals(port, cmdArgs.getLeft().port(),
                "The parser should set the correct port");
    }


    @Test
    void testPosixComplianceWhiteSpacesAuthfile() {
        String fileName = generateRandomFileName();
        String[] args2 = new String[]{"-s" + fileName};

        Pair<CommandLineArgs, Integer> cmdArgs = reader.parseArgs(args2);
        assertEquals(0, cmdArgs.getRight().intValue(), "-s" + fileName + " should not fail");

        assertEquals(fileName, cmdArgs.getLeft().authFile().getName(),
                "The parser should set the correct auth-file");
    }

}
