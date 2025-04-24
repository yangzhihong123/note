package com.yzh.note.parallel;

import cn.hutool.core.thread.ThreadUtil;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.yzh.note.parallel.TaskExpressionParser.Node;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author : zhihong.yang
 */
public class ParallelExecutor<E> {
    private final static int POOL_SIZE = (int)(Runtime.getRuntime().availableProcessors() / (1 - 0.8f));
    private final static ThreadPoolExecutor EXECUTOR =
        new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            ThreadUtil.newNamedThreadFactory("parallel-", false), new ThreadPoolExecutor.CallerRunsPolicy());

    private final static ListeningExecutorService LISTENABLE_EXECUTOR = MoreExecutors.listeningDecorator(EXECUTOR);

    private final List<ListenableFuture<E>> list = Collections.synchronizedList(new ArrayList<>());
    private List<E> result;

    public static <T, R> R parallelDo(Collection<T> list, Function<T, R> runnable, BinaryOperator<R> reduce) {
        if (list != null && !list.isEmpty()) {

            List<ListenableFuture<R>> futures =
                list.stream().map(e -> LISTENABLE_EXECUTOR.submit(() -> runnable.apply(e)))
                    .collect(Collectors.toList());

            return futures.stream().map(ParallelExecutor::resultUnchecked).reduce(reduce).orElse(null);
        }

        return null;
    }

    private static <R> R resultUnchecked(ListenableFuture<R> f) {
        return Futures.getUnchecked(f);
    }

    public static <T> void parallelDo(Collection<T> list, Consumer<T> runnable) {
        if (list != null && !list.isEmpty()) {
            list.forEach(e -> LISTENABLE_EXECUTOR.submit(() -> runnable.accept(e)));
        }
    }

    public static <R> ParallelExecutor<R> instance() {
        return new ParallelExecutor<>();
    }

    public ParallelExecutor<E> submit(Callable<E> callable) {
        ListenableFuture<E> submit = LISTENABLE_EXECUTOR.submit(callable);
        list.add(submit);

        return this;
    }

    public List<E> get() {
        if (result != null) {
            return result;
        }

        if (!list.isEmpty()) {
            result = resultUnchecked(Futures.allAsList(list));
        }

        return result;
    }

    public static Void dfs(Node root, ParallelExecutor<Void> instance, Function<Node, Void> childDo) {
        List<Node> children = root.getChildren();
        if (children != null) {
            if (root.getParallel() != null && root.getParallel()) {
                for (Node child : children) {
                    dfs(child, instance, childDo);
                }
            } else {
                instance.submit(() -> {
                    for (Node child : children) {
                        ParallelExecutor<Void> instance1 = ParallelExecutor.instance();
                        dfs(child, instance1, childDo);
                        instance1.get();
                    }
                    return null;
                });
            }

        } else {
            // 叶子节点
            instance.submit(() -> childDo.apply(root));
        }

        return null;
    }

    public static void main(String[] args) {
        TaskExpressionParser parser = new TaskExpressionParser("6|7|8|(11>(12|(15>(16|17)))>(13|14))|9|(1>(2|3|4))");
        Node root = parser.parse();

        ParallelExecutor<Void> instance = ParallelExecutor.instance();

        ParallelExecutor.dfs(root, instance, (node) -> {
            System.out.println("node : " + node.getValue());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("-----end node : " + node.getValue());
            return null;
        });
    }

}
