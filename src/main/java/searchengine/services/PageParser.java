package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.dto.statistics.Pairs;

import java.io.IOException;
import java.util.*;

public class PageParser {
    private final LuceneMorphology luceneMorphology;
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final int wordArea = 4;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    public PageParser() throws IOException {
        luceneMorphology = new RussianLuceneMorphology();
    }

    public String buildSnippet(String[] lemmas, String text){
        ArrayList<Pairs> lemmasPositions = new ArrayList<>();
        StringBuilder snippet = new StringBuilder();
        String[] words = obtainWords(text);
        for (int i = 0; i < lemmas.length; i++){
            for (int j = 0; j < words.length; j++){
                for (String word : luceneMorphology.getNormalForms(words[j])){
                    if (lemmas[i].equals(word)) {
                        lemmasPositions.add(new Pairs(i, j));
                        j = words.length;
                    }
                }
            }
        }
        lemmasPositions.sort(Comparator.comparing(Pairs::getY));
        if (lemmasPositions.size() == 0){
            return "";
        }

        for (int j = Math.max(lemmasPositions.get(0).getY() - wordArea, 0); j < lemmasPositions.get(0).getY(); j++) {
            snippet.append(words[j]).append(" ");
        }

        for (int i = 0; i < lemmasPositions.size() - 1; i++){
            for (int j = Math.max(lemmasPositions.get(i).getY(), 0);
                 j < Math.min(lemmasPositions.get(i).getY() + wordArea + 1, lemmasPositions.get(i + 1).getY() - wordArea); j++){
                if (j == lemmasPositions.get(i).getY()){
                    snippet.append("<b>").append(words[j]).append("</b>").append(" ");
                } else {
                    snippet.append(words[j]).append(" ");
                }
            }
            for (int j = Math.max(lemmasPositions.get(i + 1).getY() - wordArea, 0);
                 j < lemmasPositions.get(i + 1).getY(); j++){
                if (j == lemmasPositions.get(i).getY()){
                    snippet.append("<b>").append(words[j]).append("</b>").append(" ");
                } else {
                    snippet.append(words[j]).append(" ");
                }
            }
        }
        for (int j = Math.max(lemmasPositions.get(lemmasPositions.size() - 1).getY(), 0);
             j < Math.min(lemmasPositions.get(lemmasPositions.size() - 1).getY() + wordArea + 1, words.length); j++){
            if (j == lemmasPositions.get(lemmasPositions.size() - 1).getY()){
                snippet.append("<b>").append(words[j]).append("</b>").append(" ");
            } else {
                snippet.append(words[j]).append(" ");
            }
        }
        return snippet.toString();
    }

    private String[] obtainWords(String text){
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    public Map<String, Integer> collectLemmas(String text){
        String[] words = obtainWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            for (String normalWord : normalForms){
                if (lemmas.containsKey(normalWord)) {
                    lemmas.put(normalWord, lemmas.get(normalWord) + 1);
                } else {
                    lemmas.put(normalWord, 1);
                }
            }
        }

        return lemmas;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

}
