package pathquest.store;

public class AccountLockedException extends Exception {
    public AccountLockedException(String username) {
        super("Too many wrong PINs for " + username + ": login is locked until the game is restarted");
    }
}
