package com.yzh.note.parallel;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * 任务节点解析器
 *
 * @author yzh
 */
public class TaskExpressionParser {
    private int pos = 0;

    private Character c;
    private final String s;

    public TaskExpressionParser(String input) {
        this.s = StrUtil.removeAll(input, " ");
        this.c = s != null ? s.charAt(0) : null;
    }

    public Node parse() {
        Node node = new Node();
        List<Node> children = new ArrayList<>();

        while (c != null) {
            if (c == '(') {
                advance();
                children.add(parse());
            } else if (c == '>') {
                node.parallel = false;
                advance();
            } else if (c == '|') {
                node.parallel = true;
                advance();
            } else if (NumberUtil.isNumber(String.valueOf(c))) {
                children.add(atom());
            } else if (c == ')') {
                advance();
                break;
            }
        }
        node.children = children;
        return node;
    }

    public List<Integer> allNumbers() {
        List<Integer> numbers = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\d+");  // 匹配连续数字
        Matcher matcher = pattern.matcher(s);
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        return numbers;
    }

    private void advance() {
        pos += 1;
        if (pos < s.length()) {
            c = s.charAt(pos);
        } else {
            c = null;
        }
    }

    /**
     * 任务节点, 只有叶子节点需要执行具体的任务
     */
    @Getter
    public static class Node {
        /**
         * 任务标识
         */
        private String value;
        /**
         * children是否并行
         */
        private Boolean parallel;

        private List<Node> children;

        public Node() {
        }

        public Node(Boolean parallel, List<Node> children) {
            this.parallel = parallel;
            this.children = children;
        }

        public Node(String value) {
            this.value = value;
        }
    }

    private Node atom() {
        StringBuilder r = new StringBuilder();
        while (c != null && NumberUtil.isNumber(String.valueOf(c))) {
            r.append(c);
            advance();
        }

        return new Node(r.toString());
    }

    public static void main(String[] args) {
        TaskExpressionParser taskExpressionParser = new TaskExpressionParser("6|7|8|(11>(12|(15>(16|17)))>(13|14))|9|(1>(2|3|4))");
        Node parse = taskExpressionParser.parse();

        System.out.println(parse);
    }

}
