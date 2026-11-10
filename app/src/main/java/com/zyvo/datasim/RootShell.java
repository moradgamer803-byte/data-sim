package com.zyvo.datasim;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Runs commands as root (su -c) and returns output. */
final class RootShell {

    static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    static final class Result {
        final int code;
        final String out;

        Result(int code, String out) {
            this.code = code;
            this.out = out;
        }

        boolean ok() {
            return code == 0;
        }
    }

    private RootShell() {
    }

    static Result run(String cmd) {
        return run(cmd, 12000);
    }

    static Result run(String cmd, int timeoutMs) {
        Process p = null;
        final StringBuilder sb = new StringBuilder();
        try {
            p = new ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start();
            final Process proc = p;
            Thread reader = new Thread(new Runnable() {
                @Override
                public void run() {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            sb.append(line).append('\n');
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
            reader.start();
            boolean done = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!done) {
                p.destroy();
                return new Result(-1, "timeout");
            }
            reader.join(1500);
            return new Result(p.exitValue(), sb.toString().trim());
        } catch (Exception e) {
            return new Result(-1, e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }
}
