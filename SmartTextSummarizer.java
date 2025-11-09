import java.util.*;
import java.util.stream.Collectors;

public class SmartTextSummarizer {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("📝 Enter your text below (end with an empty line):");
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = sc.nextLine();
            if (line.trim().isEmpty()) break;
            sb.append(line).append(" ");
        }

        String text = sb.toString().trim();

        System.out.print("\n💬 How many sentences do you want in the summary? ");
        int numSentences = sc.nextInt();

        String summary = generateFluentAbstractiveSummary(text, numSentences);

        System.out.println("\n===== ✨ HUMAN-LIKE FLUENT SUMMARY ✨ =====\n");
        System.out.println(summary);
    }

    // =====================================================
    // 🧠 Human-like Abstractive Summarization with Flow
    // =====================================================
    public static String generateFluentAbstractiveSummary(String text, int numSentences) {
        // Split text into sentences
        String[] sentences = text.split("(?<=[.!?])\\s+");
        if (sentences.length <= numSentences) return text;

        // Build frequency map of words
        Map<String, Integer> freq = new HashMap<>();
        for (String sentence : sentences) {
            for (String word : sentence.toLowerCase().replaceAll("[^a-z ]", "").split("\\s+")) {
                if (word.isEmpty() || word.length() <= 2) continue;
                freq.put(word, freq.getOrDefault(word, 0) + 1);
            }
        }

        // Identify important keywords
        List<String> keywords = freq.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(20)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Step 1: Create compressed sentences using keywords
        List<String> compressed = new ArrayList<>();
        for (String sentence : sentences) {
            StringBuilder shortSentence = new StringBuilder();
            for (String word : sentence.split("\\s+")) {
                String clean = word.toLowerCase().replaceAll("[^a-z]", "");
                if (keywords.contains(clean) || word.length() > 6) {
                    shortSentence.append(word).append(" ");
                }
            }
            String result = shortSentence.toString().trim();
            if (!result.isEmpty()) compressed.add(capitalize(result));
        }

        // Step 2: Merge them into a readable flow
        String merged = mergeSentencesSmoothly(compressed);

        // Step 3: Limit to the requested number of sentences
        String[] split = merged.split("(?<=[.!?])\\s+");
        if (split.length <= numSentences) return merged;

        String summary = Arrays.stream(split)
                .limit(numSentences)
                .collect(Collectors.joining(" "));

        // Step 4: Final polishing for human flow
        summary = polishLanguage(summary);

        return summary.trim();
    }

    // =====================================================
    // 🧩 Helper Methods
    // =====================================================

    // Smooth merging logic with connectors
    private static String mergeSentencesSmoothly(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        String[] connectors = {"Moreover", "Additionally", "Furthermore", "Also", "In addition", "As a result"};

        for (int i = 0; i < lines.size(); i++) {
            String sentence = lines.get(i);
            if (i == 0) {
                sb.append(sentence).append(". ");
            } else {
                String connector = connectors[i % connectors.length];
                sb.append(connector).append(", ").append(sentence.toLowerCase()).append(". ");
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    // Capitalize helper
    private static String capitalize(String text) {
        if (text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    // Light polishing for grammar flow
    private static String polishLanguage(String text) {
        text = text.replaceAll(" ,", ",");
        text = text.replaceAll(" +", " ");
        text = text.replaceAll("\\s+\\.", ".");
        text = text.replaceAll("for example", "for instance");
        text = text.replaceAll("also,", "Additionally,");
        text = text.replaceAll("Moreover, Moreover,", "Moreover,");
        return capitalize(text);
    }
}
