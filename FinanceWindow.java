import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Main UI window for the Personal Finance & Investment Manager.
 *
 * Design language: Apple-inspired — white surfaces, soft shadows, SF-style
 * typography hierarchy, rounded cards, vibrant accent colors.
 *
 * Architecture: View layer in MVC. Communicates with the persistence layer
 * exclusively through {@link FinanceFileHandler}.
 *
 * <p><b>v3 changes:</b>
 * <ul>
 *   <li>Category list is now properly separated per transaction type
 *       (Income / Expense / Investment), each backed by its own file.</li>
 *   <li>Switching the Type combo instantly swaps the category combo contents
 *       to show only the categories relevant to that type.</li>
 *   <li>The "+" button adds a new category only to the currently selected
 *       type's file and list.</li>
 * </ul>
 */
public class FinanceWindow extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG             = new Color(0xF5F5F7);
    private static final Color SURFACE        = Color.WHITE;
    private static final Color ACCENT_GREEN   = new Color(0x34C759);
    private static final Color ACCENT_RED     = new Color(0xFF3B30);
    private static final Color ACCENT_BLUE    = new Color(0x007AFF);
    private static final Color TEXT_PRIMARY   = new Color(0x1C1C1E);
    private static final Color TEXT_SECONDARY = new Color(0x6E6E73);
    private static final Color BORDER_COLOR   = new Color(0xE5E5EA);

    /** Vibrant slice colors for the donut chart (10 distinct hues). */
    private static final Color[] CHART_COLORS = {
        new Color(0xFF3B30), new Color(0xFF9500), new Color(0xFFCC00),
        new Color(0x34C759), new Color(0x007AFF), new Color(0xAF52DE),
        new Color(0xFF2D55), new Color(0x5AC8FA), new Color(0x4CD964),
        new Color(0xFF6B6B)
    };

    // ── Formatters ────────────────────────────────────────────────────────────
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm");
    private static final java.text.NumberFormat CURRENCY =
            java.text.NumberFormat.getCurrencyInstance(new Locale("en", "US"));

    // ── MVC References ────────────────────────────────────────────────────────
    private final FinanceFileHandler fileHandler = new FinanceFileHandler();
    private final FinanceService     service     = new FinanceService(fileHandler);

    // ── Dashboard labels ──────────────────────────────────────────────────────
    private JLabel balanceValueLabel;
    private JLabel expenseValueLabel;
    private JLabel investValueLabel;

    // ── Chart carousel ────────────────────────────────────────────────────────
    private CardLayout      chartCardLayout;
    private JPanel          chartCarousel;
    private DonutChartPanel donutChart;
    private BarChartPanel   barChart;
    private JLabel          carouselTitle;
    private int             carouselIndex = 0;
    private static final String[] CAROUSEL_TITLES =
            {"Expenses by Category", "Monthly Bar Overview"};

    // ── Form widgets ──────────────────────────────────────────────────────────
    private JComboBox<String>            typeCombo;
    private JComboBox<String>            categoryCombo;
    /** Backing model for the category combo — swapped when Type changes. */
    private DefaultComboBoxModel<String> categoryModel;
    private JTextField                   amountField;

    // ── Table ─────────────────────────────────────────────────────────────────
    private DefaultTableModel tableModel;

    // ── Constructor ───────────────────────────────────────────────────────────

    public FinanceWindow() {
        super("Finance Manager");
        applySystemLook();
        buildFrame();
        refreshAll();
        setVisible(true);
    }

    // ── Look & Feel ───────────────────────────────────────────────────────────

    private void applySystemLook() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        UIManager.put("Panel.background",      BG);
        UIManager.put("ScrollPane.background", BG);
        UIManager.put("Viewport.background",   BG);
    }

    // ── Frame scaffold ────────────────────────────────────────────────────────

    private void buildFrame() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 820));
        setPreferredSize(new Dimension(1280, 900));
        setBackground(BG);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(28, 28, 28, 28));

        root.add(buildHeader());
        root.add(vGap(18));
        root.add(buildDashboard());
        root.add(vGap(18));
        root.add(buildMainContent());
        root.add(vGap(18));
        root.add(buildTableSection());

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG);
        setContentPane(scroll);
        pack();
        setLocationRelativeTo(null);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JLabel title = new JLabel("Finance Manager");
        title.setFont(new Font("SF Pro Display", Font.BOLD, 28));
        title.setForeground(TEXT_PRIMARY);
        p.add(title, BorderLayout.WEST);

        JLabel sub = new JLabel("Personal Finance & Investment Tracker");
        sub.setFont(new Font("SF Pro Text", Font.PLAIN, 14));
        sub.setForeground(TEXT_SECONDARY);
        p.add(sub, BorderLayout.EAST);
        return p;
    }

    // ── Dashboard cards ───────────────────────────────────────────────────────

    private JPanel buildDashboard() {
        JPanel p = new JPanel(new GridLayout(1, 3, 16, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel balanceCard = makeCard();
        balanceValueLabel = makeValueLabel("$0.00", ACCENT_BLUE);
        balanceCard.add(makeCardContent("Total Balance", balanceValueLabel, "💰"));

        JPanel expenseCard = makeCard();
        expenseValueLabel = makeValueLabel("$0.00", ACCENT_RED);
        expenseCard.add(makeCardContent("Total Expenses", expenseValueLabel, "📉"));

        JPanel investCard = makeCard();
        investValueLabel = makeValueLabel("$0.00", ACCENT_GREEN);
        investCard.add(makeCardContent("Total Invested", investValueLabel, "📈"));

        p.add(balanceCard);
        p.add(expenseCard);
        p.add(investCard);
        return p;
    }

    private JPanel makeCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 6; i >= 1; i--) {
                    g2.setColor(new Color(0, 0, 0, 5 * i));
                    g2.fillRoundRect(i, i + 2, getWidth() - i * 2, getHeight() - i * 2, 20, 20);
                }
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 22, 18, 22));
        return card;
    }

    private JPanel makeCardContent(String title, JLabel valueLabel, String icon) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel iconLbl = new JLabel(icon + "  " + title);
        iconLbl.setFont(new Font("SF Pro Text", Font.PLAIN, 13));
        iconLbl.setForeground(TEXT_SECONDARY);
        iconLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(iconLbl);
        p.add(vGap(8));
        p.add(valueLabel);
        return p;
    }

    private JLabel makeValueLabel(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SF Pro Display", Font.BOLD, 26));
        lbl.setForeground(color);
        return lbl;
    }

    // ── Main content: chart carousel + entry form ─────────────────────────────

    private JPanel buildMainContent() {
        JPanel p = new JPanel(new GridLayout(1, 2, 16, 0));
        p.setOpaque(false);
        p.add(buildChartCarousel());
        p.add(buildEntryForm());
        return p;
    }

    // ── Chart carousel ────────────────────────────────────────────────────────

    private JPanel buildChartCarousel() {
        JPanel wrapper = makeSurface();
        wrapper.setLayout(new BorderLayout(0, 10));
        wrapper.setBorder(new EmptyBorder(18, 22, 18, 22));

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);

        carouselTitle = new JLabel(CAROUSEL_TITLES[0]);
        carouselTitle.setFont(new Font("SF Pro Display", Font.BOLD, 17));
        carouselTitle.setForeground(TEXT_PRIMARY);

        JButton prev = makeNavButton("‹");
        JButton next = makeNavButton("›");
        prev.addActionListener(e -> rotateCarousel(-1));
        next.addActionListener(e -> rotateCarousel(+1));

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        navPanel.setOpaque(false);
        navPanel.add(prev);
        navPanel.add(next);

        titleBar.add(carouselTitle, BorderLayout.WEST);
        titleBar.add(navPanel,      BorderLayout.EAST);

        chartCardLayout = new CardLayout();
        chartCarousel   = new JPanel(chartCardLayout);
        chartCarousel.setOpaque(false);

        donutChart = new DonutChartPanel();
        barChart   = new BarChartPanel();
        chartCarousel.add(donutChart, "DONUT");
        chartCarousel.add(barChart,   "BAR");

        wrapper.add(titleBar,      BorderLayout.NORTH);
        wrapper.add(chartCarousel, BorderLayout.CENTER);
        return wrapper;
    }

    private void rotateCarousel(int direction) {
        carouselIndex = Math.floorMod(carouselIndex + direction, 2);
        carouselTitle.setText(CAROUSEL_TITLES[carouselIndex]);
        if (carouselIndex == 0) chartCardLayout.show(chartCarousel, "DONUT");
        else                     chartCardLayout.show(chartCarousel, "BAR");
    }

    private JButton makeNavButton(String label) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? BORDER_COLOR : new Color(0xEFEFF4));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(TEXT_PRIMARY);
                g2.setFont(new Font("SF Pro Display", Font.PLAIN, 20));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label,
                    (getWidth()  - fm.stringWidth(label)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(34, 30));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Entry Form ────────────────────────────────────────────────────────────

    private JPanel buildEntryForm() {
        JPanel wrapper = makeSurface();
        wrapper.setLayout(new BorderLayout(0, 16));
        wrapper.setBorder(new EmptyBorder(22, 22, 22, 22));

        JLabel title = new JLabel("New Transaction");
        title.setFont(new Font("SF Pro Display", Font.BOLD, 17));
        title.setForeground(TEXT_PRIMARY);
        wrapper.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.insets  = new Insets(6, 0, 6, 0);
        gbc.weightx = 1.0;

        // ── Type ──────────────────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        form.add(formLabel("Type"), gbc);

        gbc.gridy = 1;
        typeCombo = styledCombo(new String[]{"Income", "Expense", "Investment"});
        form.add(typeCombo, gbc);

        // ── Category (combo + "+" button on the same row) ─────────────────────
        gbc.gridy = 2; gbc.gridwidth = 2;
        form.add(formLabel("Category"), gbc);

        // Initialise model from the default type (Income) on startup
        categoryModel = new DefaultComboBoxModel<>();
        loadCategoryModelFor(TransactionEntry.Type.INCOME);

        categoryCombo = new JComboBox<>(categoryModel);
        categoryCombo.setFont(new Font("SF Pro Text", Font.PLAIN, 14));
        categoryCombo.setBackground(SURFACE);
        categoryCombo.setPreferredSize(new Dimension(0, 38));

        // Category combo — takes all remaining horizontal space
        gbc.gridy     = 3;
        gbc.gridwidth = 1;
        gbc.weightx   = 1.0;
        gbc.gridx     = 0;
        form.add(categoryCombo, gbc);

        // "+" button — fixed width, right of the combo
        gbc.gridx   = 1;
        gbc.weightx = 0.0;
        gbc.insets  = new Insets(6, 6, 6, 0);
        form.add(buildAddCategoryButton(), gbc);

        // Restore default insets / weightx for the remaining rows
        gbc.gridx   = 0;
        gbc.weightx = 1.0;
        gbc.insets  = new Insets(6, 0, 6, 0);

        // ── Amount ────────────────────────────────────────────────────────────
        gbc.gridy = 4; gbc.gridwidth = 2;
        form.add(formLabel("Amount (R$)"), gbc);

        gbc.gridy = 5;
        amountField = styledTextField("0,00");
        form.add(amountField, gbc);

        // Vertical spacer
        gbc.gridy   = 6;
        gbc.weighty = 1.0;
        form.add(Box.createVerticalGlue(), gbc);
        gbc.weighty = 0;

        // ── Submit ────────────────────────────────────────────────────────────
        gbc.gridy = 7;
        form.add(buildSubmitButton(), gbc);

        wrapper.add(form, BorderLayout.CENTER);

        // Wire Type → category swap AFTER both combos are constructed
        typeCombo.addActionListener(e -> updateCategoryCombo());

        return wrapper;
    }

    // ── Per-type category loading ─────────────────────────────────────────────

    /**
     * Resolves the currently selected {@link TransactionEntry.Type} from the
     * Type combo. Returns {@code INCOME} as a safe fallback.
     */
    private TransactionEntry.Type selectedType() {
        String label = (String) typeCombo.getSelectedItem();
        try {
            return TransactionEntry.Type.fromLabel(label != null ? label : "Income");
        } catch (IllegalArgumentException e) {
            return TransactionEntry.Type.INCOME;
        }
    }

    /**
     * Clears the category combo model and populates it with categories
     * belonging to {@code type}, loaded from its dedicated file.
     */
    private void loadCategoryModelFor(TransactionEntry.Type type) {
        categoryModel.removeAllElements();
        for (String cat : fileHandler.getCategoriesFor(type)) {
            categoryModel.addElement(cat);
        }
    }

    /**
     * Called whenever the Type combo selection changes.
     * Swaps the category list to show only the categories for the new type.
     */
    private void updateCategoryCombo() {
        loadCategoryModelFor(selectedType());
    }

    // ── "+" Add-category button ───────────────────────────────────────────────

    /**
     * Builds the small "+" button (Apple-style light-blue rounded square).
     * When clicked it opens a dialog, persists the new category to the correct
     * type-specific file, and immediately adds it to the live combo model.
     */
    private JButton buildAddCategoryButton() {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? ACCENT_BLUE.darker() : new Color(0xE5F0FF));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(ACCENT_BLUE);
                g2.setFont(new Font("SF Pro Display", Font.BOLD, 20));
                FontMetrics fm = g2.getFontMetrics();
                String plus = "+";
                g2.drawString(plus,
                    (getWidth()  - fm.stringWidth(plus)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(38, 38));
        btn.setMinimumSize(new Dimension(38, 38));
        btn.setMaximumSize(new Dimension(38, 38));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setToolTipText("Add a new category for the selected type");
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> handleAddCategory());
        return btn;
    }

    /**
     * Handles the "add category" flow:
     * <ol>
     *   <li>Reads the currently selected type from the Type combo.</li>
     *   <li>Prompts the user for a name.</li>
     *   <li>Persists via {@link FinanceFileHandler#appendCategory} (type-scoped).</li>
     *   <li>Adds to the live {@link DefaultComboBoxModel} and selects it.</li>
     * </ol>
     */
    private void handleAddCategory() {
        TransactionEntry.Type type = selectedType();

        String input = JOptionPane.showInputDialog(
                this,
                "Enter a new category for " + type.label() + " transactions:",
                "Add Category — " + type.label(),
                JOptionPane.PLAIN_MESSAGE);

        if (input == null) return;          // user cancelled
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            showError("Category name cannot be empty.");
            return;
        }

        boolean saved = fileHandler.appendCategory(type, trimmed);
        if (saved) {
            categoryModel.addElement(trimmed);
            categoryModel.setSelectedItem(trimmed);
        } else {
            // Duplicate — just select the existing entry
            categoryModel.setSelectedItem(trimmed);
            JOptionPane.showMessageDialog(this,
                    "\"" + trimmed + "\" already exists for " + type.label() + " transactions.",
                    "Duplicate Category",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ── Amount parsing (Brazilian currency format) ────────────────────────────

    /**
     * Sanitizes and parses a user-supplied amount string that may be in
     * Brazilian (pt-BR) currency format.
     *
     * <p>Accepted examples: {@code "200,50"}, {@code "2.000,50"},
     * {@code "R$ 1.500,00"}, {@code "150.00"}, {@code "  300  "}.
     *
     * @param raw raw string from the amount field
     * @return parsed positive {@code double}
     * @throws NumberFormatException on any parse failure or non-positive value
     */
    private double parseAmount(String raw) throws NumberFormatException {
        if (raw == null) throw new NumberFormatException("null input");

        String s = raw.trim();
        if (s.isEmpty()) throw new NumberFormatException("empty input");

        // Remove currency symbol and spaces
        s = s.replace("R$", "").replace(" ", "");

        // Brazilian format: "2.000,50" → thousands dot + decimal comma
        if (s.contains(".") && s.contains(",")) {
            s = s.replace(".", "").replace(",", ".");
        } else if (s.contains(",")) {
            // Simple decimal comma: "200,50" → "200.50"
            s = s.replace(",", ".");
        }
        // If only "." present → already standard decimal

        double value = Double.parseDouble(s);
        if (!Double.isFinite(value) || value <= 0) {
            throw new NumberFormatException("Amount must be a positive finite number.");
        }
        return value;
    }

    // ── Transaction submission ────────────────────────────────────────────────

    /**
     * Reads the form, sanitizes the amount, persists the entry, and refreshes
     * the full UI. Shows a user-friendly error dialog on bad input.
     */
    private void handleAddTransaction() {
        String categoryStr = (String) categoryCombo.getSelectedItem();
        String amountRaw   = amountField.getText();

        // Robust Brazilian-currency-aware amount parsing
        double amount;
        try {
            amount = parseAmount(amountRaw);
        } catch (NumberFormatException ex) {
            showError(
                "<html>Invalid amount: <b>" + amountRaw.trim() + "</b><br><br>" +
                "Accepted formats:<br>" +
                "&nbsp;&nbsp;• <b>1500.00</b> or <b>1500,00</b><br>" +
                "&nbsp;&nbsp;• <b>1.500,00</b> (Brazilian thousand-separator)<br>" +
                "&nbsp;&nbsp;• <b>R$ 1.500,00</b> (with currency symbol)</html>"
            );
            return;
        }

        TransactionEntry.Type type = selectedType();
        TransactionEntry entry = new TransactionEntry(type, categoryStr, amount);
        boolean saved = fileHandler.appendTransaction(entry);

        if (saved) {
            amountField.setText("0,00");
            refreshAll();
        } else {
            showError("Could not save the transaction.\nPlease check file permissions.");
        }
    }

    // ── Form helpers ──────────────────────────────────────────────────────────

    private JLabel formLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SF Pro Text", Font.PLAIN, 13));
        lbl.setForeground(TEXT_SECONDARY);
        return lbl;
    }

    private <T> JComboBox<T> styledCombo(T[] items) {
        JComboBox<T> box = new JComboBox<>(items);
        box.setFont(new Font("SF Pro Text", Font.PLAIN, 14));
        box.setBackground(SURFACE);
        box.setPreferredSize(new Dimension(0, 38));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return box;
    }

    private JTextField styledTextField(String placeholder) {
        JTextField tf = new JTextField(placeholder) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        tf.setFont(new Font("SF Pro Text", Font.PLAIN, 14));
        tf.setBackground(new Color(0xF2F2F7));
        tf.setBorder(new EmptyBorder(8, 12, 8, 12));
        tf.setPreferredSize(new Dimension(0, 38));
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (tf.getText().equals(placeholder)) tf.setText("");
            }
            @Override public void focusLost(FocusEvent e) {
                if (tf.getText().isBlank()) tf.setText(placeholder);
            }
        });
        return tf;
    }

    private JButton buildSubmitButton() {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? ACCENT_BLUE.darker() : ACCENT_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SF Pro Text", Font.BOLD, 15));
                FontMetrics fm = g2.getFontMetrics();
                String text = "Add Transaction";
                g2.drawString(text,
                    (getWidth()  - fm.stringWidth(text)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(0, 46));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> handleAddTransaction());
        return btn;
    }

    // ── Table section ─────────────────────────────────────────────────────────

    private JPanel buildTableSection() {
        JPanel wrapper = makeSurface();
        wrapper.setLayout(new BorderLayout(0, 12));
        wrapper.setBorder(new EmptyBorder(22, 22, 22, 22));

        JLabel title = new JLabel("Transaction History");
        title.setFont(new Font("SF Pro Display", Font.BOLD, 17));
        title.setForeground(TEXT_PRIMARY);
        wrapper.add(title, BorderLayout.NORTH);

        String[] columns = {"Type", "Category", "Amount", "Date & Time"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setFont(new Font("SF Pro Text", Font.PLAIN, 13));
        table.setRowHeight(36);
        table.setBackground(SURFACE);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0xE5F0FF));
        table.setSelectionForeground(TEXT_PRIMARY);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SF Pro Text", Font.BOLD, 12));
        header.setBackground(new Color(0xF2F2F7));
        header.setForeground(TEXT_SECONDARY);
        header.setBorder(BorderFactory.createEmptyBorder());

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean selected, boolean focused, int row, int col) {
                Component c = super.getTableCellRendererComponent(
                        tbl, value, selected, focused, row, col);
                if (!selected) {
                    c.setBackground(row % 2 == 0 ? SURFACE : new Color(0xFAFAFC));
                }
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setHorizontalAlignment(col == 2 ? SwingConstants.RIGHT
                                      : col == 0 ? SwingConstants.CENTER
                                      : SwingConstants.LEFT);
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        sp.setPreferredSize(new Dimension(0, 220));
        sp.getViewport().setBackground(SURFACE);
        wrapper.add(sp, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Refresh / data binding ────────────────────────────────────────────────

    /** Full UI refresh — called on startup and after every saved transaction. */
    private void refreshAll() {
        List<TransactionEntry> entries = fileHandler.getAllTransactions();
        refreshDashboard();
        refreshCharts(entries);
        refreshTable(entries);
    }

    private void refreshDashboard() {
        double balance  = service.getNetBalance();
        double expenses = service.getTotalExpenses();
        double invested = service.getTotalInvestments();

        balanceValueLabel.setText(CURRENCY.format(balance));
        expenseValueLabel.setText(CURRENCY.format(expenses));
        investValueLabel.setText(CURRENCY.format(invested));

        balanceValueLabel.setForeground(balance >= 0 ? ACCENT_BLUE : ACCENT_RED);
    }

    private void refreshCharts(List<TransactionEntry> entries) {
        Map<String, Double> expenseMap = service.getExpensesByCategory();
        donutChart.setData(expenseMap);
        barChart.setData(expenseMap);
    }

    private void refreshTable(List<TransactionEntry> entries) {
        tableModel.setRowCount(0);
        List<TransactionEntry> reversed = new ArrayList<>(entries);
        Collections.reverse(reversed);
        for (TransactionEntry e : reversed) {
            tableModel.addRow(new Object[]{
                e.getType().label(),
                e.getCategory(),
                CURRENCY.format(e.getAmount()),
                e.getDateTime().format(DISPLAY_FMT)
            });
        }
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    /** Creates a white, shadowed, rounded surface panel. */
    private JPanel makeSurface() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 8; i >= 1; i--) {
                    g2.setColor(new Color(0, 0, 0, 4 * i));
                    g2.fillRoundRect(i, i + 2, getWidth() - i * 2, getHeight() - i * 2, 20, 20);
                }
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, getWidth() - 5, getHeight() - 5, 20, 20);
                g2.dispose();
            }
        };
    }

    private Component vGap(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Input Error", JOptionPane.WARNING_MESSAGE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Inner class: Donut Chart
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Custom-painted donut chart (Graphics2D). Shows expense totals per
     * category as arc slices, with the grand total in the center hole and a
     * color-coded legend to the right.
     */
    private class DonutChartPanel extends JPanel {

        private Map<String, Double> data = new LinkedHashMap<>();

        DonutChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 320));
        }

        void setData(Map<String, Double> data) {
            this.data = (data == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(data);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth(), h = getHeight();

            if (data.isEmpty()) {
                g2.setFont(new Font("SF Pro Text", Font.PLAIN, 14));
                g2.setColor(TEXT_SECONDARY);
                String msg = "No expense data yet";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            int legendWidth = 190;
            int chartSize   = Math.min(w - legendWidth - 20, h - 20);
            int cx          = (w - legendWidth) / 2;
            int cy          = h / 2;
            int outerR      = chartSize / 2;
            int innerR      = (int)(outerR * 0.55);

            double total      = data.values().stream().mapToDouble(Double::doubleValue).sum();
            double startAngle = -90.0;

            List<String> keys = new ArrayList<>(data.keySet());
            List<Double> vals = new ArrayList<>(data.values());

            // Draw slices
            for (int i = 0; i < keys.size(); i++) {
                double sweepAngle = (vals.get(i) / total) * 360.0;
                g2.setColor(CHART_COLORS[i % CHART_COLORS.length]);
                g2.fill(new Arc2D.Double(
                        cx - outerR, cy - outerR, outerR * 2, outerR * 2,
                        startAngle, sweepAngle, Arc2D.PIE));
                startAngle += sweepAngle;
            }

            // Inner hole
            g2.setColor(SURFACE);
            g2.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);
            g2.setColor(BG);
            g2.setStroke(new BasicStroke(3));
            g2.drawOval(cx - outerR, cy - outerR, outerR * 2, outerR * 2);
            g2.drawOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

            // Center label
            String totalStr = CURRENCY.format(total);
            g2.setFont(new Font("SF Pro Display", Font.BOLD, (int)(innerR * 0.38)));
            FontMetrics fmBig = g2.getFontMetrics();
            g2.setColor(TEXT_PRIMARY);
            g2.drawString(totalStr,
                cx - fmBig.stringWidth(totalStr) / 2,
                cy + fmBig.getAscent() / 3);

            g2.setFont(new Font("SF Pro Text", Font.PLAIN, (int)(innerR * 0.28)));
            FontMetrics fmSm = g2.getFontMetrics();
            g2.setColor(TEXT_SECONDARY);
            String lbl = "Total";
            g2.drawString(lbl, cx - fmSm.stringWidth(lbl) / 2, cy - fmBig.getAscent() / 2);

            // Legend
            int legendX = cx + outerR + 24;
            int legendY = cy - (keys.size() * 24) / 2;
            g2.setFont(new Font("SF Pro Text", Font.PLAIN, 12));
            FontMetrics fmLeg = g2.getFontMetrics();

            for (int i = 0; i < keys.size(); i++) {
                Color col = CHART_COLORS[i % CHART_COLORS.length];
                int   ly  = legendY + i * 26;

                g2.setColor(col);
                g2.fillRoundRect(legendX, ly, 14, 14, 6, 6);

                String catLabel = keys.get(i);
                if (catLabel.length() > 16) catLabel = catLabel.substring(0, 14) + "…";
                g2.setColor(TEXT_PRIMARY);
                g2.drawString(catLabel, legendX + 20, ly + fmLeg.getAscent() - 1);

                String pctStr = String.format("%.1f%%", (vals.get(i) / total) * 100);
                g2.setColor(TEXT_SECONDARY);
                g2.drawString(pctStr,
                    legendX + legendWidth - fmLeg.stringWidth(pctStr) - 8,
                    ly + fmLeg.getAscent() - 1);
            }

            g2.dispose();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Inner class: Bar Chart
    // ═══════════════════════════════════════════════════════════════════════════

    /** Custom-painted horizontal bar chart showing expense totals per category. */
    private class BarChartPanel extends JPanel {

        private Map<String, Double> data = new LinkedHashMap<>();

        BarChartPanel() { setOpaque(false); }

        void setData(Map<String, Double> data) {
            this.data = (data == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(data);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth(), h = getHeight();
            int padLeft = 120, padRight = 80, padTop = 16;
            int chartW  = w - padLeft - padRight;

            if (data.isEmpty()) {
                g2.setFont(new Font("SF Pro Text", Font.PLAIN, 14));
                g2.setColor(TEXT_SECONDARY);
                String msg = "No expense data yet";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            double maxVal = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
            List<String> keys = new ArrayList<>(data.keySet());
            List<Double> vals = new ArrayList<>(data.values());
            int n    = keys.size();
            int rowH = Math.min(36, (h - padTop - 16) / Math.max(n, 1));

            g2.setFont(new Font("SF Pro Text", Font.PLAIN, 11));
            FontMetrics fm = g2.getFontMetrics();

            for (int i = 0; i < n; i++) {
                int y      = padTop + i * rowH;
                int barH   = (int)(rowH * 0.55);
                int barY   = y + (rowH - barH) / 2;
                int barLen = (int)((vals.get(i) / maxVal) * chartW);
                Color col  = CHART_COLORS[i % CHART_COLORS.length];

                g2.setColor(new Color(0xF2F2F7));
                g2.fillRoundRect(padLeft, barY, chartW, barH, barH, barH);

                if (barLen > 0) {
                    g2.setColor(col);
                    g2.fillRoundRect(padLeft, barY, barLen, barH, barH, barH);
                }

                String catLabel = keys.get(i);
                if (catLabel.length() > 13) catLabel = catLabel.substring(0, 11) + "…";
                g2.setColor(TEXT_PRIMARY);
                g2.drawString(catLabel,
                    padLeft - fm.stringWidth(catLabel) - 8,
                    barY + barH / 2 + fm.getAscent() / 2 - 1);

                g2.setColor(TEXT_SECONDARY);
                g2.drawString(CURRENCY.format(vals.get(i)),
                    padLeft + chartW + 6,
                    barY + barH / 2 + fm.getAscent() / 2 - 1);
            }

            g2.dispose();
        }
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FinanceWindow::new);
    }
}
