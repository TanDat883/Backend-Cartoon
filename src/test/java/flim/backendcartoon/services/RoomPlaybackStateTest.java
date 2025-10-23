package flim.backendcartoon.services;

import flim.backendcartoon.entities.RoomPlaybackState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho RoomPlaybackState
 *
 * @author Tran Tan Dat
 * @version 1.0
 */
class RoomPlaybackStateTest {

    private RoomPlaybackState state;

    @BeforeEach
    void setUp() {
        state = new RoomPlaybackState();
    }

    @Test
    void testInitialState() {
        assertEquals(0, state.getLastPositionMs());
        assertFalse(state.isPlaying());
        assertEquals(1.0, state.getPlaybackRate());
    }

    @Test
    void testPlay() {
        state.play(5000, 1.0);

        assertTrue(state.isPlaying());
        assertEquals(5000, state.getLastPositionMs());
        assertEquals(1.0, state.getPlaybackRate());
    }

    @Test
    void testPause() {
        state.play(10000, 1.0);

        // Đợi 1 giây
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long currentPos = state.getCurrentPositionMs();
        state.pause(currentPos);

        assertFalse(state.isPlaying());
        assertTrue(currentPos >= 10000); // Position đã tăng lên
    }

    @Test
    void testSeek() {
        state.play(10000, 1.0);
        state.seek(30000);

        assertEquals(30000, state.getLastPositionMs());
        assertTrue(state.isPlaying()); // Vẫn đang play
    }

    @Test
    void testPlayPauseSeek() {
        // PLAY tại 10s
        state.play(10000, 1.0);
        assertTrue(state.isPlaying());

        // Đợi 500ms
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Position nên tăng lên khoảng 500ms
        long pos1 = state.getCurrentPositionMs();
        assertTrue(pos1 >= 10000 && pos1 <= 11000);

        // PAUSE
        state.pause(pos1);
        assertFalse(state.isPlaying());

        // Đợi thêm 500ms
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Position không đổi vì đã pause
        long pos2 = state.getCurrentPositionMs();
        assertEquals(pos1, pos2);

        // SEEK đến 90s
        state.seek(90000);
        assertEquals(90000, state.getCurrentPositionMs());
        assertFalse(state.isPlaying()); // Vẫn pause
    }

    @Test
    void testPlaybackRateChange() {
        state.play(0, 2.0); // Play với tốc độ 2x

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long currentPos = state.getCurrentPositionMs();
        // Với playbackRate = 2.0, sau 1s thực tế = 2s video
        assertTrue(currentPos >= 1800 && currentPos <= 2200);
    }

    @Test
    void testGetCurrentPositionWhenPaused() {
        state.pause(15000);

        long pos1 = state.getCurrentPositionMs();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long pos2 = state.getCurrentPositionMs();

        // Position không đổi khi pause
        assertEquals(pos1, pos2);
        assertEquals(15000, pos2);
    }
}

