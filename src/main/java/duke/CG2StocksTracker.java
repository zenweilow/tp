package duke;

import java.nio.file.Path;

public class CG2StocksTracker {
    private static final String FILE_PATH = "data/CG2StocksTracker.txt";

    private final Ui ui;
    private final Parser parser;
    private final Storage storage;
    private final PortfolioBook portfolioBook;

    public CG2StocksTracker(String filePath) {
        this.ui = new Ui();
        this.parser = new Parser();
        this.storage = new Storage(filePath);

        PortfolioBook loadedBook;
        try {
            loadedBook = storage.load();
        } catch (AppException e) {
            ui.showMessage("Storage load failed: " + e.getMessage());
            loadedBook = new PortfolioBook();
        }
        this.portfolioBook = loadedBook;
    }

    public static void main(String[] args) {
        new CG2StocksTracker(FILE_PATH).run();
    }

    public void run() {
        ui.showWelcome();

        while (true) {
            String input = ui.readCommand();

            try {
                ParsedCommand command = parser.parse(input);
                ui.beginResponse();
                boolean shouldContinue = execute(command);
                ui.endResponse();
                if (!shouldContinue) {
                    break;
                }
            } catch (AppException e) {
                ui.beginResponse();
                ui.showError(e.getMessage());
                ui.endResponse();
            }
        }
    }

    private boolean execute(ParsedCommand command) throws AppException {
        switch (command.type()) {
        case CREATE:
            handleCreate(command);
            return true;
        case USE:
            handleUse(command);
            return true;
        case LIST:
            handleList(command);
            return true;
        case ADD:
            handleAdd(command);
            return true;
        case REMOVE:
            handleRemove(command);
            return true;
        case SET:
            handleSet(command);
            return true;
        case SET_MANY:
            handleSetMany(command);
            return true;
        case VALUE:
            handleValue();
            return true;
        case INSIGHTS:
            handleInsights(command);
            return true;
        case HELP:
            ui.showHelp();
            return true;
        case EXIT:
            ui.showGoodbye();
            return false;
        default:
            throw new AppException("Unknown command type: " + command.type());
        }
    }

    private void handleCreate(ParsedCommand command) throws AppException {
        String name = command.name();
        portfolioBook.createPortfolio(name);
        save();
        ui.showMessage("Created portfolio: " + name);
        ui.showMessage("Active portfolio: " + portfolioBook.getActivePortfolioName());
    }

    private void handleUse(ParsedCommand command) throws AppException {
        String name = command.name();
        portfolioBook.usePortfolio(name);
        ui.showMessage("Active portfolio: " + name);
    }

    private void handleList(ParsedCommand command) throws AppException {
        String target = command.listTarget();

        if ("--portfolios".equals(target)) {
            ui.showPortfolioSummaries(portfolioBook);
            return;
        }

        if ("--stock".equals(target)) {
            ui.showHoldings(portfolioBook.getActivePortfolio(), AssetType.STOCK);
            return;
        }

        if ("--etf".equals(target)) {
            ui.showHoldings(portfolioBook.getActivePortfolio(), AssetType.ETF);
            return;
        }

        if ("--bond".equals(target)) {
            ui.showHoldings(portfolioBook.getActivePortfolio(), AssetType.BOND);
            return;
        }

        if (portfolioBook.hasActivePortfolio()) {
            ui.showHoldings(portfolioBook.getActivePortfolio());
        } else {
            ui.showPortfolios(portfolioBook);
        }
    }

    private void handleAdd(ParsedCommand command) throws AppException {
        Portfolio portfolio = portfolioBook.getActivePortfolio();
        AssetType type = command.assetType();
        String ticker = command.ticker();
        double qty = command.quantity();
        double price = command.price();
        double fees = command.totalFees();

        boolean existed = portfolio.hasHolding(type, ticker);
        double newQty = portfolio.addHolding(type, ticker, qty, price, fees);
        save();

        Holding holding = portfolio.getHolding(type, ticker);
        if (existed) {
            ui.showMessage("Updated holding quantity: " + ticker + " (" + type.toDisplay() + ") = "
                    + Ui.formatNumber(newQty)
                    + ", avg buy price = " + Ui.formatMoney(holding.getAverageBuyPrice()));
        } else {
            ui.showAddedHolding(holding);
        }
    }

    private void handleRemove(ParsedCommand command) throws AppException {
        Portfolio portfolio = portfolioBook.getActivePortfolio();
        AssetType type = command.assetType();
        String ticker = command.ticker();
        Double qty = command.quantity();
        Double price = command.price();
        double fees = command.totalFees();

        Portfolio.RemoveResult result;
        try {
            result = portfolio.removeHolding(type, ticker, qty, price, fees);
        } catch (IllegalArgumentException e) {
            throw new AppException(e.getMessage());
        }
        save();
        String feeText = result.fees() > 0 ? ", fees = " + Ui.formatMoney(result.fees()) : "";
        ui.showMessage("Sold " + Ui.formatNumber(result.soldQuantity())
            + " of " + ticker + " (" + type.toDisplay() + ") at " + Ui.formatMoney(result.soldPrice())
            + feeText + ", realized P&L = " + Ui.formatSignedMoney(result.realizedPnl()));
    }

    private void handleSet(ParsedCommand command) throws AppException {
        Portfolio portfolio = portfolioBook.getActivePortfolio();
        String ticker = command.ticker();
        double price = command.price();

        int updatedCount = portfolio.setPriceForTicker(ticker, price);
        if (updatedCount == 0) {
            throw new AppException("Holding not found for ticker: " + ticker);
        }

        save();
        ui.showMessage("Updated price: " + ticker + " = " + Ui.formatMoney(price));
    }

    private void handleSetMany(ParsedCommand command) throws AppException {
        Portfolio portfolio = portfolioBook.getActivePortfolio();
        Path filePath = command.filePath();

        Storage.BulkUpdateResult result = storage.loadPriceUpdates(filePath, portfolio);
        save();

        ui.showBulkUpdateResult(result);
    }

    private void handleValue() throws AppException {
        Portfolio portfolio = portfolioBook.getActivePortfolio();
        ui.showPortfolioValue(portfolio);
    }

    private void handleInsights(ParsedCommand command) throws AppException {
        InsightsOptions options = parseInsightsOptions(command.listTarget());
        Portfolio portfolio = portfolioBook.getActivePortfolio();
        ui.showInsightsTable(portfolio, options.filterType(), options.topN());
    }

    private InsightsOptions parseInsightsOptions(String rawOptions) throws AppException {
        if (rawOptions == null || rawOptions.isBlank()) {
            return new InsightsOptions(null, null);
        }

        String[] tokens = rawOptions.trim().split("\\s+");
        AssetType filterType = null;
        Integer topN = null;

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].toLowerCase();

            switch (token) {
            case "--type":
                if (i + 1 >= tokens.length) {
                    throw new AppException("Usage: /insights [--type stock|etf|bond] [--top N]");
                }
                try {
                    filterType = AssetType.fromString(tokens[++i]);
                } catch (IllegalArgumentException e) {
                    throw new AppException(e.getMessage());
                }
                break;
            case "--top":
                if (i + 1 >= tokens.length) {
                    throw new AppException("Usage: /insights [--type stock|etf|bond] [--top N]");
                }
                try {
                    topN = Integer.parseInt(tokens[++i]);
                } catch (NumberFormatException e) {
                    throw new AppException("Top count must be a positive integer.");
                }
                if (topN <= 0) {
                    throw new AppException("Top count must be a positive integer.");
                }
                break;
            default:
                throw new AppException("Unknown /insights option: " + tokens[i]
                        + "\nUsage: /insights [--type stock|etf|bond] [--top N]");
            }
        }

        return new InsightsOptions(filterType, topN);
    }

    private record InsightsOptions(AssetType filterType, Integer topN) {
    }

    private void save() throws AppException {
        storage.save(portfolioBook);
    }
}
