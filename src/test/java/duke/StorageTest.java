package duke;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class StorageTest {
    @Test
    void load_emptyFile_returnsEmptyPortfolioBook(@TempDir Path tempDir) throws AppException {
        Path file = tempDir.resolve("data.txt");
        Storage storage = new Storage(file.toString());

        PortfolioBook book = storage.load();

        assertTrue(book.getPortfolios().isEmpty());
        assertFalse(book.hasActivePortfolio());
    }

    @Test
    void save_thenLoad_restoresPortfolioData(@TempDir Path tempDir) throws AppException {
        Path file = tempDir.resolve("data.txt");
        Storage storage = new Storage(file.toString());

        PortfolioBook book = new PortfolioBook();
        book.createPortfolio("tech");
        book.usePortfolio("tech");

        Portfolio portfolio = book.getActivePortfolio();
        portfolio.addHolding(AssetType.STOCK, "AAPL", 10, 150, 25);

        storage.save(book);

        PortfolioBook loaded = storage.load();

        assertEquals("tech", loaded.getActivePortfolioName());
        Portfolio loadedPortfolio = loaded.getPortfolio("tech");

        assertNotNull(loadedPortfolio);
        assertTrue(loadedPortfolio.hasHolding(AssetType.STOCK, "AAPL"));
        assertEquals(152.5, loadedPortfolio.getHolding(AssetType.STOCK, "AAPL").getAverageBuyPrice());
    }

    @Test
    void save_thenLoad_restoresFeeAdjustedRealizedPnl(@TempDir Path tempDir) throws AppException {
        Path file = tempDir.resolve("data.txt");
        Storage storage = new Storage(file.toString());

        PortfolioBook book = new PortfolioBook();
        book.createPortfolio("income");
        book.usePortfolio("income");

        Portfolio portfolio = book.getActivePortfolio();
        portfolio.addHolding(AssetType.ETF, "QQQ", 2, 400, 20);
        portfolio.removeHolding(AssetType.ETF, "QQQ", 1.0, 600.0, 15);

        storage.save(book);

        PortfolioBook loaded = storage.load();
        Portfolio loadedPortfolio = loaded.getPortfolio("income");

        assertNotNull(loadedPortfolio);
        assertEquals(175.0, loadedPortfolio.getTotalRealizedPnl());
        assertEquals(410.0, loadedPortfolio.getHolding(AssetType.ETF, "QQQ").getAverageBuyPrice());
    }

    @Test
    void loadPriceUpdates_validCsv_updatesPrices(@TempDir Path tempDir) throws Exception {
        Path storageFile = tempDir.resolve("data.txt");
        Storage storage = new Storage(storageFile.toString());

        Portfolio portfolio = new Portfolio("test");
        portfolio.addHolding(AssetType.STOCK, "AAPL", 5, 180, 0);

        Path csv = tempDir.resolve("prices.csv");

        Files.write(csv, List.of(
                "ticker,price",
                "AAPL,200"
        ));

        Storage.BulkUpdateResult result = storage.loadPriceUpdates(csv, portfolio);

        assertEquals(1, result.successCount());
        assertEquals(0, result.failedCount());

        Holding holding = portfolio.getHolding(AssetType.STOCK, "AAPL");
        assertEquals(200, holding.getLastPrice());
    }

    @Test
    void loadPriceUpdates_invalidRow_recordsFailure(@TempDir Path tempDir) throws Exception {
        Path storageFile = tempDir.resolve("data.txt");
        Storage storage = new Storage(storageFile.toString());

        Portfolio portfolio = new Portfolio("test");
        portfolio.addHolding(AssetType.STOCK, "AAPL", 5, 180, 0);

        Path csv = tempDir.resolve("prices.csv");

        Files.write(csv, List.of(
                "ticker,price",
                "AAPL,abc"
        ));

        Storage.BulkUpdateResult result = storage.loadPriceUpdates(csv, portfolio);

        assertEquals(0, result.successCount());
        assertEquals(1, result.failedCount());
    }

    @Test
    void loadPriceUpdates_missingFile_throwsException(@TempDir Path tempDir) {
        Storage storage = new Storage(tempDir.resolve("data.txt").toString());

        Portfolio portfolio = new Portfolio("test");

        Path missing = tempDir.resolve("missing.csv");

        assertThrows(AppException.class, () ->
                storage.loadPriceUpdates(missing, portfolio)
        );
    }

    @Test
    void quarantineCorruptedStorageFile_preservesBadContentsAndResetsPrimaryFile(@TempDir Path tempDir)
            throws Exception {
        Path file = tempDir.resolve("data.txt");
        Storage storage = new Storage(file.toString());
        List<String> corruptedContents = List.of(
                "ACTIVE|main",
                "broken line with no separators"
        );
        Files.write(file, corruptedContents);

        Path quarantined = storage.quarantineCorruptedStorageFile();

        assertTrue(Files.exists(quarantined));
        assertEquals(corruptedContents, Files.readAllLines(quarantined));
        assertTrue(Files.exists(file));
        assertTrue(Files.readAllLines(file).isEmpty());
    }

    @Test
    void save_afterQuarantine_doesNotOverwritePreservedCorruptedContents(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("data.txt");
        Storage storage = new Storage(file.toString());
        List<String> corruptedContents = List.of(
                "ACTIVE|main",
                "PORTFOLIO|growth|abc"
        );
        Files.write(file, corruptedContents);

        Path quarantined = storage.quarantineCorruptedStorageFile();

        PortfolioBook book = new PortfolioBook();
        book.createPortfolio("newport");
        book.usePortfolio("newport");
        storage.save(book);

        assertEquals(corruptedContents, Files.readAllLines(quarantined));
        List<String> savedLines = Files.readAllLines(file);
        assertTrue(savedLines.contains("ACTIVE|newport"));
        assertTrue(savedLines.contains("PORTFOLIO|newport|0.0"));
    }

    @Test
    void saveWatchlist_thenLoadWatchlist_restoresEntries(@TempDir Path tempDir) throws AppException {
        Path file = tempDir.resolve("data.txt");
        Storage storage = new Storage(file.toString());

        Watchlist watchlist = new Watchlist();
        watchlist.addItem(AssetType.STOCK, "AAPL", 220.0);
        watchlist.addItem(AssetType.ETF, "QQQ", null);

        storage.saveWatchlist(watchlist);
        Watchlist loaded = storage.loadWatchlist();

        assertTrue(loaded.hasItem(AssetType.STOCK, "AAPL"));
        assertTrue(loaded.hasItem(AssetType.ETF, "QQQ"));
        assertEquals(220.0, loaded.getItem(AssetType.STOCK, "AAPL").targetPrice());
        assertNull(loaded.getItem(AssetType.ETF, "QQQ").targetPrice());
    }

    @Test
    void loadWatchlist_corruptedFile_throwsException(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("data.txt");
        Storage storage = new Storage(file.toString());

        Path watchlistFile = tempDir.resolve("data.txt.watchlist");
        Files.write(watchlistFile, List.of("WATCH|STOCK|AAPL|-10"));

        assertThrows(AppException.class, storage::loadWatchlist);
    }
}
