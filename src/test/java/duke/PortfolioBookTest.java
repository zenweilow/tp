package duke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PortfolioBookTest {
    @Test
    void createPortfolio_blankName_throws() {
        PortfolioBook book = new PortfolioBook();

        AppException ex = assertThrows(AppException.class, () -> book.createPortfolio("   "));

        assertEquals("Portfolio name cannot be blank", ex.getMessage());
    }
}
