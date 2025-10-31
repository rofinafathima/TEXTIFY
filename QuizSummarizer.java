import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

class Question {
    String questionText;
    String answer;

    Question(String questionText, String answer) {
        this.questionText = questionText;
        this.answer = answer;
    }
}

public class QuizSummarizer {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Input text
        System.out.println("Enter your text (type END to finish):");
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = sc.nextLine();
            if (line.equalsIgnoreCase("END")) break;
            sb.append(line).append(" ");
        }

        // Summarize
        String summary = summarize(sb.toString(), 3);
        System.out.println("\n--- Summary ---");
        System.out.println(summary);

        // Generate quiz
        ArrayList<Question> quiz = generateQuiz(summary);

        // Conduct quiz
        int score = 0;
        System.out.println("\n=== Quiz Time ===");
        for (int i = 0; i < quiz.size(); i++) {
            Question q = quiz.get(i);
            System.out.println("Q" + (i + 1) + ": " + q.questionText);
            String userAns = sc.nextLine().trim();
            if (userAns.equalsIgnoreCase(q.answer)) {
                System.out.println("✅ Correct!\n");
                score++;
            } else {
                System.out.println("❌ Wrong. Correct Answer: " + q.answer + "\n");
            }
        }

        System.out.println("🎯 Your Score: " + score + "/" + quiz.size());
    }

    // Simple summarizer
    public static String summarize(String text, int numSentences) {
        if (text.isBlank()) return "No text provided.";
        String[] sentencesArr = text.split("(?<=[.!?])\\s+");
        ArrayList<String> sentences = new ArrayList<>(Arrays.asList(sentencesArr));

        HashMap<String, Integer> wordFreq = new HashMap<>();
        for (String word : text.toLowerCase().split("\\W+")) {
            if (word.isBlank()) continue;
            wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
        }

        HashMap<String, Integer> sentenceScores = new HashMap<>();
        for (String sentence : sentences) {
            int score = 0;
            for (String word : sentence.toLowerCase().split("\\W+")) {
                score += wordFreq.getOrDefault(word, 0);
            }
            sentenceScores.put(sentence, score);
        }

        PriorityQueue<Map.Entry<String, Integer>> pq =
                new PriorityQueue<>((a, b) -> b.getValue() - a.getValue());
        pq.addAll(sentenceScores.entrySet());

        ArrayList<String> summarySentences = new ArrayList<>();
        for (int i = 0; i < numSentences && !pq.isEmpty(); i++) {
            summarySentences.add(pq.poll().getKey());
        }

        return sentences.stream()
                .filter(summarySentences::contains)
                .collect(Collectors.joining(" "));
    }

    // Generate quiz questions from summary
    public static ArrayList<Question> generateQuiz(String summary) {
        ArrayList<Question> quiz = new ArrayList<>();
        String[] sentences = summary.split("(?<=[.!?])\\s+");

        for (String s : sentences) {
            String[] words = s.split("\\s+");
            if (words.length > 3) {
                // Remove a keyword (2nd or 3rd word) → fill in the blank
                String answer = words[2].replaceAll("[^a-zA-Z]", "");
                String qText = s.replaceFirst(words[2], "____");
                quiz.add(new Question(qText, answer));
            }
            if (quiz.size() >= 5) break; // limit to 5 questions
        }
        return quiz;
    }
}
