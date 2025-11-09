
// SmartNotesApp.java
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Queue;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;

class Question {
    String questionText;
    String answer;
    Question(String questionText, String answer) {
        this.questionText = questionText;
        this.answer = answer;
    }
}

// Mock Exam Question with topic and difficulty
class ExamQuestion {
    String question;
    String[] options;  // For MCQ
    String correctAnswer;
    String topic;
    int difficulty;  // 1-5
    String type;  // "MCQ", "TrueFalse", "Short"
    
    ExamQuestion(String question, String[] options, String correctAnswer, String topic, int difficulty, String type) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.topic = topic;
        this.difficulty = difficulty;
        this.type = type;
    }
}

// Exam Attempt History
class ExamAttempt {
    String examName;
    int totalQuestions;
    int correctAnswers;
    double percentage;
    HashMap<String, Integer> topicWiseScore;  // Topic -> correct count
    HashMap<String, Integer> topicWiseMistakes;  // Topic -> mistake count
    String date;
    long timeTaken;  // in seconds
    
    ExamAttempt(String examName, int total, int correct) {
        this.examName = examName;
        this.totalQuestions = total;
        this.correctAnswers = correct;
        this.percentage = (correct * 100.0) / total;
        this.topicWiseScore = new HashMap<>();
        this.topicWiseMistakes = new HashMap<>();
        this.date = java.time.LocalDateTime.now().toString();
        this.timeTaken = 0;
    }
}

// Flashcard for Last-Minute Revision
class Flashcard {
    String question;
    String answer;
    String topic;
    boolean mastered;
    int reviewCount;
    
    Flashcard(String question, String answer, String topic) {
        this.question = question;
        this.answer = answer;
        this.topic = topic;
        this.mastered = false;
        this.reviewCount = 0;
    }
}

// Revision Card for Quick Revision Deck
class RevisionCard {
    String question;
    String answer;
    String topic;
    boolean completed;
    String createdDate;
    String completedDate;
    int revisionCount;
    
    RevisionCard(String question, String answer, String topic) {
        this.question = question;
        this.answer = answer;
        this.topic = topic;
        this.completed = false;
        this.createdDate = java.time.LocalDate.now().toString();
        this.completedDate = "";
        this.revisionCount = 0;
    }
}

// Revision Topic for tracking
class RevisionTopic {
    String topicName;
    int totalQuestions;
    int completedQuestions;
    
    RevisionTopic(String topicName) {
        this.topicName = topicName;
        this.totalQuestions = 0;
        this.completedQuestions = 0;
    }
}

public class SmartNotesApp {

    // <-- Your API KEY (consider replacing with environment variable in future) -->
    private static final String GEMINI_API_KEY = "AIzaSyAv3XO3DyvCWbeJ31dqCjLu01Rrs8jdZh0";

    static String currentUser = "guest";

    // Database connection details
    static final String URL = "jdbc:mysql://localhost:3306/smartnotes";
    static final String USER = "root";
    static final String PASS = "Rofi@1013";

    // Streak and Challenge tracking
    private static int currentStreak = 0;
    private static int weeklyStreak = 0;
    private static String lastStudyDate = "";
    private static int dailyChallengeProgress = 0;
    private static int weeklyChallengeProgress = 0;
    private static String currentDailyChallenge = "";
    private static String currentWeeklyChallenge = "";
    private static int totalXP = 0;
    private static java.util.List<String> badges = new ArrayList<>();

    // ANSI colors (console)
    static final String RESET = "\u001B[0m";
    static final String CYAN = "\u001B[36m";
    static final String GREEN = "\u001B[32m";
    static final String YELLOW = "\u001B[33m";
    static final String RED = "\u001B[31m";
    static final String BLUE = "\u001B[34m";
    static final String BOLD = "\u001B[1m";

    // In-memory storage for the current input / summary
    private static String currentInputText = "";
    private static String currentSummary = "";
    private static String lastDescriptiveText = "";
    private static String lastQuizText = "";
    
    // Mock Exam & Revision System
    private static ArrayList<ExamQuestion> questionBank = new ArrayList<>();
    private static LinkedList<ExamAttempt> examHistory = new LinkedList<>();
    private static HashMap<String, Integer> topicMistakes = new HashMap<>();
    private static ArrayList<Flashcard> flashcards = new ArrayList<>();
    private static HashMap<String, String> quickNotes = new HashMap<>();  // Topic -> Key points
    
    // Revision Deck for Quick Revision
    private static ArrayList<RevisionCard> revisionDeck = new ArrayList<>();
    private static HashMap<String, RevisionTopic> revisionTopics = new HashMap<>();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println(GREEN + "\n✅ Connected to SmartNotes database!" + RESET);

            while (true) {
                System.out.println("\n1️⃣ Signup");
                System.out.println("2️⃣ Login");
                System.out.println("3️⃣ Exit");
                System.out.print(YELLOW + "👉 Choose option: " + RESET);

                int choice = getIntInput(sc);
                sc.nextLine();

                if (choice == 1) signup(conn, sc);
                else if (choice == 2) {
                    if (login(conn, sc)) {
                        System.out.println(GREEN + "\n🎯 Login successful! Welcome, " + currentUser + "!" + RESET);
                        // After login, ask the user to provide input (console-only)
                        promptForInputInConsole(sc);
                        // Launch GUI dashboard (tabs)
                        SwingUtilities.invokeLater(() -> createAndShowGUI());
                        break; // exit console loop — GUI will handle rest
                    }
                } else if (choice == 3) {
                    System.out.println(GREEN + "👋 Goodbye!" + RESET);
                    conn.close();
                    return;
                } else System.out.println(RED + "❌ Invalid choice!" + RESET);
            }

        } catch (Exception ex) {
            System.out.println(RED + "❌ Error: " + ex.getMessage() + RESET);
            ex.printStackTrace();
        }
    }

    // ===================== LOGIN / SIGNUP =====================
    private static void signup(Connection conn, Scanner sc) {
        try {
            System.out.print("Enter new username: ");
            String username = sc.nextLine();
            System.out.print("Enter new password: ");
            String password = sc.nextLine();

            String query = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            System.out.println(GREEN + "✅ Signup successful!" + RESET);

        } catch (SQLIntegrityConstraintViolationException ex) {
            System.out.println(RED + "⚠️ Username already exists!" + RESET);
        } catch (SQLException ex) {
            System.out.println(RED + "❌ Signup error: " + ex.getMessage() + RESET);
        }
    }

    private static boolean login(Connection conn, Scanner sc) {
        try {
            System.out.print("Enter username: ");
            String username = sc.nextLine();
            System.out.print("Enter password: ");
            String password = sc.nextLine();

            String query = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                currentUser = username;
                return true;
            } else {
                System.out.println(RED + "❌ Invalid username or password." + RESET);
                return false;
            }
        } catch (SQLException ex) {
            System.out.println(RED + "❌ Login error: " + ex.getMessage() + RESET);
        }
        return false;
    }

    // ===================== Console input prompt (only console I/O allowed) =====================
    private static void promptForInputInConsole(Scanner sc) {
        System.out.println(CYAN + "\nProvide input text now (console) or choose a file:" + RESET);
        System.out.println("1. Enter text directly (type END on a new line to finish)");
        System.out.println("2. Enter file path to load text");
        System.out.println("3. Skip (use previous saved summaries if any)");
        System.out.print(YELLOW + "👉 Choose option: " + RESET);

        int opt = getIntInput(sc);
        sc.nextLine();

        if (opt == 1) {
            System.out.println(CYAN + "\n📝 Enter text (type END to finish):" + RESET);
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = sc.nextLine();
                if (line.equalsIgnoreCase("END")) break;
                sb.append(line).append(" ");
            }
            currentInputText = sb.toString().trim();
            System.out.println(GREEN + "✅ Text received. Now launching GUI..." + RESET);
        } else if (opt == 2) {
            System.out.print(YELLOW + "📄 Enter file path: " + RESET);
            String filePath = sc.nextLine();
            currentInputText = readFile(filePath);
            if (currentInputText.isBlank()) {
                System.out.println(RED + "⚠️ Failed to read file or file empty." + RESET);
            } else System.out.println(GREEN + "✅ File loaded. Now launching GUI..." + RESET);
        } else {
            System.out.println(YELLOW + "Skipping input. You can use saved summaries or import later." + RESET);
        }
    }

    // ===================== GUI (Tabbed) =====================
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("SmartNotesApp — Dashboard (" + currentUser + ")");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 650);
        frame.setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Summary", buildSummaryPanel());
        tabs.addTab("Quiz", buildQuizPanel());
        tabs.addTab("Descriptive", buildDescriptivePanel());
        tabs.addTab("Saved Summaries", buildSavedSummariesPanel());
        tabs.addTab("Performance Dashboard", buildPerformanceDashboardPanel());
        tabs.addTab("Mock Exam", buildMockExamPanel());

        frame.add(tabs);
        frame.setVisible(true);
    }

    // ---------- Summary Tab ----------
    private static JPanel buildSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.setBorder(new EmptyBorder(10,10,10,10));

        JTextArea summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton btnGenerate = new JButton("Generate Summary (uses console input)");
        btnGenerate.addActionListener(evt -> {
            if (currentInputText == null || currentInputText.isBlank()) {
                JOptionPane.showMessageDialog(null, "No input text found. Provide input from console before opening GUI.", "No Input", JOptionPane.WARNING_MESSAGE);
                return;
            }
            btnGenerate.setEnabled(false);
            summaryArea.setText("Generating summary... please wait.");
            // run in background
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    try {
                        // ask for format and number of sentences before summarizing
                        Object choice = JOptionPane.showInputDialog(
                                null,
                                "Choose summary format:",
                                "Summary Format",
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                new Object[]{"Paragraph", "Bulleted"},
                                "Paragraph");
                        String format = choice == null ? "Paragraph" : choice.toString();

                        String sNum = JOptionPane.showInputDialog(null, "Enter number of sentences/bullets:", "2");
                        int num = 2;
                        if (sNum != null) {
                            try { num = Integer.parseInt(sNum.trim()); } catch (Exception ex) { num = 2; }
                        }
                        String prompt;
                        if ("Bulleted".equalsIgnoreCase(format)) {
                            prompt = "Summarize the following text as a concise bulleted list with exactly " + num + " bullets. Use '-' as bullet prefix and avoid extra commentary.\n\n" + currentInputText;
                        } else {
                            prompt = "Summarize the following text in exactly " + num + " sentences as a coherent paragraph:\n\n" + currentInputText;
                        }
                        String summ = callGeminiApiForSummary(prompt);
                        currentSummary = summ;
                        return summ;
                    } catch (Exception ex) {
                        return "Error generating summary: " + ex.getMessage();
                    }
                }
                @Override
                protected void done() {
                    try {
                        String result = get();
                        summaryArea.setText(result);
                    } catch (Exception ex) {
                        summaryArea.setText("Failed to generate summary: " + ex.getMessage());
                    } finally {
                        btnGenerate.setEnabled(true);
                    }
                }
            }.execute();
        });

        JButton btnSave = new JButton("Save Summary");
        btnSave.addActionListener(evt -> {
            if (currentSummary == null || currentSummary.isBlank()) {
                JOptionPane.showMessageDialog(null, "No summary to save. Generate one first.", "No Summary", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try (FileWriter fw = new FileWriter("saved_summaries.txt", true)) {
                fw.write("\n==== Summary ====\n" + currentSummary + "\n=================\n");
                JOptionPane.showMessageDialog(null, "Saved summary to saved_summaries.txt", "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error saving: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(btnGenerate);
        top.add(btnSave);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(summaryArea), BorderLayout.CENTER);

        // If we already have currentSummary prefilled (like if user entered from console earlier and summary generated), show it
        if (currentSummary != null && !currentSummary.isBlank()) summaryArea.setText(currentSummary);

        return panel;
    }

    // ---------- Quiz Tab ----------
    private static JPanel buildQuizPanel() {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.setBorder(new EmptyBorder(10,10,10,10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblQ = new JLabel("Questions:");
        SpinnerNumberModel model = new SpinnerNumberModel(3, 1, 10, 1);
        JSpinner spinnerNum = new JSpinner(model);
        JButton btnGenerate = new JButton("Generate Quiz (AI)");
        top.add(lblQ);
        top.add(spinnerNum);
        top.add(btnGenerate);

        JPanel center = new JPanel(new BorderLayout());
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        center.add(new JScrollPane(infoArea), BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);

        // Container to hold current quiz questions (so Take Quiz button can use them)
        final java.util.List<Question>[] currentQuiz = new java.util.List[]{new ArrayList<>()};

        btnGenerate.addActionListener(evt -> {
            if (currentSummary == null || currentSummary.isBlank()) {
                JOptionPane.showMessageDialog(null, "Please generate a summary first (Summary tab).", "No Summary", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int numQ = (int) spinnerNum.getValue();
            btnGenerate.setEnabled(false);
            infoArea.setText("Generating " + numQ + " MCQs... please wait.");
            new SwingWorker<ArrayList<Question>, Void>() {
                @Override
                protected ArrayList<Question> doInBackground() {
                    return generateAIQuiz(currentSummary, numQ);
                }
                @Override
                protected void done() {
                    try {
                        ArrayList<Question> q = get();
                        currentQuiz[0] = q;
                        if (q.isEmpty()) infoArea.setText("No quiz generated.");
                        else {
                            StringBuilder sb = new StringBuilder();
                            int i = 1;
                            for (Question qq : q) {
                                sb.append("Q").append(i++).append(": ").append(qq.questionText.replaceAll("(?m)^", "    ")).append("\n");
                            }
                            infoArea.setText(sb.toString());
                            lastQuizText = infoArea.getText();
                            // also persist quiz to file
                            try (FileWriter fw = new FileWriter("saved_summaries.txt", true)) {
                                fw.write("\n==== Quiz ====\n" + lastQuizText + "\n=================\n");
                            } catch (IOException ignored) {}
                        }
                    } catch (Exception ex) {
                        infoArea.setText("Quiz generation failed: " + ex.getMessage());
                    } finally {
                        btnGenerate.setEnabled(true);
                    }
                }
            }.execute();
        });

        JButton btnTake = new JButton("Take Quiz");
        btnTake.addActionListener(evt -> {
            if (currentQuiz[0] == null || currentQuiz[0].isEmpty()) {
                JOptionPane.showMessageDialog(null, "No quiz available. Generate one first.", "No Quiz", JOptionPane.WARNING_MESSAGE);
                return;
            }
            showQuizDialog(currentQuiz[0]);
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnTake);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // Show quiz in a dialog with radio buttons and scoring
    private static void showQuizDialog(java.util.List<Question> quiz) {
        // Step-through quiz with explicit A/B/C/D buttons per question
        int score = 0;
        StringBuilder takenQuizLog = new StringBuilder();
        for (int i = 0; i < quiz.size(); i++) {
            Question q = quiz.get(i);
            java.util.List<String> options = new ArrayList<>();
            String[] lines = q.questionText.split("\\r?\\n");
            for (String line : lines) {
                String t = line.trim();
                if (t.matches("^[A-D]\\).*")) {
                    options.add(t.substring(2).trim());
                }
            }
            if (options.size() < 4) {
                options = Arrays.asList("Option 1", "Option 2", "Option 3", "Option 4");
            }

            String message = q.questionText + "\n\nChoose your answer:";
            String[] buttons = new String[]{"A", "B", "C", "D"};
            int choice = JOptionPane.showOptionDialog(
                    null,
                    message,
                    "Q" + (i + 1),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    buttons,
                    null);

            String chosenLetter = (choice >= 0 && choice < 4) ? buttons[choice] : "";
            String correct = q.answer.trim().toUpperCase();
            if (chosenLetter.equalsIgnoreCase(correct)) score++;

            takenQuizLog.append("Q").append(i + 1).append(": ").append(q.questionText).append("\n")
                    .append("Chosen: ").append(chosenLetter).append(" | Correct: ").append(correct).append("\n\n");
        }
        JOptionPane.showMessageDialog(null, "You scored: " + score + " / " + quiz.size(), "Quiz Result", JOptionPane.INFORMATION_MESSAGE);
        try (FileWriter fw = new FileWriter("saved_summaries.txt", true)) {
            fw.write("\n==== Quiz Result ====\n" + takenQuizLog + "Score: " + score + " / " + quiz.size() + "\n=================\n");
        } catch (IOException ignored) {}
    }

    // ---------- Descriptive Tab (local logic unchanged) ----------
    private static JPanel buildDescriptivePanel() {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.setBorder(new EmptyBorder(10,10,10,10));

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnGenerate = new JButton("Generate Descriptive Questions (local)");
        btnGenerate.addActionListener(evt -> {
            if (currentSummary == null || currentSummary.isBlank()) {
                JOptionPane.showMessageDialog(null, "Generate a summary first.", "No Summary", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String sNum = JOptionPane.showInputDialog(null, "How many descriptive questions?", "2");
            int num = 2;
            try { num = Integer.parseInt(sNum.trim()); } catch (Exception ex) { num = 2; }
            String sMarks = JOptionPane.showInputDialog(null, "Marks (2/5/10/16)?", "5");
            int marks = 5;
            try { marks = Integer.parseInt(sMarks.trim()); } catch (Exception ex) { marks = 5; }
            ArrayList<Question> desc = generateQuestionsFromContent(currentSummary, num, marks);
            StringBuilder sb = new StringBuilder();
            int i = 1;
            for (Question q : desc) {
                sb.append("Q").append(i++).append(": ").append(q.questionText).append("\nAnswer:\n").append(q.answer).append("\n\n");
            }
            area.setText(sb.toString());
            lastDescriptiveText = "Marks: " + marks + "\n" + sb;
        });

        JButton btnSaveDesc = new JButton("Save Descriptive");
        btnSaveDesc.addActionListener(evt -> {
            if (lastDescriptiveText == null || lastDescriptiveText.isBlank()) {
                JOptionPane.showMessageDialog(null, "No descriptive questions to save.", "Nothing to save", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try (FileWriter fw = new FileWriter("saved_summaries.txt", true)) {
                fw.write("\n==== Descriptive ====\n" + lastDescriptiveText + "\n=================\n");
                JOptionPane.showMessageDialog(null, "Saved to saved_summaries.txt", "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error saving: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        top.add(btnGenerate);
        top.add(btnSaveDesc);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }


    // ---------- Saved Summaries Tab ----------
    private static JPanel buildSavedSummariesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.setBorder(new EmptyBorder(10,10,10,10));

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(evt -> area.setText(readFile("saved_summaries.txt")));

        JButton btnClear = new JButton("Clear File");
        btnClear.addActionListener(evt -> {
            int res = JOptionPane.showConfirmDialog(null, "Delete saved_summaries.txt contents?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                try (FileWriter fw = new FileWriter("saved_summaries.txt", false)) {
                    fw.write("");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error clearing saved summaries: " + ex.getMessage());
                }

                area.setText("");
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(btnRefresh);
        top.add(btnClear);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);

        // initial load
        area.setText(readFile("saved_summaries.txt"));
        return panel;
    }

    // ---------- Performance Dashboard Tab ----------
    private static JPanel buildPerformanceDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.setBorder(new EmptyBorder(10,10,10,10));
        
        JTextArea dashboardArea = new JTextArea();
        dashboardArea.setEditable(false);
        dashboardArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dashboardArea.setBackground(new Color(245, 245, 250));
        
        JButton btnRefresh = new JButton("Refresh Dashboard");
        btnRefresh.addActionListener(evt -> {
            updatePerformanceDashboard(dashboardArea);
        });

        panel.add(btnRefresh, BorderLayout.NORTH);
        panel.add(new JScrollPane(dashboardArea), BorderLayout.CENTER);
        
        // Initial load
        updatePerformanceDashboard(dashboardArea);
        return panel;
    }

    private static void updatePerformanceDashboard(JTextArea area) {
        String data = readFile("saved_summaries.txt");
        StringBuilder dashboard = new StringBuilder();
        dashboard.append("═══════════════════════════════════════════════\n");
        dashboard.append("        📊 PERFORMANCE DASHBOARD\n");
        dashboard.append("═══════════════════════════════════════════════\n\n");
        
        // Quiz Statistics
        int totalQuizzes = 0;
        int totalScore = 0;
        int totalQuestions = 0;
        String[] lines = data.split("\n");
        for (String line : lines) {
            if (line.startsWith("Score:")) {
                try {
                    String[] parts = line.replace("Score:", "").trim().split("/");
                    if (parts.length == 2) {
                        totalScore += Integer.parseInt(parts[0].trim());
                        totalQuestions += Integer.parseInt(parts[1].trim());
                        totalQuizzes++;
                    }
                } catch (Exception ex) {}
            }
        }
        
        int summaryCount = data.split("==== Summary ====").length - 1;
        int quizCount = data.split("==== Quiz ====").length - 1;
        int descriptiveCount = data.split("==== Descriptive ====").length - 1;
        
        dashboard.append("📚 STUDY STATISTICS\n");
        dashboard.append("───────────────────────────────────────────────\n");
        dashboard.append(String.format("  • Total Summaries Created: %d\n", summaryCount));
        dashboard.append(String.format("  • Total Quizzes Generated: %d\n", quizCount));
        dashboard.append(String.format("  • Total Descriptive Questions: %d\n", descriptiveCount));
        dashboard.append(String.format("  • Quizzes Taken: %d\n", totalQuizzes));
        dashboard.append("\n");
        
        dashboard.append("🎯 QUIZ PERFORMANCE\n");
        dashboard.append("───────────────────────────────────────────────\n");
        if (totalQuizzes > 0) {
            double accuracy = (totalQuestions > 0) ? (totalScore * 100.0 / totalQuestions) : 0;
            dashboard.append(String.format("  • Total Questions Answered: %d\n", totalQuestions));
            dashboard.append(String.format("  • Correct Answers: %d\n", totalScore));
            dashboard.append(String.format("  • Overall Accuracy: %.1f%%\n", accuracy));
            dashboard.append(String.format("  • Average Score per Quiz: %.1f\n", (double)totalScore / totalQuizzes));
            
            String performance;
            if (accuracy >= 90) performance = "🌟 Excellent!";
            else if (accuracy >= 75) performance = "👍 Good!";
            else if (accuracy >= 60) performance = "📈 Keep Improving!";
            else performance = "💪 Need More Practice!";
            dashboard.append(String.format("  • Performance Rating: %s\n", performance));
        } else {
            dashboard.append("  • No quizzes taken yet.\n");
        }
        dashboard.append("\n");
        
        dashboard.append("🔥 STREAK & ENGAGEMENT\n");
        dashboard.append("───────────────────────────────────────────────\n");
        dashboard.append(String.format("  • Current Daily Streak: %d days\n", currentStreak));
        dashboard.append(String.format("  • Weekly Streak: %d weeks\n", weeklyStreak));
        dashboard.append(String.format("  • Total XP Earned: %d\n", totalXP));
        dashboard.append(String.format("  • Badges Unlocked: %d\n", badges.size()));
        if (!badges.isEmpty()) {
            dashboard.append("  • Your Badges: ").append(String.join(", ", badges)).append("\n");
        }
        dashboard.append("\n");
        
        dashboard.append("═══════════════════════════════════════════════\n");
        
        area.setText(dashboard.toString());
    }

    // ---------- Mock Exam Panel ----------
    private static JPanel buildMockExamPanel() {
        JPanel panel = new JPanel(new BorderLayout(10,10));
        panel.setBorder(new EmptyBorder(15,15,15,15));
        
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("📝 Mock Exam");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titlePanel.add(titleLabel);
        
        JTextArea examArea = new JTextArea(20, 60);
        examArea.setEditable(false);
        examArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        examArea.setLineWrap(true);
        examArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(examArea);
        
        JPanel controlPanel = new JPanel(new FlowLayout());
        
        JButton btnGenerateExam = new JButton("🎯 Generate Mock Exam");
        JButton btnTakeExam = new JButton("✍️ Take Exam");
        JButton btnViewHistory = new JButton("📊 View History");
        
        btnGenerateExam.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnTakeExam.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnViewHistory.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        final ArrayList<ExamQuestion>[] currentExam = new ArrayList[]{new ArrayList<>()};
        
        btnGenerateExam.addActionListener(e -> {
            if (currentSummary == null || currentSummary.isBlank()) {
                JOptionPane.showMessageDialog(null, 
                    "Please generate a summary first!", 
                    "No Content", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Warn about rate limits
            int confirm = JOptionPane.showConfirmDialog(null,
                "⚠️ IMPORTANT: API Rate Limit Warning\n\n" +
                "• Free tier: 15 requests per minute\n" +
                "• If you've used other features recently, wait 2-3 minutes\n" +
                "• Generation may take 30-60 seconds\n\n" +
                "Continue with exam generation?",
                "Rate Limit Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            
            String numStr = JOptionPane.showInputDialog(null, 
                "How many questions for the exam?", "10");
            int tempNum = 10;
            try {
                tempNum = Integer.parseInt(numStr.trim());
            } catch (Exception ex) {
                tempNum = 10;
            }
            final int numQuestions = tempNum;
            
            btnGenerateExam.setEnabled(false);
            examArea.setText("Generating mock exam... please wait.");
            
            new SwingWorker<ArrayList<ExamQuestion>, Void>() {
                @Override
                protected ArrayList<ExamQuestion> doInBackground() {
                    return generateMockExam(currentSummary, numQuestions);
                }
                
                @Override
                protected void done() {
                    try {
                        ArrayList<ExamQuestion> exam = get();
                        currentExam[0] = exam;
                        
                        if (exam.isEmpty()) {
                            examArea.setText("❌ Failed to generate exam.\n\n" +
                                "Possible reasons:\n" +
                                "• API Rate Limit: You've made too many requests. Wait 2-3 minutes.\n" +
                                "• API Error: The Gemini API might be temporarily unavailable.\n" +
                                "• Network Issue: Check your internet connection.\n\n" +
                                "💡 TIP: Space out your requests by 10-15 seconds to avoid rate limits.\n" +
                                "Free tier limit: 15 requests per minute.");
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append("═══════════════════════════════════════\n");
                            sb.append("        MOCK EXAM GENERATED\n");
                            sb.append("═══════════════════════════════════════\n\n");
                            sb.append("Total Questions: ").append(exam.size()).append("\n\n");
                            
                            for (int i = 0; i < exam.size(); i++) {
                                ExamQuestion q = exam.get(i);
                                sb.append("Q").append(i + 1).append(": ").append(q.question).append("\n");
                                if (q.options != null && q.options.length > 0) {
                                    for (int j = 0; j < q.options.length; j++) {
                                        sb.append("  ").append((char)('A' + j)).append(") ").append(q.options[j]).append("\n");
                                    }
                                }
                                sb.append("\n");
                            }
                            
                            examArea.setText(sb.toString());
                        }
                    } catch (Exception ex) {
                        examArea.setText("Error generating exam: " + ex.getMessage());
                    } finally {
                        btnGenerateExam.setEnabled(true);
                    }
                }
            }.execute();
        });
        
        btnTakeExam.addActionListener(e -> {
            if (currentExam[0] == null || currentExam[0].isEmpty()) {
                JOptionPane.showMessageDialog(null, 
                    "Generate an exam first!", 
                    "No Exam", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            takeMockExam(currentExam[0], examArea);
        });
        
        btnViewHistory.addActionListener(e -> {
            displayExamHistory(examArea);
        });
        
        controlPanel.add(btnGenerateExam);
        controlPanel.add(btnTakeExam);
        controlPanel.add(btnViewHistory);
        
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(controlPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // Helper methods for Mock Exam
    private static ArrayList<ExamQuestion> generateMockExam(String content, int numQuestions) {
        ArrayList<ExamQuestion> exam = new ArrayList<>();
        try {
            System.out.println("🔄 Starting mock exam generation...");
            
            // Limit content length to avoid token limits
            String limitedContent = content.length() > 3000 ? content.substring(0, 3000) + "..." : content;
            
            String prompt = "Generate " + numQuestions + " multiple choice questions (MCQs) for a mock exam based on the following content:\n\n"
                    + limitedContent
                    + "\n\nEach question must have four options labeled A, B, C, D, and end with the correct answer on its own line formatted like:\n"
                    + "Answer: <A/B/C/D>\n\nProvide questions numbered clearly.";
            
            System.out.println("📡 Calling Gemini API for exam questions...");
            String examText = callGeminiApiForSummary(prompt);
            System.out.println("✅ API Response received. Length: " + examText.length());
            System.out.println("📝 Response preview: " + examText.substring(0, Math.min(200, examText.length())));
            
            String[] blocks = examText.split("(?i)\\n?Q\\d*[:\\.]");
            System.out.println("📊 Split into " + blocks.length + " blocks");
            
            if (blocks.length <= 1) {
                blocks = examText.split("\\n\\s*\\n");
                System.out.println("📊 Re-split into " + blocks.length + " blocks");
            }
            
            for (String block : blocks) {
                String b = block.trim();
                if (b.isEmpty()) continue;
                
                String[] lines = b.split("\\r?\\n");
                ArrayList<String> options = new ArrayList<>();
                String answer = "A";
                String questionText = "";
                
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.matches("(?i)^Answer\\s*[:\\-]\\s*[A-D]$") || trimmed.matches("(?i)^Answer\\s*[:\\-]\\s*[A-D]\\b.*")) {
                        String a = trimmed.replaceAll("(?i).*([A-D]).*$", "$1").trim();
                        answer = a.toUpperCase();
                    } else if (trimmed.matches("^[A-D]\\).*")) {
                        options.add(trimmed.substring(2).trim());
                    } else if (!trimmed.isEmpty() && questionText.isEmpty()) {
                        questionText = trimmed;
                    }
                }
                
                if (!questionText.isEmpty() && options.size() == 4) {
                    exam.add(new ExamQuestion(questionText, options.toArray(new String[0]), answer, "General", 3, "MCQ"));
                    System.out.println("✅ Added question " + exam.size() + ": " + questionText.substring(0, Math.min(50, questionText.length())));
                } else {
                    System.out.println("⚠️ Skipped block - Question: " + (!questionText.isEmpty()) + ", Options: " + options.size());
                }
                
                if (exam.size() >= numQuestions) break;
            }
            
            System.out.println("✅ Mock exam generation complete. Total questions: " + exam.size());
            
        } catch (Exception ex) {
            System.out.println("❌ Mock exam generation failed: " + ex.getMessage());
            ex.printStackTrace();
        }
        return exam;
    }
    
    private static void takeMockExam(ArrayList<ExamQuestion> exam, JTextArea resultArea) {
        long startTime = System.currentTimeMillis();
        int score = 0;
        HashMap<String, Integer> topicScore = new HashMap<>();
        HashMap<String, Integer> topicMistake = new HashMap<>();
        
        StringBuilder log = new StringBuilder();
        
        for (int i = 0; i < exam.size(); i++) {
            ExamQuestion q = exam.get(i);
            
            String message = "Q" + (i + 1) + ": " + q.question + "\n\n";
            for (int j = 0; j < q.options.length; j++) {
                message += (char)('A' + j) + ") " + q.options[j] + "\n";
            }
            message += "\nChoose your answer:";
            
            String[] buttons = new String[]{"A", "B", "C", "D"};
            int choice = JOptionPane.showOptionDialog(
                    null,
                    message,
                    "Question " + (i + 1) + " of " + exam.size(),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    buttons,
                    null);
            
            String chosenLetter = (choice >= 0 && choice < 4) ? buttons[choice] : "";
            String correct = q.correctAnswer.trim().toUpperCase();
            
            boolean isCorrect = chosenLetter.equalsIgnoreCase(correct);
            if (isCorrect) {
                score++;
                topicScore.put(q.topic, topicScore.getOrDefault(q.topic, 0) + 1);
            } else {
                topicMistake.put(q.topic, topicMistake.getOrDefault(q.topic, 0) + 1);
            }
            
            log.append("Q").append(i + 1).append(": ").append(q.question).append("\n");
            log.append("Your Answer: ").append(chosenLetter).append(" | Correct: ").append(correct);
            log.append(isCorrect ? " ✅\n\n" : " ❌\n\n");
        }
        
        long endTime = System.currentTimeMillis();
        long timeTaken = (endTime - startTime) / 1000;
        
        ExamAttempt attempt = new ExamAttempt("Mock Exam", exam.size(), score);
        attempt.topicWiseScore = topicScore;
        attempt.topicWiseMistakes = topicMistake;
        attempt.timeTaken = timeTaken;
        examHistory.add(attempt);
        
        // Update topic mistakes for weak area tracking
        for (String topic : topicMistake.keySet()) {
            topicMistakes.put(topic, topicMistakes.getOrDefault(topic, 0) + topicMistake.get(topic));
        }
        
        StringBuilder result = new StringBuilder();
        result.append("═══════════════════════════════════════\n");
        result.append("        EXAM RESULTS\n");
        result.append("═══════════════════════════════════════\n\n");
        result.append("Score: ").append(score).append(" / ").append(exam.size());
        result.append(" (").append(String.format("%.1f", attempt.percentage)).append("%)\n");
        result.append("Time Taken: ").append(timeTaken).append(" seconds\n\n");
        result.append("Performance: ");
        if (attempt.percentage >= 90) result.append("🌟 Excellent!\n");
        else if (attempt.percentage >= 75) result.append("👍 Good!\n");
        else if (attempt.percentage >= 60) result.append("📈 Keep Improving!\n");
        else result.append("💪 Need More Practice!\n");
        result.append("\n");
        result.append(log);
        
        resultArea.setText(result.toString());
        
        JOptionPane.showMessageDialog(null, 
            "Exam Complete!\nScore: " + score + "/" + exam.size() + " (" + String.format("%.1f", attempt.percentage) + "%)",
            "Exam Results", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private static void displayExamHistory(JTextArea area) {
        if (examHistory.isEmpty()) {
            area.setText("No exam history yet. Take a mock exam to get started!");
            return;
        }
        
        StringBuilder history = new StringBuilder();
        history.append("═══════════════════════════════════════\n");
        history.append("        EXAM HISTORY\n");
        history.append("═══════════════════════════════════════\n\n");
        
        int attemptNum = 1;
        for (ExamAttempt attempt : examHistory) {
            history.append("Attempt #").append(attemptNum++).append("\n");
            history.append("─────────────────────────────────────\n");
            history.append("Date: ").append(attempt.date).append("\n");
            history.append("Score: ").append(attempt.correctAnswers).append("/").append(attempt.totalQuestions);
            history.append(" (").append(String.format("%.1f", attempt.percentage)).append("%)\n");
            history.append("Time: ").append(attempt.timeTaken).append(" seconds\n\n");
        }
        
        // Calculate average
        double avgScore = examHistory.stream().mapToDouble(a -> a.percentage).average().orElse(0);
        history.append("═══════════════════════════════════════\n");
        history.append("Average Score: ").append(String.format("%.1f", avgScore)).append("%\n");
        history.append("Total Attempts: ").append(examHistory.size()).append("\n");
        
        area.setText(history.toString());
    }
    

    // ===================== SUMMARIZER CORE (unchanged) =====================
    public static String summarize(String text, int numSentences) {
        if (text == null || text.isBlank()) return "No text provided.";
        try {
            String prompt = "Summarize the following text in exactly " + numSentences + " sentences: \n\n" + text;
            String summary = callGeminiApiForSummary(prompt);
            currentSummary = summary;
            return summary;
        } catch (Exception ex) {
            return "Summarization failed: " + ex.getMessage();
        }
    }

    private static void checkInToday() {
        String today = java.time.LocalDate.now().toString();
        if (lastStudyDate.isEmpty()) {
            currentStreak = 1;
            lastStudyDate = today;
            totalXP += 10;
        } else if (lastStudyDate.equals(today)) {
            // Already checked in today
            return;
        } else {
            java.time.LocalDate last = java.time.LocalDate.parse(lastStudyDate);
            java.time.LocalDate now = java.time.LocalDate.now();
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(last, now);
            
            if (daysBetween == 1) {
                currentStreak++;
                totalXP += 10 + (currentStreak * 2); // Bonus XP for longer streaks
                
                // Award badges for milestones
                if (currentStreak == 3 && !badges.contains("🔥 3-Day Streak")) {
                    badges.add("🔥 3-Day Streak");
                }
                if (currentStreak == 7 && !badges.contains("⭐ Week Warrior")) {
                    badges.add("⭐ Week Warrior");
                }
                if (currentStreak == 30 && !badges.contains("💎 Monthly Master")) {
                    badges.add("💎 Monthly Master");
                }
            } else {
                // Streak broken
                currentStreak = 1;
                totalXP += 10;
            }
            lastStudyDate = today;
        }
        
        // Update weekly streak
        if (currentStreak % 7 == 0) {
            weeklyStreak = currentStreak / 7;
        }
    }

    private static void generateNewChallenges() {
        String[] dailyChallenges = {
            "Finish 3 quizzes today",
            "Create 2 summaries today",
            "Generate 5 descriptive questions",
            "Score 80% or higher in any quiz",
            "Study for 30 minutes",
            "Review yesterday's notes"
        };
        
        String[] weeklyChallenges = {
            "Complete 10 quizzes this week",
            "Score 80% average in all quizzes this week",
            "Create summaries for 5 different topics",
            "Maintain a 7-day study streak",
            "Generate 20 descriptive questions this week",
            "Review all saved summaries"
        };
        
        Random rand = new Random();
        currentDailyChallenge = dailyChallenges[rand.nextInt(dailyChallenges.length)];
        currentWeeklyChallenge = weeklyChallenges[rand.nextInt(weeklyChallenges.length)];
        dailyChallengeProgress = 0;
        weeklyChallengeProgress = 0;
    }

    private static void updateStreakDisplay(JTextArea area) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("  🔥 CURRENT DAILY STREAK: ").append(currentStreak).append(" days\n\n");
        
        if (currentStreak >= 7) {
            sb.append("  ⭐ WEEKLY STREAK: ").append(weeklyStreak).append(" weeks\n\n");
        }
        
        if (currentStreak > 0) {
            sb.append("  💪 You've studied for ").append(currentStreak).append(" consecutive days!\n");
            sb.append("     Keep it up! 🎯\n\n");
        } else {
            sb.append("  Start your streak today! Check in to begin. 🚀\n\n");
        }
        
        sb.append("  📅 Last Study Date: ").append(lastStudyDate.isEmpty() ? "Never" : lastStudyDate).append("\n");
        sb.append("  ⚡ Total XP: ").append(totalXP).append("\n\n");
        
        if (currentStreak >= 3) {
            sb.append("  🎉 Amazing dedication! Keep going!\n");
        }
        
        area.setText(sb.toString());
    }

    private static void updateChallengesDisplay(JTextArea area) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("  📋 DAILY CHALLENGE\n");
        sb.append("  " + "─".repeat(50) + "\n");
        if (currentDailyChallenge.isEmpty()) {
            sb.append("  ✅ Daily challenge completed! Come back tomorrow.\n");
        } else {
            sb.append("  🎯 ").append(currentDailyChallenge).append("\n");
            sb.append("  💰 Reward: +50 XP\n");
            sb.append("  Progress: ").append(dailyChallengeProgress > 0 ? "In Progress" : "Not Started").append("\n");
        }
        sb.append("\n\n");
        
        sb.append("  📅 WEEKLY CHALLENGE\n");
        sb.append("  " + "─".repeat(50) + "\n");
        if (currentWeeklyChallenge.isEmpty()) {
            sb.append("  ✅ Weekly challenge completed! New one next week.\n");
        } else {
            sb.append("  🏆 ").append(currentWeeklyChallenge).append("\n");
            sb.append("  💰 Reward: +200 XP + Badge\n");
            sb.append("  Progress: ").append(weeklyChallengeProgress > 0 ? "In Progress" : "Not Started").append("\n");
        }
        sb.append("\n\n");
        
        sb.append("  💡 TIP: Complete challenges to earn XP and unlock badges!\n");
        sb.append("  🎁 Higher streaks = More bonus XP!\n");
        
        area.setText(sb.toString());
    }

    private static String callGeminiApiForSummary(String prompt) throws Exception {
        int maxRetries = 5;
        int retryDelay = 5000; // Start with 5 seconds
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro-latest:generateContent?key=" + GEMINI_API_KEY;
                HttpClient client = HttpClient.newHttpClient();

                JSONObject textPart = new JSONObject();
                textPart.put("text", prompt);

                JSONArray partsArray = new JSONArray();
                partsArray.put(textPart);

                JSONObject content = new JSONObject();
                content.put("parts", partsArray);

                JSONArray contentsArray = new JSONArray();
                contentsArray.put(content);

                JSONObject requestBodyJson = new JSONObject();
                requestBodyJson.put("contents", contentsArray);

                String requestBody = requestBodyJson.toString();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    if (candidates.length() > 0) {
                        return candidates.getJSONObject(0).getJSONObject("content")
                                .getJSONArray("parts").getJSONObject(0).getString("text");
                    } else return "No content could be generated.";
                } else if (response.statusCode() == 503 || response.statusCode() == 429 || response.statusCode() == 500) {
                    // Service unavailable, rate limit, or internal error - retry
                    if (attempt < maxRetries - 1) {
                        System.out.println("⚠️ API busy (attempt " + (attempt + 1) + "/" + maxRetries + "). Retrying in " + (retryDelay/1000) + " seconds...");
                        Thread.sleep(retryDelay);
                        retryDelay *= 2; // Exponential backoff
                        continue;
                    }
                    throw new Exception("API temporarily unavailable. Please wait a few minutes and try again. (Status: " + response.statusCode() + ")");
                } else {
                    throw new Exception("API Error (" + response.statusCode() + "): " + response.body());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Request interrupted");
            } catch (Exception e) {
                if (attempt == maxRetries - 1) {
                    throw e; // Last attempt, throw the error
                }
                // For other exceptions, retry
                System.out.println("⚠️ Error (attempt " + (attempt + 1) + "/" + maxRetries + "): " + e.getMessage());
                Thread.sleep(retryDelay);
                retryDelay *= 2;
            }
        }
        throw new Exception("Failed after " + maxRetries + " attempts");
    }

    // ===================== AI QUIZ (Gemini API) - returns MCQs parsed =====================
    private static ArrayList<Question> generateAIQuiz(String summary, int numQuestions) {
        ArrayList<Question> quiz = new ArrayList<>();
        try {
            // Limit summary length to avoid token limits
            String limitedSummary = summary.length() > 2000 ? summary.substring(0, 2000) + "..." : summary;
            
            String prompt = "Generate " + numQuestions + " multiple choice questions (MCQs) based on the following summary:\n\n"
                    + limitedSummary
                    + "\n\nEach question must have four options labeled A, B, C, D, and end with the correct answer on its own line formatted like:\n"
                    + "Answer: <A/B/C/D>\n\nProvide questions numbered or separated clearly.";

            String quizText = callGeminiApiForSummary(prompt);

            // Attempt to robustly parse blocks
            String[] blocks = quizText.split("(?i)\\n?Q\\d*[:\\.]"); // split by Q, Q1:, Q2.
            if (blocks.length <= 1) {
                // If no Q detected, fallback to splitting by double newlines
                blocks = quizText.split("\\n\\s*\\n");
            }

            for (String block : blocks) {
                String b = block.trim();
                if (b.isEmpty()) continue;
                // Extract question lines and answer
                String[] lines = b.split("\\r?\\n");
                StringBuilder qBuilder = new StringBuilder();
                String answer = "A";
                for (String line : lines) {
                    if (line.trim().matches("(?i)^Answer\\s*[:\\-]\\s*[A-D]$") || line.trim().matches("(?i)^Answer\\s*[:\\-]\\s*[A-D]\\b.*")) {
                        // grab last character A-D
                        String a = line.replaceAll("(?i).*([A-D]).*$", "$1").trim();
                        answer = a.toUpperCase();
                        break;
                    } else {
                        qBuilder.append(line).append("\n");
                    }
                }
                String qText = qBuilder.toString().trim();
                if (!qText.isEmpty()) {
                    quiz.add(new Question(qText, answer));
                }
                if (quiz.size() >= numQuestions) break;
            }

        } catch (Exception ex) {
            System.out.println("❌ Quiz generation failed: " + ex.getMessage());
        }
        return quiz;
    }

    // ===================== CONSOLE QUIZ (unused) - keep for compatibility =====================
    public static int conductMCQQuiz(ArrayList<Question> quiz, Scanner sc) {
        System.out.println(CYAN + "\n📘 === MCQ QUIZ START ===" + RESET);
        int score = 0;
        for (int i = 0; i < quiz.size(); i++) {
            Question q = quiz.get(i);
            System.out.println(BLUE + "\nQ" + (i + 1) + ":" + RESET);
            System.out.println(q.questionText);
            System.out.print(YELLOW + "👉 Your answer (A/B/C/D): " + RESET);
            String ans = sc.nextLine().trim().toUpperCase();
            if (ans.equals(q.answer.toUpperCase())) {
                System.out.println(GREEN + "✅ Correct!\n" + RESET);
                score++;
            } else System.out.println(RED + "❌ Wrong. Correct: " + q.answer + "\n" + RESET);
        }
        System.out.println(GREEN + "🏆 Score: " + score + "/" + quiz.size() + RESET);
        return score;
    }

    // ===================== DESCRIPTIVE QUESTIONS (local logic unchanged) =====================
    public static void generateDescriptiveQuestions(String content, Scanner sc) {
        if (content == null || content.isBlank()) {
            System.out.println(RED + "⚠️ No content found to generate questions!" + RESET);
            return;
        }

        System.out.print(YELLOW + "\n🧾 How many descriptive questions do you want? " + RESET);
        int numQ = getIntInput(sc);
        sc.nextLine();

        System.out.print(YELLOW + "🎯 Enter marks type (2 / 5 / 10 / 16): " + RESET);
        int marks = getIntInput(sc);
        sc.nextLine();

        ArrayList<Question> descQuestions = generateQuestionsFromContent(content, numQ, marks);

        System.out.println(CYAN + "\n📘 === DESCRIPTIVE QUESTIONS (" + marks + " Marks) ===" + RESET);
        for (int i = 0; i < descQuestions.size(); i++) {
            Question q = descQuestions.get(i);
            System.out.println(BLUE + "\nQ" + (i + 1) + ". " + q.questionText + RESET);
            System.out.println(GREEN + "Answer:\n" + q.answer + RESET);
        }
    }

    public static ArrayList<Question> generateQuestionsFromContent(String text, int numQ, int marks) {
        ArrayList<Question> questions = new ArrayList<>();
        if (text == null || text.isBlank()) return questions;

        // Using DATA STRUCTURES in the code implementation:
        // 1. Stack - for processing sentences in LIFO order
        // 2. Queue - for processing sentences in FIFO order
        // 3. LinkedHashSet - to maintain unique sentences with insertion order
        // 4. HashMap - to store keyword frequencies
        
        String[] sentences = text.split("(?<=[.!?])\\s+");
        
        // Using LinkedHashSet to remove duplicates while maintaining order
        LinkedHashSet<String> uniqueSentences = new LinkedHashSet<>(Arrays.asList(sentences));
        
        // Using Stack to process sentences in reverse order (LIFO)
        Stack<String> sentenceStack = new Stack<>();
        for (String s : uniqueSentences) {
            if (s.trim().length() > 30 && !s.contains("?")) {
                sentenceStack.push(s.trim());
            }
        }
        
        // Using Queue for FIFO processing of sentences
        Queue<String> sentenceQueue = new LinkedList<>();
        while (!sentenceStack.isEmpty()) {
            sentenceQueue.offer(sentenceStack.pop());
        }
        
        // Using HashMap to track keyword frequencies
        HashMap<String, Integer> keywordFrequency = new HashMap<>();
        
        if (sentenceQueue.isEmpty()) {
            System.out.println(RED + "⚠️ Not enough content to generate questions." + RESET);
            return questions;
        }

        Random random = new Random();
        
        for (int i = 0; i < numQ; i++) {
            if (sentenceQueue.isEmpty()) break;
            
            // Poll from queue (FIFO - First In First Out)
            String base = sentenceQueue.poll();
            String keyword = extractKeyword(base);
            
            // Update keyword frequency in HashMap
            keywordFrequency.put(keyword, keywordFrequency.getOrDefault(keyword, 0) + 1);
            
            String qText;
            switch (marks) {
                case 2 -> qText = "What is " + keyword + "?";
                case 5 -> qText = "Explain " + keyword + " briefly with an example.";
                case 10 -> qText = "Discuss in detail about " + keyword + " and its importance.";
                case 16 -> qText = "Write an elaborate note on " + keyword + ", covering all key aspects.";
                default -> qText = "Explain " + keyword + ".";
            }
            
            // Generate answer using the base sentence
            String answer = generateAnswerFromSentence(base, marks, keyword);
            questions.add(new Question(qText, answer));
        }
        
        // Display keyword frequency statistics using HashMap
        System.out.println("\n📊 Keyword Frequency Analysis (using HashMap):");
        for (Map.Entry<String, Integer> entry : keywordFrequency.entrySet()) {
            System.out.println("  • " + entry.getKey() + ": " + entry.getValue() + " times");
        }
        
        return questions;
    }
    
    public static String generateAnswerFromSentence(String sentence, int marks, String keyword) {
        StringBuilder answer = new StringBuilder();
        
        // Generate answer based on marks
        switch (marks) {
            case 2 -> {
                answer.append(sentence);
                if (sentence.length() < 100) {
                    answer.append(" This is a fundamental concept that helps in understanding the topic better.");
                }
            }
            case 5 -> {
                answer.append(sentence).append("\n\n");
                answer.append("Key Points:\n");
                answer.append("• ").append(keyword).append(" is an important concept in this domain.\n");
                answer.append("• It helps in organizing and managing information effectively.\n");
                answer.append("• Understanding ").append(keyword).append(" is crucial for practical applications.\n");
                answer.append("\nExample: ").append(keyword).append(" can be applied in various real-world scenarios.");
            }
            case 10 -> {
                answer.append("Introduction:\n");
                answer.append(sentence).append("\n\n");
                answer.append("Detailed Explanation:\n");
                answer.append("• ").append(keyword).append(" plays a vital role in the overall understanding of the subject.\n");
                answer.append("• It provides a structured approach to problem-solving.\n");
                answer.append("• The concept involves multiple aspects that need to be considered.\n\n");
                answer.append("Importance:\n");
                answer.append("• Helps in efficient data management\n");
                answer.append("• Improves system performance\n");
                answer.append("• Enables better decision-making\n\n");
                answer.append("Applications:\n");
                answer.append("• Used in software development\n");
                answer.append("• Applied in database systems\n");
                answer.append("• Essential for algorithm design");
            }
            case 16 -> {
                answer.append("COMPREHENSIVE ANALYSIS\n\n");
                answer.append("1. INTRODUCTION:\n");
                answer.append(sentence).append("\n\n");
                answer.append("2. FUNDAMENTAL CONCEPTS:\n");
                answer.append("• ").append(keyword).append(" is a cornerstone concept in computer science.\n");
                answer.append("• It encompasses various principles and methodologies.\n");
                answer.append("• Understanding its core principles is essential for advanced topics.\n\n");
                answer.append("3. DETAILED EXPLANATION:\n");
                answer.append("• The concept involves multiple layers of abstraction.\n");
                answer.append("• Each layer serves a specific purpose in the overall architecture.\n");
                answer.append("• Implementation requires careful consideration of various factors.\n\n");
                answer.append("4. PRACTICAL APPLICATIONS:\n");
                answer.append("• Software Engineering: Used in designing scalable systems\n");
                answer.append("• Database Management: Essential for query optimization\n");
                answer.append("• Algorithm Design: Fundamental for efficient problem-solving\n");
                answer.append("• System Architecture: Critical for performance tuning\n\n");
                answer.append("5. ADVANTAGES AND BENEFITS:\n");
                answer.append("• Improves code efficiency and maintainability\n");
                answer.append("• Reduces time and space complexity\n");
                answer.append("• Enhances system scalability\n");
                answer.append("• Facilitates better resource management\n\n");
                answer.append("6. CHALLENGES AND CONSIDERATIONS:\n");
                answer.append("• Requires thorough understanding of underlying principles\n");
                answer.append("• Implementation complexity may vary\n");
                answer.append("• Trade-offs between different approaches need evaluation\n\n");
                answer.append("7. CONCLUSION:\n");
                answer.append("In summary, ").append(keyword).append(" is an essential concept that forms the foundation ");
                answer.append("for many advanced topics. Mastering this concept is crucial for anyone working in ");
                answer.append("the field of computer science and software development.");
            }
            default -> answer.append(sentence);
        }
        
        return answer.toString();
    }

    public static String generateAnswerText(String base, int marks, ArrayList<String> context, Random rand) {
        int minLines = 2, maxLines = 3;
        if (marks == 5) { minLines = 4; maxLines = 6; }
        else if (marks == 10) { minLines = 7; maxLines = 10; }
        else if (marks == 16) { minLines = 11; maxLines = 16; }

        StringBuilder sb = new StringBuilder();
        
        // Generate generic data structures answer based on marks
        if (marks == 2) {
            sb.append("A data structure is a specialized format for organizing, processing, and storing data. ");
            sb.append("It provides a way to manage large amounts of data efficiently for uses such as large databases and internet indexing services.");
        } else if (marks == 5) {
            sb.append("Data structures are fundamental concepts in computer science that allow efficient data organization and manipulation.\n\n");
            sb.append("• They provide different ways to store and organize data based on specific requirements.\n");
            sb.append("• Common operations include insertion, deletion, searching, and traversal.\n");
            sb.append("• Each data structure has its own advantages and use cases.\n");
            sb.append("• Time and space complexity vary depending on the implementation.\n");
            sb.append("\nExample: Arrays provide O(1) access time but fixed size, while Linked Lists offer dynamic size but O(n) access time.");
        } else if (marks == 10) {
            sb.append("Data structures are essential building blocks in computer programming and software development.\n\n");
            sb.append("Key Concepts:\n");
            sb.append("• Linear Data Structures: Arrays, Linked Lists, Stacks, Queues\n");
            sb.append("• Non-Linear Data Structures: Trees, Graphs, Heaps\n");
            sb.append("• Hash-based Structures: Hash Tables, Hash Maps\n\n");
            sb.append("Implementation Details:\n");
            sb.append("• Arrays: Contiguous memory allocation, O(1) access, fixed size\n");
            sb.append("• Linked Lists: Dynamic memory allocation, O(n) access, flexible size\n");
            sb.append("• Stacks: LIFO principle, used in recursion and expression evaluation\n");
            sb.append("• Queues: FIFO principle, used in scheduling and buffering\n\n");
            sb.append("Time Complexity Analysis:\n");
            sb.append("• Different operations have different time complexities\n");
            sb.append("• Choice of data structure impacts algorithm efficiency\n");
            sb.append("• Trade-offs between time and space complexity must be considered");
        } else { // 16 marks
            sb.append("COMPREHENSIVE OVERVIEW OF DATA STRUCTURES\n\n");
            sb.append("1. INTRODUCTION:\n");
            sb.append("Data structures are specialized formats for organizing, processing, retrieving, and storing data. ");
            sb.append("They are fundamental to creating efficient algorithms and software systems.\n\n");
            sb.append("2. LINEAR DATA STRUCTURES:\n");
            sb.append("• Arrays: Fixed-size, contiguous memory, O(1) random access\n");
            sb.append("• Linked Lists: Dynamic size, non-contiguous memory, O(n) access\n");
            sb.append("  - Singly Linked List: One-way traversal\n");
            sb.append("  - Doubly Linked List: Two-way traversal\n");
            sb.append("  - Circular Linked List: Last node points to first\n");
            sb.append("• Stacks: LIFO (Last In First Out) principle\n");
            sb.append("  - Applications: Function calls, expression evaluation, undo operations\n");
            sb.append("  - Operations: Push O(1), Pop O(1), Peek O(1)\n");
            sb.append("• Queues: FIFO (First In First Out) principle\n");
            sb.append("  - Applications: CPU scheduling, printer spooling, BFS traversal\n");
            sb.append("  - Variants: Circular Queue, Priority Queue, Deque\n\n");
            sb.append("3. NON-LINEAR DATA STRUCTURES:\n");
            sb.append("• Trees: Hierarchical structure with root and child nodes\n");
            sb.append("  - Binary Tree: Each node has at most 2 children\n");
            sb.append("  - Binary Search Tree: Left < Parent < Right property\n");
            sb.append("  - AVL Tree: Self-balancing BST\n");
            sb.append("  - B-Trees: Used in databases and file systems\n");
            sb.append("• Graphs: Collection of vertices and edges\n");
            sb.append("  - Directed vs Undirected\n");
            sb.append("  - Weighted vs Unweighted\n");
            sb.append("  - Representations: Adjacency Matrix, Adjacency List\n");
            sb.append("• Heaps: Complete binary tree with heap property\n");
            sb.append("  - Min Heap: Parent < Children\n");
            sb.append("  - Max Heap: Parent > Children\n");
            sb.append("  - Applications: Priority queues, heap sort\n\n");
            sb.append("4. HASH-BASED STRUCTURES:\n");
            sb.append("• Hash Tables: Key-value pairs with O(1) average access\n");
            sb.append("• Collision Resolution: Chaining, Open Addressing\n");
            sb.append("• Load Factor: Ratio of elements to table size\n\n");
            sb.append("5. TIME COMPLEXITY COMPARISON:\n");
            sb.append("• Array: Access O(1), Search O(n), Insert O(n), Delete O(n)\n");
            sb.append("• Linked List: Access O(n), Search O(n), Insert O(1), Delete O(1)\n");
            sb.append("• BST: Average O(log n) for all operations\n");
            sb.append("• Hash Table: Average O(1) for all operations\n\n");
            sb.append("6. REAL-WORLD APPLICATIONS:\n");
            sb.append("• Operating Systems: Process scheduling, memory management\n");
            sb.append("• Databases: Indexing, query optimization\n");
            sb.append("• Networking: Routing algorithms, packet scheduling\n");
            sb.append("• Compilers: Symbol tables, syntax trees\n\n");
            sb.append("CONCLUSION:\n");
            sb.append("Understanding data structures is crucial for writing efficient code and solving complex computational problems. ");
            sb.append("The choice of appropriate data structure significantly impacts program performance and resource utilization.");
        }
        
        return sb.toString().trim();
    }

    public static String extractKeyword(String sentence) {
        String[] words = sentence.split("\\s+");
        if (words.length < 3) return "the topic";
        for (String w : words)
            if (Character.isUpperCase(w.charAt(0)) && w.length() > 3)
                return w.replaceAll("[^a-zA-Z0-9]", "");
        return words[Math.min(2, words.length - 1)].replaceAll("[^a-zA-Z0-9]", "");
    }

    // ===================== FILE & UTILS =====================
    public static void saveSummary(String summary, int score) {
        try (FileWriter fw = new FileWriter("saved_summaries.txt", true)) {
            fw.write("\n==== Summary ====\n" + summary + "\nScore: " + score + "\n=================\n");
            System.out.println(GREEN + "✅ Summary saved!" + RESET);
        } catch (IOException ex) {
            System.out.println(RED + "❌ Error saving summary: " + ex.getMessage() + RESET);
        }
    }

    public static String readFile(String path) {
        try {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
        } catch (IOException ex) {
            return "";
        }
    }

    public static int getIntInput(Scanner sc) {
        while (true) {
            try {
                return sc.nextInt();
            } catch (InputMismatchException ex) {
                System.out.print(RED + "⚠️ Enter a valid number: " + RESET);
                sc.next();
            }
        }
    }

    public static void viewSavedSummaries() {
        String data = readFile("saved_summaries.txt");
        if (data.isBlank()) System.out.println(RED + "⚠️ No saved summaries!" + RESET);
        else System.out.println(CYAN + "\n📚 SAVED SUMMARIES\n" + RESET + data);
    }

    public static void searchSavedSummaries(Scanner sc) {
        String data = readFile("saved_summaries.txt");
        if (data.isBlank()) {
            System.out.println(RED + "⚠️ No summaries found!" + RESET);
            return;
        }
        System.out.print(YELLOW + "Enter keyword: " + RESET);
        String key = sc.nextLine().toLowerCase();
        for (String s : data.split("==== Summary ===="))
            if (s.toLowerCase().contains(key))
                System.out.println(GREEN + "\nMatch:\n" + RESET + s);
    }

    public static void viewUserAnalytics() {
        String data = readFile("saved_summaries.txt");
        if (data.isBlank()) {
            System.out.println(RED + "⚠️ No analytics!" + RESET);
            return;
        }
        int count = data.split("==== Summary ====").length - 1;
        int total = 0;
        for (String line : data.split("\n"))
            if (line.startsWith("Score:"))
                total += Integer.parseInt(line.replace("Score:", "").trim());
        System.out.println(GREEN + "\n📊 Total Summaries: " + count + " | Avg Score: " + (count == 0 ? 0 : total / count) + RESET);
    }


    public static void printHeader(String title) {
        System.out.println(BOLD + BLUE + "\n===============================");
        System.out.println("⭐ " + title);
        System.out.println("===============================" + RESET);
    }
}
