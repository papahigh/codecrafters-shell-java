package shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


public class Suggest {

    private final Trie trie = new Trie();

    public void index(String key) {
        trie.index(key);
    }

    public Result suggest(String prefix) {
        return trie.suggest(prefix);
    }

    /**
     * A record that encapsulates the result of a suggestion operation.
     * It consists of the longest common prefix derived from a trie structure
     * and a list of suggestion options.
     */
    public record Result(String longestCommonPrefix, List<String> suggestOptions) {

        public String firstOption() {
            return suggestOptions.getFirst();
        }

        public int count() {
            return suggestOptions.size();
        }

        static Result of(String longestCommonPrefix, List<String> options) {
            return new Result(longestCommonPrefix, options);
        }

        static Result notFound() {
            return new Result("", List.of());
        }
    }

    private static class Trie {
        private Node root;

        void index(String key) {
            root = index(root, key.toCharArray(), key, 0);
        }

        Result suggest(String prefix) {
            var node = search(root, prefix.toCharArray(), 0);
            if (node == null) return Result.notFound();

            var sb = new StringBuilder(prefix);
            var curr = node.middle;
            while (curr != null && curr.isCommonPrefix()) {
                sb.append(curr.symbol);
                curr = curr.middle;
            }
            return Result.of(sb.toString(), new ArrayList<>(node.values));
        }

        private Node index(Node node, char[] data, String value, int index) {
            if (node == null) node = new Node(data[index]);

            if (data[index] > node.symbol) {
                node.right = index(node.right, data, value, index);
            } else if (data[index] < node.symbol) {
                node.left = index(node.left, data, value, index);
            } else {
                if (index < data.length - 1)
                    node.middle = index(node.middle, data, value, index + 1);
                node.values.add(value);
            }

            return node;
        }

        private Node search(Node node, char[] data, int index) {
            if (index == data.length || node == null) return node;

            if (data[index] > node.symbol) return search(node.right, data, index);
            if (data[index] < node.symbol) return search(node.left, data, index);
            if (index == data.length - 1) return node;

            return search(node.middle, data, index + 1);
        }

        private static class Node {
            final Set<String> values = new TreeSet<>();
            final char symbol;

            Node left, right, middle;

            Node(char symbol) {
                this.symbol = symbol;
            }

            boolean isCommonPrefix() {
                return left == null && right == null;
            }
        }
    }
}
