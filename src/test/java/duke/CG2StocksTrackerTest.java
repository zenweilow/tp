package duke;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CG2StocksTrackerTest {
    @Test
    void run_watchBuy_mentionsRemovalFromWatchlist() {
        String input = String.join(System.lineSeparator(),
                "/create main",
                "/watch add --type etf --ticker QQQ --price 450",
                "/watch buy --type etf --ticker QQQ --qty 2 --portfolio main",
                "/exit"
        ) + System.lineSeparator();

        ByteArrayInputStream testIn = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream testOut = new ByteArrayOutputStream();
        java.io.InputStream originalIn = System.in;
        PrintStream originalOut = System.out;

        try {
            System.setIn(testIn);
            System.setOut(new PrintStream(testOut, true, StandardCharsets.UTF_8));
            new CG2StocksTracker("build/test-data/cg2stockstracker-test-data.txt").run();
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }

        String output = testOut.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Removed from watchlist."));
    }
}
