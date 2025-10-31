import java.awt.*;
import java.util.*;
import javax.swing.*;

public class SmartNotesConsole {

    // ---- Console data ----
    static Scanner sc = new Scanner(System.in);
    static ArrayList<String> notes = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("🧠 Welcome to SmartNotes!");
        while (true) {
            System.out.println("\n=== Main Menu ===");
            System.out.println("1. Add a Note");
            System.out.println("2. Summarize Notes");
            System.out.println("3. Generate Quiz");
            System.out.println("4. Open GUI Mode");
            System.out.println("5. Exit");
            System.out.print("Choose: ");
            int ch = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (ch) {
                case 1 -> addNote();
                case 2 -> summarizeNotes();
                case 3 -> generateQuiz();
                case 4 -> new SmartNotesGUI(notes);
                case 5 -> {
                    System.out.println("👋 Goodbye!");
                    System.exit(0);
                }
                default -> System.out.println("❌ Invalid choice!");
            }
        }
    }

    static void addNote() {
        System.out.print("Enter your note: ");
        String note = sc.nextLine();
        notes.add(note);
        System.out.println("✅ Note saved!");
    }

    static void summarizeNotes() {
        if (notes.isEmpty()) {
            System.out.println("⚠️ No notes available.");
            return;
        }
        System.out.println("\n=== Summary ===");
        for (String note : notes) {
            String summary = summarize(note);
            System.out.println("- " + summary);
        }
    }

    static String summarize(String text) {
        String[] words = text.split(" ");
        if (words.length <= 10) return text;
        return String.join(" ", Arrays.copyOfRange(words, 0, 10)) + "...";
    }

    static void generateQuiz() {
        if (notes.isEmpty()) {
            System.out.println("⚠️ No notes available.");
            return;
        }
        Random rand = new Random();
        String note = notes.get(rand.nextInt(notes.size()));
        System.out.println("\n📝 Quiz question:");
        System.out.println("What is the main idea of: \"" + summarize(note) + "\"?");
    }
}

// ---- GUI Part ----
class SmartNotesGUI extends JFrame {
    JTextArea noteArea, summaryArea;
    java.util.List<String> notes;

    SmartNotesGUI(java.util.List<String> notes) {
        this.notes = notes;
        setTitle("🧠 SmartNotes - GUI Mode");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top panel
        JLabel title = new JLabel("SmartNotes GUI", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setOpaque(true);
        title.setBackground(new Color(100, 149, 237));
        title.setForeground(Color.WHITE);
        add(title, BorderLayout.NORTH);

        // Center panel
        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        noteArea = new JTextArea();
        summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        noteArea.setBorder(BorderFactory.createTitledBorder("Your Notes"));
        summaryArea.setBorder(BorderFactory.createTitledBorder("Summaries"));
        center.add(new JScrollPane(noteArea));
        center.add(new JScrollPane(summaryArea));
        add(center, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottom = new JPanel();
        JButton summarizeBtn = new JButton("Summarize");
        JButton saveBtn = new JButton("Save Note");
        JButton loadBtn = new JButton("Load Notes");
        bottom.add(summarizeBtn);
        bottom.add(saveBtn);
        bottom.add(loadBtn);
        add(bottom, BorderLayout.SOUTH);

        summarizeBtn.addActionListener(e -> summarizeGUI());
        saveBtn.addActionListener(e -> saveNote());
        loadBtn.addActionListener(e -> loadNotes());

        setVisible(true);
    }

    void summarizeGUI() {
        String text = noteArea.getText();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter some text first!");
            return;
        }
        String[] words = text.split(" ");
        String summary = text;
        if (words.length > 10) summary = String.join(" ", Arrays.copyOfRange(words, 0, 10)) + "...";
        summaryArea.setText(summary);
    }

    void saveNote() {
        String text = noteArea.getText();
        if (!text.isEmpty()) {
            notes.add(text);
            JOptionPane.showMessageDialog(this, "✅ Note saved!");
        } else {
            JOptionPane.showMessageDialog(this, "⚠️ No text to save!");
        }
    }

    void loadNotes() {
        if (notes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠️ No saved notes yet!");
            return;
        }
        noteArea.setText(String.join("\n\n---\n\n", notes));
    }
}
