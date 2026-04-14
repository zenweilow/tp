# Elegazia - Project Portfolio Page

## Project: CG2StocksTracker

CG2StocksTracker is a desktop command-line portfolio tracker built for typing-oriented users who want to manage holdings quickly.
The user interacts with it through a CLI, and it is written in Java.

Given below are my contributions to the project.

---

### Key engineering contributions

* Implemented holding-level tracking and P&L computation:
  quantity, average buy price, and realized/unrealized gains across `/add`, `/remove`, and `/value`.
* Implemented weighted-average buy-price updates so repeated buys update cost basis correctly
  instead of overwriting previous position data.
* Ensured sell-flow logic computes realized gain against average cost and keeps remaining holdings
  internally consistent after partial disposals.
* Built and maintained the flag-based parser for `/add`, `/remove`, `/set`, `/list`, and `/value`
  with validation for required fields (for example `--type`, `--ticker`, `--qty`, `--price`) and normalization into `ParsedCommand`.
* Standardized parser error handling so malformed input fails early with actionable user-facing messages
  before command execution reaches model logic.
* Added transaction-fee support in `/add` and `/remove` via optional `--brokerage`, `--fx`, and `--platform` inputs.
* Integrated fee aggregation directly into transaction calculations so reported performance reflects
  realistic net outcomes rather than gross-only values.
* Refactored parser and related tests as command signatures evolved to preserve behavior across parser, model, and UI layers.

### Design and maintainability impact

* Reduced parser-execution coupling by consistently routing validated user input through `ParsedCommand`.
* Improved long-term maintainability by keeping command contracts aligned across parser, execution, tests, and docs.
* Lowered regression risk during feature expansion by updating tests in parallel with signature or behavior changes.

---

### Bug fixes and code quality

* Fixed acceptance of blank names in `/create` and `/use` by introducing `validatePortfolioName()`
  (rejects null, blank, and `/`-prefixed names), with regression tests for both commands.
* Added assertions across `Parser`, `Portfolio`, `PortfolioBook`, and `Storage`
  to enforce preconditions, postconditions, and invariants.
* Introduced structured logging in `CG2StocksTracker` and `Storage` for diagnostics,
  while suppressing default console log spam with `LogManager.reset()`.
* Improved readability through SLAP-driven refactoring and a shared `Ui.printFormatted()` helper
  to remove repetitive print patterns.

---

### Code contributed

* [RepoSense contribution link](https://nus-cs2113-ay2526-s2.github.io/tp-dashboard/?search=w09-4&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~other~test-code&since=2026-02-20T00%3A00%3A00&filteredFileName=&tabOpen=true&tabType=authorship&tabAuthor=Elegazia&tabRepo=AY2526S2-CS2113-W09-4%2Ftp%5Bmaster%5D&authorshipIsMergeGroup=false&authorshipFileTypes=docs~functional-code~other~test-code&authorshipIsBinaryFileTypeChecked=false&authorshipIsIgnoredFilesChecked=false)

---

### Documentation

* Updated the User Guide command formats and examples for final flag-based syntax
    (`/add`, `/remove`, `/set`, `/list`, `/value`) including filtered listing and portfolio-summary options.
* Updated the Developer Guide to match the current parser-to-execution flow
    (`Parser` -> `ParsedCommand` -> `CG2StocksTracker.execute`) and refreshed UML sequence diagrams for `/set`, `/value`, and `/list`.
* Reviewed wording and examples to remove outdated command signatures so documentation remains consistent
  with the shipped product behavior.

---

### Testing

* Added regression tests for blank-name validation in `ParserTest` (`parseCreate_withBlankQuotedName_throws`, `parseUse_withBlankQuotedName_throws`).
* Updated `UiTest` assertions to match spelling changes (e.g. "Unrealised").
* Regenerated `text-ui-test/EXPECTED.TXT` to stay in sync with final output and messaging changes.

---

### Contributions to team-based tasks

* Helped prepare and release `v1.0`.
* Supported team integration during refactoring-heavy phases.
* Contributed to task coordination and keeping the team aligned with deadlines.

---

### Review and mentoring contributions

* Reviewed teammates' pull requests and provided feedback on command behaviour and parser consistency.
* Helped troubleshoot parsing issues and integration bugs.
* Flagged edge cases during reviews (invalid options, missing required flags, blank inputs)
  to improve robustness before merge.

---

### Contributions beyond the project team

* Shared implementation and refactoring insights with peers where relevant.
