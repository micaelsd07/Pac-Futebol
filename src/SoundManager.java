import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundManager {

    private static final float SAMPLE_RATE = 44100f;
    private boolean muted = false;
    private boolean stepToggle = false;

    private final ExecutorService soundPool = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "SoccerArcadeSoundThread");
        t.setDaemon(true);
        return t;
    });

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public void toggleMute() {
        this.muted = !this.muted;
    }

    public void playPelletSound() {
        if (muted) return;
        stepToggle = !stepToggle;
        final boolean high = stepToggle;
        soundPool.submit(() -> {
            if (high) {
                playSweep(360, 540, 42, 0.20, WaveType.TRIANGLE);
            } else {
                playSweep(500, 320, 42, 0.20, WaveType.TRIANGLE);
            }
        });
    }

    public void playCoinOrStarSound() {
        if (muted) return;
        soundPool.submit(() -> {
            playTone(784, 45, 0.25, WaveType.SQUARE);
            playTone(1046, 70, 0.26, WaveType.SQUARE);
        });
    }

    public void playPowerPelletSound() {
        if (muted) return;
        soundPool.submit(() -> {
            playSweep(350, 820, 85, 0.30, WaveType.SQUARE);
            playSweep(580, 1120, 110, 0.30, WaveType.SQUARE);
        });
    }

    public void playGoldenBallUnlockedSound() {
        if (muted) return;
        soundPool.submit(() -> {
            int[] notes = {523, 659, 784, 1046, 1318};
            for (int n : notes) {
                playTone(n, 70, 0.32, WaveType.SQUARE);
                sleepQuietly(15);
            }
        });
    }

    public void playEatGhostSound() {
        if (muted) return;
        soundPool.submit(() -> {
            int[] freqs = {392, 523, 659, 784, 1046, 1318};
            for (int f : freqs) {
                playTone(f, 42, 0.32, WaveType.SQUARE);
            }
        });
    }

    public void playLoseLifeSound() {
        if (muted) return;
        soundPool.submit(() -> {
            // Apito do juiz seguido de queda arcade
            playSweep(2400, 2650, 140, 0.28, WaveType.SQUARE);
            sleepQuietly(30);
            playSweep(640, 320, 140, 0.32, WaveType.SAW);
            playSweep(480, 210, 160, 0.32, WaveType.SAW);
            playSweep(320, 110, 220, 0.35, WaveType.SQUARE);
        });
    }

    public void playVictorySound() {
        if (muted) return;
        soundPool.submit(() -> {
            // Fanfarra de GOL / Conquista da Bola de Ouro
            int[] notes = {523, 659, 784, 1046, 784, 1046, 1318};
            int[] durations = {100, 100, 100, 200, 100, 160, 340};
            for (int i = 0; i < notes.length; i++) {
                playTone(notes[i], durations[i], 0.33, WaveType.SQUARE);
                sleepQuietly(20);
            }
        });
    }

    public void playGameOverSound() {
        if (muted) return;
        soundPool.submit(() -> {
            int[] notes = {494, 466, 440, 392, 349, 330, 262};
            int[] durations = {130, 130, 130, 150, 150, 170, 380};
            for (int i = 0; i < notes.length; i++) {
                playTone(notes[i], durations[i], 0.32, WaveType.SAW);
                sleepQuietly(25);
            }
        });
    }

    public void playMenuClickSound() {
        if (muted) return;
        soundPool.submit(() -> playTone(660, 45, 0.22, WaveType.SQUARE));
    }

    public void playStartGameSound() {
        if (muted) return;
        soundPool.submit(() -> {
            // Apito inicial de partida + fanfarra esportiva
            playSweep(2200, 2550, 160, 0.25, WaveType.SQUARE);
            sleepQuietly(45);
            int[] notes = {330, 392, 523, 659};
            int[] durations = {85, 85, 95, 170};
            for (int i = 0; i < notes.length; i++) {
                playTone(notes[i], durations[i], 0.28, WaveType.SQUARE);
                sleepQuietly(18);
            }
        });
    }

    private enum WaveType {
        SINE, SQUARE, TRIANGLE, SAW
    }

    private void playTone(double freq, int durationMs, double volume, WaveType wave) {
        playSweep(freq, freq, durationMs, volume, wave);
    }

    private void playSweep(double startFreq, double endFreq, int durationMs, double volume, WaveType wave) {
        if (muted) return;
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            int totalSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
            if (totalSamples <= 0) return;

            byte[] buffer = new byte[totalSamples];
            double phase = 0.0;

            for (int i = 0; i < totalSamples; i++) {
                double progress = (double) i / totalSamples;
                double currentFreq = startFreq + (endFreq - startFreq) * progress;
                phase += (2.0 * Math.PI * currentFreq) / SAMPLE_RATE;

                double env = 1.0;
                if (progress < 0.08) {
                    env = progress / 0.08;
                } else if (progress > 0.78) {
                    env = (1.0 - progress) / 0.22;
                }

                double sample;
                switch (wave) {
                    case SQUARE:
                        sample = Math.sin(phase) >= 0 ? 1.0 : -1.0;
                        break;
                    case TRIANGLE:
                        sample = (2.0 / Math.PI) * Math.asin(Math.sin(phase));
                        break;
                    case SAW:
                        double norm = (phase / (2.0 * Math.PI)) % 1.0;
                        sample = 2.0 * norm - 1.0;
                        break;
                    case SINE:
                    default:
                        sample = Math.sin(phase);
                        break;
                }

                buffer[i] = (byte) (sample * 127.0 * volume * env);
            }

            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format, Math.max(2048, totalSamples));
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
            }
        } catch (Exception ignored) {
        }
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
