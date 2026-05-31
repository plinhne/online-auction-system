package com.auction;

import com.auction.model.item.*;
import com.auction.model.pattern.factory.ItemFactory;
import com.auction.model.pattern.observer.AuctionSubject;
import com.auction.model.pattern.observer.BidObserver;
import com.auction.model.pattern.singleton.AuctionManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DesignPattenTest {

    // ─────────────────────────────────────
    // 1. FACTORY PATTERN TEST
    // ─────────────────────────────────────

    @Test
    void test_factory_create_electronics() {
        Item item = ItemFactory.createItem(ItemCategory.ELECTRONICS, 1, "Laptop", 1000);

        assertTrue(item instanceof Electronics);
        assertEquals("Laptop", item.getName());
    }

    @Test
    void test_factory_create_art() {
        Item item = ItemFactory.createItem(ItemCategory.ART, 2, "Painting", 500);

        assertTrue(item instanceof Art);
    }

    @Test
    void test_factory_create_vehicle() {
        Item item = ItemFactory.createItem(ItemCategory.VEHICLE, 3, "Car", 10000);

        assertTrue(item instanceof Vehicle);
    }

    @Test
    void test_factory_invalid() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem(null, 1, "Test", 100));
    }

    // ─────────────────────────────────────
    // 2. OBSERVER PATTERN TEST
    // ─────────────────────────────────────

    static class TestObserver implements BidObserver {
        List<String> logs = new ArrayList<>();

        @Override
        public void updateNewBid(int auctionId, double newAmount, int bidderId) {
            logs.add(auctionId + "-" + newAmount + "-" + bidderId);
        }
    }

    static class TestSubject implements AuctionSubject {

        List<BidObserver> observers = new ArrayList<>();

        @Override
        public void addObserver(BidObserver observer) {
            observers.add(observer);
        }

        @Override
        public void removeObserver(BidObserver observer) {
            observers.remove(observer);
        }

        @Override
        public void notifyObservers(double newAmount, int bidderId) {
            for (BidObserver o : observers) {
                o.updateNewBid(1, newAmount, bidderId);
            }
        }
    }

    @Test
    void test_observer_notify() {

        TestSubject subject = new TestSubject();
        TestObserver observer = new TestObserver();

        subject.addObserver(observer);

        subject.notifyObservers(100.0, 10);

        assertEquals(1, observer.logs.size());
        assertEquals("1-100.0-10", observer.logs.get(0));
    }

    @Test
    void test_observer_remove() {

        TestSubject subject = new TestSubject();
        TestObserver observer = new TestObserver();

        subject.addObserver(observer);
        subject.removeObserver(observer);

        subject.notifyObservers(200.0, 20);

        assertTrue(observer.logs.isEmpty());
    }

    // ─────────────────────────────────────
    // 3. SINGLETON PATTERN TEST
    // ─────────────────────────────────────

    @Test
    void test_singleton_same_instance() {

        AuctionManager m1 = AuctionManager.getInstance();
        AuctionManager m2 = AuctionManager.getInstance();

        assertSame(m1, m2);
    }

    @Test
    void test_singleton_add_get_auction() {

        AuctionManager manager = AuctionManager.getInstance();

        Auction auction = mock(Auction.class);
        when(auction.getId()).thenReturn(1);

        manager.addAuction(auction);

        Auction result = manager.getAuction(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void test_singleton_thread_safe() throws Exception {

        Callable<AuctionManager> task = AuctionManager::getInstance;

        ExecutorService executor = Executors.newFixedThreadPool(10);

        List<Future<AuctionManager>> results =
                executor.invokeAll(java.util.Collections.nCopies(10, task));

        AuctionManager first = results.get(0).get();

        for (Future<AuctionManager> f : results) {
            assertSame(first, f.get());
        }

        executor.shutdown();
    }
}