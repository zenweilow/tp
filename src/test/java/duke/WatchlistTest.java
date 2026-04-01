package duke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class WatchlistTest {
    @Test
    void addItem_withOptionalPrice_storesWatchlistItem() {
        Watchlist watchlist = new Watchlist();

        watchlist.addItem(AssetType.STOCK, "aapl", null);
        watchlist.addItem(AssetType.ETF, "qqq", 450.0);

        assertTrue(watchlist.hasItem(AssetType.STOCK, "AAPL"));
        assertTrue(watchlist.hasItem(AssetType.ETF, "QQQ"));

        WatchlistItem item = watchlist.getItem(AssetType.ETF, "QQQ");
        assertNotNull(item);
        assertEquals(450.0, item.targetPrice());
    }

    @Test
    void addItem_duplicate_throwsException() {
        Watchlist watchlist = new Watchlist();
        watchlist.addItem(AssetType.STOCK, "AAPL", 200.0);

        assertThrows(IllegalArgumentException.class, () ->
                watchlist.addItem(AssetType.STOCK, "AAPL", 210.0));
    }

    @Test
    void removeItem_existingItem_returnsTrue() {
        Watchlist watchlist = new Watchlist();
        watchlist.addItem(AssetType.STOCK, "MSFT", 300.0);

        boolean removed = watchlist.removeItem(AssetType.STOCK, "MSFT");

        assertTrue(removed);
        assertFalse(watchlist.hasItem(AssetType.STOCK, "MSFT"));
    }

    @Test
    void buyItem_withPriceAndPortfolio_addsHoldingAndRemovesItem() throws AppException {
        Watchlist watchlist = new Watchlist();
        watchlist.addItem(AssetType.STOCK, "VOO", 600.0);

        PortfolioBook portfolioBook = new PortfolioBook();
        portfolioBook.createPortfolio("growth");

        Watchlist.BuyResult result = watchlist.buyItem(AssetType.STOCK, "VOO", "growth", portfolioBook);

        Portfolio portfolio = portfolioBook.getPortfolio("growth");
        assertNotNull(portfolio);
        assertTrue(portfolio.hasHolding(AssetType.STOCK, "VOO"));
        assertEquals(1.0, portfolio.getHolding(AssetType.STOCK, "VOO").getQuantity());
        assertEquals(600.0, portfolio.getHolding(AssetType.STOCK, "VOO").getAverageBuyPrice());
        assertFalse(watchlist.hasItem(AssetType.STOCK, "VOO"));
        assertEquals("growth", result.portfolioName());
        assertEquals(1.0, result.boughtQuantity());
        assertEquals(1.0, result.resultingQuantity());
    }

    @Test
    void buyItem_withoutPrice_throwsException() throws AppException {
        Watchlist watchlist = new Watchlist();
        watchlist.addItem(AssetType.STOCK, "VOO", null);

        PortfolioBook portfolioBook = new PortfolioBook();
        portfolioBook.createPortfolio("growth");

        assertThrows(IllegalArgumentException.class, () ->
                watchlist.buyItem(AssetType.STOCK, "VOO", "growth", portfolioBook));
    }

    @Test
    void buyItem_missingPortfolio_throwsException() {
        Watchlist watchlist = new Watchlist();
        watchlist.addItem(AssetType.STOCK, "VOO", 600.0);

        PortfolioBook portfolioBook = new PortfolioBook();

        assertThrows(IllegalArgumentException.class, () ->
                watchlist.buyItem(AssetType.STOCK, "VOO", "missing", portfolioBook));
    }
}
