package com.dispatch.java.dsa.tree;



class Node {
    int data;
    Node left;
    Node right;

    public Node(int data) {
        this.data = data;
        this.left = null;
        this.right = null;
    }

    public Node(int data, Node left, Node right) {
        this.data = data;
        this.left = left;
        this.right = right;
    }

    public int getData() {
        return data;
    }

    public void setData(int data) {
        this.data = data;
    }

    public Node getLeft() {
        return left;
    }

    public void setLeft(Node left) {
        this.left = left;
    }

    public Node getRight() {
        return right;
    }

    public void setRight(Node right) {
        this.right = right;
    }
}

public class BinaryTree {

    private Node root;

    public BinaryTree() {
        this.root = null;
    }

    public BinaryTree(Node node) {
        this.root = node;
    }

    public Node getRoot() {
        return root;
    }

    public void setRoot(Node root) {
        this.root = root;
    }

    public void printInOrder(Node node) {
        if (node == null) return;
        printInOrder(node.getLeft());
        System.out.print(node.data + " ");
        printInOrder(node.getRight());
    }

    public void printPreOrder(Node node) {
        if (node == null) return;
        System.out.print(node.data + " ");
        printPreOrder(node.getLeft());
        printPreOrder(node.getRight());
    }

    public void printPostOrder(Node node) {
        if (node == null) return;

        printPostOrder(node.getLeft());
        printPostOrder(node.getRight());
        System.out.print(node.getData() + " ");
    }

    public static void main(String[] args) {
        BinaryTree tree = new BinaryTree(new Node(1));

        /* Constructing the tree using setters:
                 1
               /   \
              2     3
             / \
            4   5
        */


        // Create and link left and right children of root
        tree.getRoot().setLeft(new Node(2));
        tree.getRoot().setRight(new Node(3));

        // Link children to node 2
        tree.getRoot().getLeft().setLeft(new Node(4));
        tree.getRoot().getLeft().setRight(new Node(5));

        // Execute and print traversals
        System.out.print("Pre-order traversal:  ");
        tree.printPreOrder(tree.getRoot());

        System.out.print("\nIn-order traversal:   ");
        tree.printInOrder(tree.getRoot());

        System.out.print("\nPost-order traversal: ");
        tree.printPostOrder(tree.getRoot());
    }
}

