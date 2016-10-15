package ca.jbrains.pos.test;

import ca.jbrains.pos.FireTextCommands;
import ca.jbrains.pos.TextCommandListener;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.auto.Auto;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FireTextCommandsWithBufferedReader {
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Mock
    private TextCommandListener textCommandListener;

    @Auto
    private Sequence commandsSequence;
    private FireTextCommands fireTextCommands;

    // REFACTOR Move to a reusable library for test data.
    public static List<String> nLinesLike(int n, String formatDescription) {
        return IntStream.range(1, n).mapToObj(
                (each) -> String.format(formatDescription, each)
        ).collect(Collectors.toList());
    }

    // REFACTOR Move to generic text library
    public static String unlines(List<String> lines) {
        return lines.stream().reduce("", (sum, each) -> sum + each + System.lineSeparator());
    }

    @Before
    public void setUp() throws Exception {
        fireTextCommands = new FireTextCommands(textCommandListener);
    }

    @Test
    public void onlyAlreadyCleanNonEmptyCommands() throws Exception {
        // Commands are "valid" if they have neither leading
        // nor trailing whitespace. By implication, this means
        // that they are not empty, either.
        context.checking(new Expectations() {{
            nLinesLike(5, "::valid command %d::").stream().forEach(
                    (eachCommand) -> {
                        oneOf(textCommandListener).onCommand(eachCommand);
                        inSequence(commandsSequence);
                    }
            );
        }});

        fireTextCommands.consumeText(new StringReader(
                unlines(nLinesLike(5, "::valid command %d::"))
        ));
    }

    @Test
    public void trimEmptyCommands() throws Exception {
        context.checking(new Expectations() {{
            exactly(7).of(textCommandListener).onCommand("");
        }});

        fireTextCommands.consumeText(new StringReader(
                unlines(Arrays.asList(
                        "",
                        "\t",
                        "\r",
                        "\f",
                        "\u000B",
                        " ",
                        " \t  \f  \u000B   "
                ))
        ));
    }

    @Test
    public void trimWhitespaceFromNonEmptyCommands() throws Exception {
        context.checking(new Expectations() {{
            oneOf(textCommandListener).onCommand("::command\twith\finterior\u000Bspaces::");
        }});

        fireTextCommands.consumeText(new StringReader(
                "  \t  \f  \u000B  \t ::command\twith\finterior\u000Bspaces::\t\t \f  \t   \u000B  \f \t  "
        ));
    }
}
