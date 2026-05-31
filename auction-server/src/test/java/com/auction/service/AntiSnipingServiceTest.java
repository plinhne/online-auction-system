package com.auction.service;

import com.auction.model.auction.Auction;
import com.auction.server.dao.AuctionDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AntiSnipingServiceTest {

    private AuctionDAO auctionDAO;
    private AntiSnipingService service;

    @BeforeEach
    void setUp() {
        auctionDAO = mock(AuctionDAO.class);
        service = new AntiSnipingService(auctionDAO);
    }

    /**
     * Case 1: Auction còn rất lâu mới kết thúc → KHÔNG extend
     */
    @Test
    void test_no_extend_when_far_from_end_time() throws SQLException {

        Auction auction = mock(Auction.class);

        LocalDateTime endTime = LocalDateTime.now().plusMinutes(10);

        when(auction.getId()).thenReturn(1);
        when(auction.getEndTime()).thenReturn(endTime);

        boolean result = service.handle(auction);

        assertFalse(result);

        verify(auctionDAO, never()).updateEndTime(anyInt(), any());
    }

    /**
     * Case 2: Trong 20 giây cuối → PHẢI extend
     */
    @Test
    void test_extend_when_in_sniping_window() throws SQLException {

        Auction auction = mock(Auction.class);

        LocalDateTime endTime = LocalDateTime.now().plusSeconds(10);

        when(auction.getId()).thenReturn(1);
        when(auction.getEndTime()).thenReturn(endTime);

        boolean result = service.handle(auction);

        assertTrue(result);

        verify(auction).setEndTime(endTime.plusSeconds(40));
        verify(auctionDAO).updateEndTime(1, endTime.plusSeconds(40));
    }

    /**
     * Case 3: Auction đã kết thúc → KHÔNG extend
     */
    @Test
    void test_no_extend_when_auction_ended() throws SQLException {

        Auction auction = mock(Auction.class);

        when(auction.getId()).thenReturn(1);
        when(auction.getEndTime())
                .thenReturn(LocalDateTime.now().minusSeconds(1));

        boolean result = service.handle(auction);

        assertFalse(result);

        verify(auctionDAO, never()).updateEndTime(anyInt(), any());
    }

    /**
     * Case 4: Đúng thời điểm endTime → KHÔNG extend
     */
    @Test
    void test_no_extend_when_exact_end_time() throws SQLException {

        Auction auction = mock(Auction.class);

        LocalDateTime endTime = LocalDateTime.now();

        when(auction.getId()).thenReturn(1);
        when(auction.getEndTime()).thenReturn(endTime);

        boolean result = service.handle(auction);

        assertFalse(result);

        verify(auctionDAO, never()).updateEndTime(anyInt(), any());
    }

    /**
     * Case 5: đảm bảo DAO không bị gọi sai lần nào
     */
    @Test
    void test_dao_never_called_when_no_extend() throws SQLException {

        Auction auction = mock(Auction.class);

        when(auction.getId()).thenReturn(1);
        when(auction.getEndTime())
                .thenReturn(LocalDateTime.now().plusHours(1));

        service.handle(auction);

        verifyNoInteractions(auctionDAO);
    }
}