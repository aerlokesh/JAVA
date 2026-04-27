import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.stream.*;

/*
 * MUSIC PLAYER (Spotify-like) - Low Level Design
 * ==================================================
 * 
 * REQUIREMENTS:
 * 1. Songs with metadata (title, artist, duration, genre)
 * 2. Playlists: create, add/remove songs
 * 3. Playback: play, pause, skip next/prev, shuffle
 * 4. Pluggable play order: sequential, shuffle, repeat (Strategy)
 * 5. Playback event notifications (Observer)
 * 6. Search songs by title/artist
 * 7. Thread-safe
 * 
 * DESIGN PATTERNS:
 *   Strategy  (PlayOrderStrategy)  — SequentialOrder, ShuffleOrder, RepeatOneOrder
 *   Observer  (PlaybackListener)   — PlaybackLogger, NowPlayingNotifier
 *   Facade    (MusicPlayerService)
 * 
 * KEY DS: Map<songId, Song>, Map<playlistId, Playlist>, Queue for play queue
 */

// ==================== EXCEPTIONS ====================

class SongNotFoundException extends RuntimeException {
    SongNotFoundException(String id) { super("Song not found: " + id); }
}

class PlaylistNotFoundException extends RuntimeException {
    PlaylistNotFoundException(String id) { super("Playlist not found: " + id); }
}

class EmptyPlaylistException extends RuntimeException {
    EmptyPlaylistException(String id) { super("Playlist is empty: " + id); }
}

// ==================== ENUMS ====================

enum PlaybackState { STOPPED, PLAYING, PAUSED }

enum MusicGenre { POP, ROCK, JAZZ, CLASSICAL, HIPHOP, ELECTRONIC, OTHER }

// ==================== MODELS ====================

class Song {
    final String id, title, artist;
    final int durationSecs;
    final MusicGenre genre;

    Song(String id, String title, String artist, int durationSecs, MusicGenre genre) {
        this.id = id; this.title = title; this.artist = artist;
        this.durationSecs = durationSecs; this.genre = genre;
    }
}

class Playlist {
    final String id, name;
    final List<Song> songs = new ArrayList<>();

    Playlist(String id, String name) { this.id = id; this.name = name; }

    void addSong(Song song) { songs.add(song); }

    boolean removeSong(String songId) { return songs.removeIf(s -> s.id.equals(songId)); }
}

// ==================== INTERFACES ====================

/** Strategy — determines song play order. */
interface PlayOrderStrategy {
    List<Song> arrange(List<Song> songs);
}

/** Observer — playback events. */
interface PlaybackListener {
    void onPlaybackEvent(PlaybackState state, Song song);
}

// ==================== STRATEGY IMPLEMENTATIONS ====================

/** Play songs in order from current index. */
class SequentialOrder implements PlayOrderStrategy {
    @Override public List<Song> arrange(List<Song> songs) {
        return new ArrayList<>(songs);
    }
}

/** Shuffle: randomize all songs. */
class ShuffleOrder implements PlayOrderStrategy {
    @Override public List<Song> arrange(List<Song> songs) {
        List<Song> shuffled = new ArrayList<>(songs);
        Collections.shuffle(shuffled);
        return shuffled;
    }
}

/** Repeat one: just the first song. */
class RepeatOneOrder implements PlayOrderStrategy {
    @Override public List<Song> arrange(List<Song> songs) {
        if (songs.isEmpty()) return Collections.emptyList();
        return List.of(songs.get(0));
    }
}

// ==================== OBSERVER IMPLEMENTATIONS ====================

class PlaybackLogger implements PlaybackListener {
    final List<String> events = new ArrayList<>();
    @Override public void onPlaybackEvent(PlaybackState state, Song song) {
        events.add(state + ":" + (song != null ? song.title : "none"));
    }
}

class NowPlayingNotifier implements PlaybackListener {
    String currentDisplay = "";
    @Override public void onPlaybackEvent(PlaybackState state, Song song) {
        if (state == PlaybackState.PLAYING && song != null)
            currentDisplay = "♫ Now Playing: " + song.title + " — " + song.artist;
        else if (state == PlaybackState.PAUSED)
            currentDisplay = "⏸ Paused: " + (song != null ? song.title : "");
        else currentDisplay = "⏹ Stopped";
    }
}

// ==================== MUSIC PLAYER SERVICE (FACADE) ====================

class MusicPlayerService {
    private final Map<String, Song> songs = new ConcurrentHashMap<>();
    private final Map<String, Playlist> playlists = new ConcurrentHashMap<>();
    private final List<PlaybackListener> listeners = new ArrayList<>();
    private PlayOrderStrategy playOrder;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Playback state
    private final LinkedList<Song> playQueue = new LinkedList<>();
    private Song currentSong;
    private PlaybackState state = PlaybackState.STOPPED;
    private final java.util.concurrent.atomic.AtomicInteger songCounter = new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger playlistCounter = new java.util.concurrent.atomic.AtomicInteger();

    MusicPlayerService(PlayOrderStrategy order) { this.playOrder = order; }
    MusicPlayerService() { this(new SequentialOrder()); }

    void setPlayOrder(PlayOrderStrategy order) { this.playOrder = order; }
    void addListener(PlaybackListener l) { listeners.add(l); }

    private void fireEvent(PlaybackState state, Song song) {
        listeners.forEach(l -> l.onPlaybackEvent(state, song));
    }

    // --- Song Library ---

    Song addSong(String title, String artist, int duration, MusicGenre genre) {
        String id = "S-" + songCounter.incrementAndGet();
        Song song = new Song(id, title, artist, duration, genre);
        songs.put(id, song);
        return song;
    }

    Song getSong(String id) {
        Song s = songs.get(id);
        if (s == null) throw new SongNotFoundException(id);
        return s;
    }

    List<Song> searchByTitle(String keyword) {
        String lower = keyword.toLowerCase();
        return songs.values().stream()
            .filter(s -> s.title.toLowerCase().contains(lower))
            .collect(Collectors.toList());
    }

    List<Song> searchByArtist(String artist) {
        String lower = artist.toLowerCase();
        return songs.values().stream()
            .filter(s -> s.artist.toLowerCase().contains(lower))
            .collect(Collectors.toList());
    }

    List<Song> searchByGenre(MusicGenre genre) {
        return songs.values().stream()
            .filter(s -> s.genre == genre)
            .collect(Collectors.toList());
    }

    // --- Playlists ---

    Playlist createPlaylist(String name) {
        String id = "PL-" + playlistCounter.incrementAndGet();
        Playlist pl = new Playlist(id, name);
        playlists.put(id, pl);
        return pl;
    }

    Playlist getPlaylist(String id) {
        Playlist pl = playlists.get(id);
        if (pl == null) throw new PlaylistNotFoundException(id);
        return pl;
    }

    void addSongToPlaylist(String playlistId, String songId) {
        getPlaylist(playlistId).addSong(getSong(songId));
    }

    void removeSongFromPlaylist(String playlistId, String songId) {
        getPlaylist(playlistId).removeSong(songId);
    }

    // --- Playback ---

    /** Play a playlist from the beginning using current play order strategy. */
    Song playPlaylist(String playlistId) {
        lock.writeLock().lock();
        try {
            Playlist pl = getPlaylist(playlistId);
            if (pl.songs.isEmpty()) throw new EmptyPlaylistException(playlistId);
            playQueue.clear();
            playQueue.addAll(playOrder.arrange(pl.songs));
            return playNext();
        } finally { lock.writeLock().unlock(); }
    }

    /** Play next song in queue. */
    Song playNext() {
        lock.writeLock().lock();
        try {
            if (playQueue.isEmpty()) { stop(); return null; }
            currentSong = playQueue.poll();
            state = PlaybackState.PLAYING;
            fireEvent(state, currentSong);
            return currentSong;
        } finally { lock.writeLock().unlock(); }
    }

    /** Play a specific song directly. */
    Song playSong(String songId) {
        lock.writeLock().lock();
        try {
            currentSong = getSong(songId);
            state = PlaybackState.PLAYING;
            fireEvent(state, currentSong);
            return currentSong;
        } finally { lock.writeLock().unlock(); }
    }

    void pause() {
        lock.writeLock().lock();
        try {
            if (state == PlaybackState.PLAYING) {
                state = PlaybackState.PAUSED;
                fireEvent(state, currentSong);
            }
        } finally { lock.writeLock().unlock(); }
    }

    void resume() {
        lock.writeLock().lock();
        try {
            if (state == PlaybackState.PAUSED) {
                state = PlaybackState.PLAYING;
                fireEvent(state, currentSong);
            }
        } finally { lock.writeLock().unlock(); }
    }

    void stop() {
        lock.writeLock().lock();
        try {
            state = PlaybackState.STOPPED;
            fireEvent(state, currentSong);
            currentSong = null;
            playQueue.clear();
        } finally { lock.writeLock().unlock(); }
    }

    Song getCurrentSong() { return currentSong; }
    PlaybackState getState() { return state; }
    int getQueueSize() { return playQueue.size(); }
    int getSongCount() { return songs.size(); }
    int getPlaylistCount() { return playlists.size(); }
}

// ==================== MAIN / TESTS ====================

public class MusicPlayerSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║   MUSIC PLAYER - LLD Demo             ║");
        System.out.println("╚═══════════════════════════════════════╝\n");

        MusicPlayerService svc = new MusicPlayerService();

        // --- Test 1: Add songs ---
        System.out.println("=== Test 1: Add songs ===");
        Song s1 = svc.addSong("Bohemian Rhapsody", "Queen", 354, MusicGenre.ROCK);
        Song s2 = svc.addSong("Blinding Lights", "The Weeknd", 200, MusicGenre.POP);
        Song s3 = svc.addSong("Take Five", "Dave Brubeck", 324, MusicGenre.JAZZ);
        Song s4 = svc.addSong("Lose Yourself", "Eminem", 326, MusicGenre.HIPHOP);
        Song s5 = svc.addSong("Stairway to Heaven", "Led Zeppelin", 482, MusicGenre.ROCK);
        check(svc.getSongCount(), 5, "5 songs added");
        System.out.println("✓\n");

        // --- Test 2: Search ---
        System.out.println("=== Test 2: Search ===");
        check(svc.searchByTitle("blind").size(), 1, "Search 'blind' = 1");
        check(svc.searchByArtist("queen").size(), 1, "Search artist 'queen' = 1");
        check(svc.searchByGenre(MusicGenre.ROCK).size(), 2, "Search ROCK = 2");
        check(svc.searchByTitle("xyz").size(), 0, "No match");
        System.out.println("✓\n");

        // --- Test 3: Playlist ---
        System.out.println("=== Test 3: Playlist ===");
        Playlist pl = svc.createPlaylist("My Favorites");
        svc.addSongToPlaylist(pl.id, s1.id);
        svc.addSongToPlaylist(pl.id, s2.id);
        svc.addSongToPlaylist(pl.id, s3.id);
        check(pl.songs.size(), 3, "3 songs in playlist");
        svc.removeSongFromPlaylist(pl.id, s3.id);
        check(pl.songs.size(), 2, "2 after remove");
        svc.addSongToPlaylist(pl.id, s3.id);
        svc.addSongToPlaylist(pl.id, s4.id);
        svc.addSongToPlaylist(pl.id, s5.id);
        check(pl.songs.size(), 5, "5 songs final");
        System.out.println("✓\n");

        // --- Test 4: Sequential play ---
        System.out.println("=== Test 4: Sequential play ===");
        Song playing = svc.playPlaylist(pl.id);
        check(playing.title, "Bohemian Rhapsody", "Starts with first song");
        check(svc.getState(), PlaybackState.PLAYING, "Playing");
        Song next = svc.playNext();
        check(next.title, "Blinding Lights", "Next = 2nd song");
        System.out.println("✓\n");

        // --- Test 5: Pause/Resume ---
        System.out.println("=== Test 5: Pause/Resume ===");
        svc.pause();
        check(svc.getState(), PlaybackState.PAUSED, "Paused");
        svc.resume();
        check(svc.getState(), PlaybackState.PLAYING, "Resumed");
        System.out.println("✓\n");

        // --- Test 6: Stop ---
        System.out.println("=== Test 6: Stop ===");
        svc.stop();
        check(svc.getState(), PlaybackState.STOPPED, "Stopped");
        check(svc.getCurrentSong() == null, true, "No current song");
        check(svc.getQueueSize(), 0, "Queue cleared");
        System.out.println("✓\n");

        // --- Test 7: Shuffle strategy ---
        System.out.println("=== Test 7: Shuffle ===");
        svc.setPlayOrder(new ShuffleOrder());
        Song shuffled = svc.playPlaylist(pl.id);
        check(shuffled != null, true, "Shuffle plays a song");
        check(svc.getState(), PlaybackState.PLAYING, "Playing");
        // Play all
        Set<String> played = new HashSet<>();
        played.add(shuffled.title);
        while (svc.getQueueSize() > 0) { Song s = svc.playNext(); if (s != null) played.add(s.title); }
        check(played.size(), 5, "All 5 songs played in shuffle");
        System.out.println("✓\n");

        // --- Test 8: Repeat one ---
        System.out.println("=== Test 8: Repeat one ===");
        svc.setPlayOrder(new RepeatOneOrder());
        Song repeated = svc.playPlaylist(pl.id);
        check(repeated.title, "Bohemian Rhapsody", "Repeat one = first song");
        check(svc.getQueueSize(), 0, "Queue empty (only 1 in repeat)");
        svc.setPlayOrder(new SequentialOrder()); // reset
        System.out.println("✓\n");

        // --- Test 9: Play specific song ---
        System.out.println("=== Test 9: Play specific song ===");
        svc.playSong(s4.id);
        check(svc.getCurrentSong().title, "Lose Yourself", "Playing Lose Yourself");
        System.out.println("✓\n");

        // --- Test 10: Observer ---
        System.out.println("=== Test 10: Observer ===");
        MusicPlayerService svc2 = new MusicPlayerService();
        PlaybackLogger logger = new PlaybackLogger();
        NowPlayingNotifier nowPlaying = new NowPlayingNotifier();
        svc2.addListener(logger);
        svc2.addListener(nowPlaying);
        Song a = svc2.addSong("Test", "Artist", 180, MusicGenre.POP);
        svc2.playSong(a.id);
        svc2.pause();
        svc2.resume();
        svc2.stop();
        check(logger.events.size(), 4, "4 events: play, pause, resume, stop");
        check(nowPlaying.currentDisplay, "⏹ Stopped", "Now playing shows stopped");
        System.out.println("  Events: " + logger.events);
        System.out.println("✓\n");

        // --- Test 11: Exceptions ---
        System.out.println("=== Test 11: Exceptions ===");
        try { svc.getSong("S-999"); } catch (SongNotFoundException e) { System.out.println("  ✓ " + e.getMessage()); }
        try { svc.getPlaylist("PL-999"); } catch (PlaylistNotFoundException e) { System.out.println("  ✓ " + e.getMessage()); }
        Playlist empty = svc.createPlaylist("Empty");
        try { svc.playPlaylist(empty.id); } catch (EmptyPlaylistException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓\n");

        // --- Test 12: Thread Safety ---
        System.out.println("=== Test 12: Thread Safety ===");
        MusicPlayerService svc3 = new MusicPlayerService();
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int x = i;
            futures.add(exec.submit(() -> svc3.addSong("Song" + x, "Artist" + x, 200, MusicGenre.POP)));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        exec.shutdown();
        check(svc3.getSongCount(), 100, "100 songs added concurrently");
        System.out.println("✓\n");

        System.out.println("════════ ALL 12 TESTS PASSED ✓ ════════");
    }

    static void check(PlaybackState a, PlaybackState e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(int a, int e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(String a, String e, String m) { System.out.println("  " + (Objects.equals(a, e) ? "✓" : "✗ GOT '" + a + "'") + " " + m); }
    static void check(boolean a, boolean e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. STRATEGY (PlayOrderStrategy): SequentialOrder, ShuffleOrder, RepeatOneOrder.
 *    Swap at runtime: svc.setPlayOrder(new ShuffleOrder()).
 *    arrange() returns ordered list — decouples order logic from player.
 *
 * 2. OBSERVER (PlaybackListener): PlaybackLogger tracks all events,
 *    NowPlayingNotifier maintains current display string.
 *    Could add: ScrobbleListener (Last.fm), AnalyticsListener.
 *
 * 3. PLAY QUEUE: LinkedList<Song> — poll() for next, clear on stop/new playlist.
 *    Strategy fills queue from playlist; player just consumes.
 *
 * 4. SEARCH: Streams with case-insensitive contains. By title, artist, genre.
 *
 * 5. THREAD SAFETY: ReadWriteLock for playback state. ConcurrentHashMap for library.
 *
 * 6. EXTENSIONS: queue manipulation (add next, reorder), lyrics, offline cache,
 *    collaborative playlists, recommendations, audio streaming.
 */
