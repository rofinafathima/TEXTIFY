import java.io.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;

class Question {
    String questionText;
    String answer;
    Question(String questionText, String answer) {
        this.questionText = questionText;
        this.answer = answer;
    }
}

public class SmartNotesApp {

    static String currentUser = "guest";

    // ✅ Database connection details
    static final String URL = "jdbc:mysql://localhost:3306/smartnotes";
    static final String USER = "root";
    static final String PASS = "Rofi@1013";

    // ANSI colors
    static final String RESET = "\u001B[0m";
    static final String CYAN = "\u001B[36m";
    static final String GREEN = "\u001B[32m";
    static final String YELLOW = "\u001B[33m";
    static final String RED = "\u001B[31m";
    static final String BLUE = "\u001B[34m";
    static final String BOLD = "\u001B[1m";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try {
            // ✅ Connect to MySQL
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
                        openMainApp(sc); 
                    }
                } else if (choice == 3) {
                    System.out.println(GREEN + "👋 Goodbye!" + RESET);
                    conn.close();
                    return;
                } else System.out.println(RED + "❌ Invalid choice!" + RESET);
            }

        } catch (Exception e) {
            System.out.println(RED + "❌ Error: " + e.getMessage() + RESET);
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

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println(RED + "⚠️ Username already exists!" + RESET);
        } catch (SQLException e) {
            System.out.println(RED + "❌ Signup error: " + e.getMessage() + RESET);
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
        } catch (SQLException e) {
            System.out.println(RED + "❌ Login error: " + e.getMessage() + RESET);
        }
        return false;
    }

    // ===================== SMART NOTES APP =====================
    private static void openMainApp(Scanner sc) {
        printHeader("AI NOTES SUMMARIZER + QUIZ APP");

        while (true) {
            printHeader("MAIN MENU");
            System.out.println("1️⃣ Summarize Text (Console Input)");
            System.out.println("2️⃣ Summarize Text (File Input)");
            System.out.println("3️⃣ View Saved Summaries");
            System.out.println("4️⃣ Search Saved Summaries 🔍");
            System.out.println("5️⃣ View Analytics");
            System.out.println("6️⃣ Chat with Notes");
            System.out.println("7️⃣ Exit");
            System.out.print(YELLOW + "👉 Choose option: " + RESET);

            int choice = getIntInput(sc);
            sc.nextLine();

            if (choice == 1 || choice == 2) handleSummarize(sc, choice);
            else if (choice == 3) viewSavedSummaries();
            else if (choice == 4) searchSavedSummaries(sc);
            else if (choice == 5) viewUserAnalytics();
            else if (choice == 6) chatWithNotes(sc);
            else if (choice == 7) {
                System.out.println(GREEN + "\n👋 Goodbye, " + currentUser + "!" + RESET);
                break;
            } else System.out.println(RED + "❌ Invalid option!" + RESET);
        }
    }

    // ===================== SUMMARIZER =====================
    private static void handleSummarize(Scanner sc, int choice) {
        String inputText = "";
        if (choice == 1) {
            System.out.println(CYAN + "\n📝 Enter text (type END to finish):" + RESET);
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = sc.nextLine();
                if (line.equalsIgnoreCase("END")) break;
                sb.append(line).append(" ");
            }
            inputText = sb.toString();
        } else {
            System.out.print(YELLOW + "📄 Enter file path: " + RESET);
            String filePath = sc.nextLine();
            inputText = readFile(filePath);
        }

        System.out.print(YELLOW + "\n✂️ Sentences for su1mmary: " + RESET);
        int num = getIntInput(sc);
        sc.nextLine();

        String summary = summarize(inputText, num);
        showSummaryGUI(summary);

        // ✅ MCQ Quiz
        System.out.print(YELLOW + "\n🎯 How many quiz questions (1–5)? " + RESET);
        int numQ = Math.min(Math.max(getIntInput(sc), 1), 5);
        sc.nextLine();
        ArrayList<Question> quiz = generateMCQQuiz(summary, numQ);
        int score = conductMCQQuiz(quiz, sc);

        // ✅ Descriptive Questions
        generateDescriptiveQuestions(summary, sc);

        System.out.print(YELLOW + "💾 Save summary & score? (yes/no): " + RESET);
        if (sc.nextLine().equalsIgnoreCase("yes")) saveSummary(summary, score);
    }

    // ===================== SUMMARIZER CORE =====================
    public static String summarize(String text, int numSentences) {
        if (text.isBlank()) return "No text provided.";
        String[] sentences = text.split("(?<=[.!?])\\s+");
        ArrayList<String> sList = new ArrayList<>(Arrays.asList(sentences));
        HashMap<String, Integer> freq = new HashMap<>();

        for (String w : text.toLowerCase().split("\\W+"))
            if (!w.isBlank()) freq.put(w, freq.getOrDefault(w, 0) + 1);

        sList.sort((a, b) -> scoreSentence(b, freq) - scoreSentence(a, freq));
        return String.join(" ", sList.subList(0, Math.min(numSentences, sList.size())));
    }

    private static int scoreSentence(String s, Map<String, Integer> freq) {
        int score = 0;
        for (String w : s.toLowerCase().split("\\W+"))
            score += freq.getOrDefault(w, 0);
        return score;
    }

    // ===================== GUI =====================
    public static void showSummaryGUI(String summary) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("🧠 Generated Summary");
            f.setSize(600, 400);
            JTextArea area = new JTextArea(summary);
            area.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 15));
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setEditable(false);
            f.add(new JScrollPane(area));
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    // ===================== MCQ QUIZ =====================
    public static ArrayList<Question> generateMCQQuiz(String summary, int numQuestions) {
        ArrayList<Question> quiz = new ArrayList<>();
        ArrayList<String> sentences = new ArrayList<>(Arrays.asList(summary.split("(?<=[.!?])\\s+")));
        Random rand = new Random();

        for (String s : sentences) {
            String[] words = s.split("\\s+");
            if (words.length > 3) {
                String answer = words[2].replaceAll("[^a-zA-Z]", "");
                ArrayList<String> options = new ArrayList<>();
                options.add(answer);
                for (int i = 0; i < 3; i++) options.add("Option" + rand.nextInt(100));
                Collections.shuffle(options);

                StringBuilder qText = new StringBuilder(s.replaceFirst(words[2], "____") + "\n");
                char opt = 'A';
                char correct = 'A';
                for (String o : options) {
                    qText.append(opt).append(". ").append(o).append("\n");
                    if (o.equals(answer)) correct = opt;
                    opt++;
                }
                quiz.add(new Question(qText.toString(), String.valueOf(correct)));
            }
            if (quiz.size() >= numQuestions) break;
        }
        return quiz;
    }

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

    // ===================== DESCRIPTIVE QUESTIONS =====================
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

        String[] sentences = text.split("(?<=[.!?])\\s+");
        LinkedHashSet<String> uniqueSentences = new LinkedHashSet<>(Arrays.asList(sentences));
        ArrayList<String> keySentences = new ArrayList<>();

        for (String s : uniqueSentences)
            if (s.trim().length() > 30 && !s.contains("?")) keySentences.add(s.trim());

        if (keySentences.isEmpty()) {
            System.out.println(RED + "⚠️ Not enough content to generate questions." + RESET);
            return questions;
        }

        Random random = new Random();
        Queue<String> sentenceQueue = new LinkedList<>(keySentences);

        for (int i = 0; i < numQ; i++) {
            if (sentenceQueue.isEmpty()) break;
            String base = sentenceQueue.poll();
            String keyword = extractKeyword(base);
            String qText;
            switch (marks) {
                case 2 -> qText = "What is " + keyword + "?";
                case 5 -> qText = "Explain " + keyword + " briefly with an example.";
                case 10 -> qText = "Discuss in detail about " + keyword + " and its importance.";
                case 16 -> qText = "Write an elaborate note on " + keyword + ", covering all key aspects.";
                default -> qText = "Explain " + keyword + ".";
            }
            String answer = generateAnswerText(base, marks, keySentences, random);
            questions.add(new Question(qText, answer));
        }
        return questions;
    }

    public static String generateAnswerText(String base, int marks, ArrayList<String> context, Random rand) {
        int minLines = 2, maxLines = 3;
        if (marks == 5) { minLines = 4; maxLines = 6; }
        else if (marks == 10) { minLines = 7; maxLines = 10; }
        else if (marks == 16) { minLines = 11; maxLines = 16; }

        int lines = rand.nextInt(maxLines - minLines + 1) + minLines;
        String keyword = extractKeyword(base);
        StringBuilder sb = new StringBuilder();
        sb.append("• ").append(base).append("\n");

        for (int i = 1; i < lines; i++) {
            String extra = context.get(rand.nextInt(context.size()));
            if (!extra.equals(base)) {
                sb.append("• ").append(extra);
                if (i % 3 == 0)
                    sb.append(" This further explains the concept of ").append(keyword).append(".");
                sb.append("\n");
            }
        }

        sb.append("In summary, ").append(keyword)
          .append(" plays a crucial role in this topic and helps understand it better.");
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
        } catch (IOException e) {
            System.out.println(RED + "❌ Error saving summary: " + e.getMessage() + RESET);
        }
    }

    public static String readFile(String path) {
        try {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
        } catch (IOException e) {
            return "";
        }
    }

    public static int getIntInput(Scanner sc) {
        while (true) {
            try {
                return sc.nextInt();
            } catch (InputMismatchException e) {
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

    public static void chatWithNotes(Scanner sc) {
        String data = readFile("saved_summaries.txt");
        if (data.isBlank()) {
            System.out.println(RED + "⚠️ No notes found!" + RESET);
            return;
        }
        System.out.println(CYAN + "\n💬 Chat with your notes (type 'exit' to quit)" + RESET);
        while (true) {
            System.out.print(YELLOW + "You: " + RESET);
            String q = sc.nextLine();
            if (q.equalsIgnoreCase("exit")) break;
            String ans = findRelevantAnswer(data, q);
            System.out.println(BLUE + "NotesBot: " + RESET + ans);
        }
    }

    private static String findRelevantAnswer(String content, String question) {
        String[] sums = content.split("==== Summary ====");
        String best = "";
        int score = 0;
        for (String s : sums) {
            int sc = 0;
            for (String w : question.toLowerCase().split("\\W+"))
                if (s.toLowerCase().contains(w)) sc++;
            if (sc > score) {
                score = sc;
                best = s.trim();
            }
        }
        return best.isEmpty() ? "Sorry, nothing relevant found." : best.split("\n")[0];
    }

    public static void printHeader(String title) {
        System.out.println(BOLD + BLUE + "\n===============================");
        System.out.println("⭐ " + title);
        System.out.println("===============================" + RESET);
    }
}
