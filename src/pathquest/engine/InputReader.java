package pathquest.engine;

import java.util.Scanner;
import pathquest.model.Command;

public class InputReader implements Runnable {
    private final Scanner in;
    private final BoundedBuffer<Command> buffer;

    public InputReader(Scanner in, BoundedBuffer<Command> buffer) {
        this.in = in;
        this.buffer = buffer;
    }

    @Override
    public void run() {
        while (true) {
            if (!in.hasNextLine()) {
                buffer.put(new Command(Command.EXIT));
                return;
            }
            String line = in.nextLine();
            if (buffer.isClosed()) {
                return;
            }
            Command cmd;
            try {
                cmd = Command.parse(line);
            } catch (InvalidMoveException e) {
                System.out.println(e.getMessage());
                continue;
            }
            if (!buffer.put(cmd) || cmd.getName().equals(Command.EXIT)) {
                return;
            }
        }
    }
}
