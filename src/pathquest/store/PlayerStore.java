package pathquest.store;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pathquest.model.Player;

public class PlayerStore implements Repository<Player, Integer> {
    public static final String FILE = "data/players.csv";
    public static final int MAX_TRIES = 3;

    private final String path;
    private final Map<String, Integer> failedTries = new HashMap<>();

    public PlayerStore(String path) {
        this.path = path;
    }

    public Player register(String username, String pin) throws IOException {
        if (!username.matches("[A-Za-z0-9]{3,20}")) {
            throw new IllegalArgumentException("Username must be 3-20 letters or digits");
        }
        checkPin(pin);
        if (findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("The username " + username + " is already taken, choose another");
        }
        Player player = new Player(0, 0);
        player.setUsername(username);
        player.setPin(pin);
        player.setCreatedAt(LocalDateTime.now().withNano(0));
        player.setPlayerId(save(player));
        return player;
    }

    public Optional<Player> findByUsername(String username) throws IOException {
        return findAll().stream().filter(p -> p.getUsername().equalsIgnoreCase(username)).findFirst();
    }

    public Optional<Player> login(String username, String pin) throws IOException, AccountLockedException {
        String key = username.toLowerCase();
        if (triesLeft(username) == 0) {
            throw new AccountLockedException(username);
        }
        Optional<Player> player = findByUsername(username).filter(p -> p.getPin().equals(pin));
        if (player.isPresent()) {
            failedTries.remove(key);
        } else {
            failedTries.merge(key, 1, Integer::sum);
        }
        return player;
    }

    public int triesLeft(String username) {
        return Math.max(0, MAX_TRIES - failedTries.getOrDefault(username.toLowerCase(), 0));
    }

    public void updatePin(Player player, String newPin) throws IOException {
        checkPin(newPin);
        player.setPin(newPin);
        update(player.getPlayerId(), player);
    }

    @Override
    public Integer save(Player player) throws IOException {
        List<Player> players = findAll();
        int id = 1;
        for (Player p : players) {
            id = Math.max(id, p.getPlayerId() + 1);
        }
        player.setPlayerId(id);
        players.add(player);
        writeAll(players);
        return id;
    }

    @Override
    public Optional<Player> findById(Integer id) throws IOException {
        return findAll().stream().filter(p -> p.getPlayerId() == id).findFirst();
    }

    @Override
    public List<Player> findAll() throws IOException {
        List<String> lines = DataFile.readLines(path);
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            players.add(toPlayer(lines.get(i), i + 1));
        }
        return players;
    }

    @Override
    public void update(Integer id, Player player) throws IOException {
        List<Player> players = findAll();
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getPlayerId() == id) {
                player.setPlayerId(id);
                player.setCreatedAt(players.get(i).getCreatedAt());
                players.set(i, player);
            }
        }
        writeAll(players);
    }

    @Override
    public void delete(Integer id) throws IOException {
        List<Player> players = findAll();
        players.removeIf(p -> p.getPlayerId() == id);
        writeAll(players);
    }

    private void writeAll(List<Player> players) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Player p : players) {
            lines.add(p.getPlayerId() + "," + p.getUsername() + "," + p.getPin() + "," + p.getCreatedAt());
        }
        DataFile.writeLines(path, lines);
    }

    private Player toPlayer(String line, int lineNo) throws IOException {
        String[] f = line.split(",");
        if (f.length != 4) {
            throw new IOException(path + " line " + lineNo + ": expected 4 fields, found " + f.length);
        }
        Player player = new Player(0, 0);
        player.setPlayerId(DataFile.number(f[0], path, lineNo));
        player.setUsername(f[1]);
        player.setPin(f[2]);
        try {
            player.setCreatedAt(LocalDateTime.parse(f[3]));
        } catch (DateTimeParseException e) {
            throw new IOException(path + " line " + lineNo + ": '" + f[3] + "' is not a date and time");
        }
        return player;
    }

    private static void checkPin(String pin) {
        boolean ok = pin.length() == 4 && pin.charAt(0) != '+' && pin.charAt(0) != '-';
        if (ok) {
            try {
                Integer.parseInt(pin);
            } catch (NumberFormatException e) {
                ok = false;
            }
        }
        if (!ok) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits");
        }
    }
}
