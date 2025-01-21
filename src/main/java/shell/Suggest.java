package shell;

import java.util.ArrayList;
import java.util.List;


public class Suggest {

    private final Trie trie = new Trie();

    public void index(String key) {
        trie.index(key);
    }

    private List<String> suggest(String prefix) {
        return trie.suggest(prefix);
    }

    static class Trie {
        private Node root;

        void index(String key) {
            root = index(root, key.toCharArray(), key, 0);
        }

        List<String> suggest(String prefix) {
            var node = search(root, prefix.toCharArray(), 0);
            return node == null ? List.of() : node.values;
        }

        private Node search(Node node, char[] data, int index) {

            if (index == data.length || node == null) {
                return node;
            }

            if (data[index] > node.symbol) {
                return search(node.right, data, index);
            }

            if (data[index] < node.symbol) {
                return search(node.left, data, index);
            }

            if (index == data.length - 1) {
                return node;
            }

            return search(node.middle, data, index + 1);
        }

        private Node index(Node node, char[] data, String value, int index) {
            if (node == null) {
                node = new Node(data[index]);
            }

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

        static class Node {
            private final char symbol;
            private Node left, right, middle;
            private final List<String> values = new ArrayList<>();

            Node(char symbol) {
                this.symbol = symbol;
            }
        }
    }
}
