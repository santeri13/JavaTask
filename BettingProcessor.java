import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class Player {
    private UUID playerId;
    private long balance;
    private int betsPlaced;
    private int betsWon;

    public Player(UUID playerId) {
        this.playerId = playerId;
        this.balance = 0;
        this.betsPlaced = 0;
        this.betsWon = 0;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getBalance() {
        return balance;
    }

    public void deposit(long amount) {
        balance += amount;
    }

    public boolean withdraw(long amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }

    public void placeBet(long amount, boolean won) {
        betsPlaced++;
        if (won) {
            betsWon++;
            balance += amount;
        } else {
            balance -= amount;
        }
    }

    public BigDecimal getWinRate() {
        if (betsPlaced == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(betsWon).divide(BigDecimal.valueOf(betsPlaced), 2, RoundingMode.HALF_UP);
    }
}

class Match {
    private UUID matchId;
    private double rateA;
    private double rateB;
    private String result;

    public Match(UUID matchId, double rateA, double rateB, String result) {
        this.matchId = matchId;
        this.rateA = rateA;
        this.rateB = rateB;
        this.result = result;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public double getRateA() {
        return rateA;
    }

    public double getRateB() {
        return rateB;
    }

    public String getResult() {
        return result;
    }
}

public class BettingProcessor {
    public static void main(String[] args) {
        List<Player> players = new ArrayList<>();
        List<Match> matches = new ArrayList<>();
        Map<UUID, String> playerActions = new LinkedHashMap<>();

        try (Scanner scanner = new Scanner(new File("player_data.txt"))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] values = line.split(",");
                UUID playerId = UUID.fromString(values[0]);
                String action = values[1];

                if (action.equals("DEPOSIT")) {
                    long amount = Long.parseLong(values[3]);
                    Player player = getPlayerOrCreate(playerId, players);
                    player.deposit(amount);
                } else if (action.equals("BET")) {
                    playerActions.put(playerId, line);
                } else if (action.equals("WITHDRAW")) {
                    long amount = values.length > 3 ? Long.parseLong(values[3]) : 0;
                    Player player = getPlayerOrCreate(playerId, players);
                    if (player.withdraw(amount)) {
                        playerActions.put(playerId, line);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try (Scanner scanner = new Scanner(new File("match_data.txt"))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] values = line.split(",");
                UUID matchId = UUID.fromString(values[0]);
                double rateA = Double.parseDouble(values[1]);
                double rateB = Double.parseDouble(values[2]);
                String result = values[3];
                matches.add(new Match(matchId, rateA, rateB, result));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Map<UUID, Player> playerMap = players.stream().collect(Collectors.toMap(Player::getPlayerId, Function.identity()));
        long casinoBalanceChange = 0;

        for (Match match : matches) {
            String result = match.getResult();
            UUID matchId = match.getMatchId();
            double rateA = match.getRateA();
            double rateB = match.getRateB();

            for (Map.Entry<UUID, String> entry : playerActions.entrySet()) {
                UUID playerId = entry.getKey();
                String action = entry.getValue();
                String[] values = action.split(",");
                UUID actionMatchId = UUID.fromString(values[2]);
                long amount = Long.parseLong(values[3]);
                String side = values[4];

                if (matchId.equals(actionMatchId)) {
                    Player player = playerMap.get(playerId);
                    if (action.startsWith(playerId + ",BET,")) {
                        if (side.equals(result)) {
                            player.placeBet(amount, true);
                        } else {
                            player.placeBet(amount, false);
                        }
                        casinoBalanceChange += (side.equals(result) ? amount * rateA : amount * rateB);
                    }
                }
            }
        }

        try (PrintWriter writer = new PrintWriter("result.txt")) {
            
            List<Player> legitimatePlayers = players.stream().filter(player -> !playerActions.containsKey(player.getPlayerId())).collect(Collectors.toList());
            legitimatePlayers.sort(Comparator.comparing(Player::getPlayerId));
            for (Player player : legitimatePlayers) {
                writer.println(player.getPlayerId() + " " + player.getBalance() + " " + player.getWinRate());
            }

            if (!legitimatePlayers.isEmpty()) {
                writer.println(); 
            }

            List<UUID> illegitimatePlayers = playerActions.keySet().stream().sorted().collect(Collectors.toList());
            for (UUID playerId : illegitimatePlayers) {
                writer.println(playerId + " " + playerActions.get(playerId));
            }

            if (!illegitimatePlayers.isEmpty()) {
                writer.println(); 
            }

            writer.println(casinoBalanceChange);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Player getPlayerOrCreate(UUID playerId, List<Player> players) {
        Optional<Player> optionalPlayer = players.stream().filter(player -> player.getPlayerId().equals(playerId)).findFirst();
        if (optionalPlayer.isPresent()) {
            return optionalPlayer.get();
        } else {
            Player newPlayer = new Player(playerId);
            players.add(newPlayer);
            return newPlayer;
        }
    }
}
