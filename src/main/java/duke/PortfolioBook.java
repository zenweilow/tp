package duke;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PortfolioBook {
    private final Map<String, Portfolio> portfolios;
    private String activePortfolioName;

    public PortfolioBook() {
        this.portfolios = new LinkedHashMap<>();
        this.activePortfolioName = null;
    }

    public void createPortfolio(String name) throws AppException {
        if (name == null || name.isBlank()) {
            throw new AppException("Portfolio name cannot be blank");
        }
        if (portfolios.containsKey(name)) {
            throw new AppException("Portfolio already exists: " + name);
        }

        portfolios.put(name, new Portfolio(name));

        if (activePortfolioName == null) {
            activePortfolioName = name;
        }
    }

    public void ensurePortfolioExists(String name) throws AppException {
        if (!portfolios.containsKey(name)) {
            createPortfolio(name);
        }
    }

    public void usePortfolio(String name) throws AppException {
        if (!portfolios.containsKey(name)) {
            throw new AppException("Portfolio not found: " + name);
        }
        activePortfolioName = name;
    }

    public boolean hasActivePortfolio() {
        return activePortfolioName != null;
    }

    public Portfolio getActivePortfolio() throws AppException {
        if (activePortfolioName == null) {
            throw new AppException("No active portfolio selected. Use: /use NAME");
        }
        return portfolios.get(activePortfolioName);
    }

    public String getActivePortfolioName() {
        return activePortfolioName;
    }

    public List<Portfolio> getPortfolios() {
        return new ArrayList<>(portfolios.values());
    }

    public Portfolio getPortfolio(String name) {
        return portfolios.get(name);
    }
}
