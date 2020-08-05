package org.hyperskill.blockchain;

import org.hyperskill.blockchain.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {

    private static final int BLOCK_COUNT = 15;
    private static final int MINERS_COUNT = 4;
    private static final int GENERATORS_COUNT = 3;
    public static final int MAX_THREADS = 4;
    private static final int TRANS_PER_BLOCK = 5;

    public static void main(String[] args) {
        //processWithThreads();
        processWithExecutors();
    }

    private static void processWithExecutors() {
        Blockchain blockchain = new Blockchain();
        final int nThreads = Math.min(MAX_THREADS, Runtime.getRuntime().availableProcessors());
        ExecutorService threadPoolBack = Executors.newCachedThreadPool();
        ExecutorService threadPoolMain = Executors.newFixedThreadPool(nThreads);

        List<Miner> miners = IntStream.range(0, MINERS_COUNT).mapToObj(i -> new Miner(String.format("Miner#%03d", i), blockchain))
                .collect(Collectors.toList());
        List<CryptoCurrencyHolder> cryptoCurrencyHolders = Stream.concat(miners.stream(),
                Stream.of("Anna", "Bell", "Jack").map(name -> new CryptoCurrencyHolderImpl(name, blockchain)))
                .collect(Collectors.toList());

        for (int i = 0; i < GENERATORS_COUNT; i++) {
            TransactionGenerator transactionGenerator = new TransactionGenerator(blockchain, cryptoCurrencyHolders);
            threadPoolBack.submit(() -> {
                transactionGenerator.generate()
                        .limit(BLOCK_COUNT * TRANS_PER_BLOCK)
                        .forEach(blockchain::addTransaction);
            });
        }
        for (int i = 0; i < MINERS_COUNT; i++) {
            Miner miner = miners.get(i);
            long k = BLOCK_COUNT / MINERS_COUNT;
            long blocksToMine = i != MINERS_COUNT - 1 ? k : BLOCK_COUNT - ((MINERS_COUNT - 1) * k);
            threadPoolMain.submit(() -> {
                try {
                    miner.mine(blocksToMine);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
            });
        }

        threadPoolBack.shutdown();
        threadPoolMain.shutdown();
        try {
            threadPoolMain.awaitTermination(1, TimeUnit.DAYS);
            threadPoolBack.awaitTermination(1, TimeUnit.DAYS);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Logger.getDebugLogger().println("main() finished");
    }

    private static void processWithThreads() {
        Blockchain blockchain = new Blockchain();

        List<Miner> miners = IntStream.range(0, MINERS_COUNT).mapToObj(i -> new Miner(String.format("Miner#%03d", i), blockchain))
                .collect(Collectors.toList());
        List<CryptoCurrencyHolder> cryptoCurrencyHolders = Stream.concat(miners.stream(),
                Stream.of("Anna", "Bell", "Jack").map(name -> new CryptoCurrencyHolderImpl(name, blockchain)))
                .collect(Collectors.toList());

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < GENERATORS_COUNT; i++) {
            TransactionGenerator transactionGenerator = new TransactionGenerator(blockchain, cryptoCurrencyHolders);

            Thread thread = new Thread(() -> {
                transactionGenerator.generate()
                        .limit(BLOCK_COUNT * TRANS_PER_BLOCK)
                        .forEach(blockchain::addTransaction);
            });
            threads.add(thread);
            thread.start();
        }
        for (int i = 0; i < MINERS_COUNT; i++) {
            Miner miner = miners.get(i);
            long k = BLOCK_COUNT / MINERS_COUNT;
            long blocksToMine = i != MINERS_COUNT - 1 ? k : BLOCK_COUNT - ((MINERS_COUNT - 1) * k);
            Thread thread = new Thread(() -> {
                try {
                    miner.mine(blocksToMine);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
            });
            threads.add(thread);
            thread.start();
        }


        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String result = blockchain.toString();
        System.out.println(result);

        Logger.getDebugLogger().println("main() finished");
    }

}
