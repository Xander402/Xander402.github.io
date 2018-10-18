package musicplayer;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class MusicPlayer {

    // All standard 99 notes from A0 (A,,) to B8 (B''''') and utility entries
    static final String[] NOTATIONS = new String[]{
        "pause", "A0", "As0", "B0",
        "C1", "Cs1", "D1", "Ds1", "E1", "F1", "Fs1", "G1", "Gs1", "A1", "As1", "B1",
        "C2", "Cs2", "D2", "Ds2", "E2", "F2", "Fs2", "G2", "Gs2", "A2", "As2", "B2",
        "C3", "Cs3", "D3", "Ds3", "E3", "F3", "Fs3", "G3", "Gs3", "A3", "As3", "B3",
        "C4", "Cs4", "D4", "Ds4", "E4", "F4", "Fs4", "G4", "Gs4", "A4", "As4", "B4",
        "C5", "Cs5", "D5", "Ds5", "E5", "F5", "Fs5", "G5", "Gs5", "A5", "As5", "B5",
        "C6", "Cs6", "D6", "Ds6", "E6", "F6", "Fs6", "G6", "Gs6", "A6", "As6", "B6",
        "C7", "Cs7", "D7", "Ds7", "E7", "F7", "Fs7", "G7", "Gs7", "A7", "As7", "B7",
        "C8", "Cs8", "D8", "Ds8", "E8", "F8", "Fs8", "G8", "Gs8", "A8", "As8", "B8",
        "click", "bass", "snare"
    };
    static final int DRUM_CLICK = 100, DRUM_BASS = 101, DRUM_SNARE = 102;

    static final String[] INSTRUMENTS = {
        "test", "sinwave", "sqwave", "sawtoothwave",
        "trianglewave", "drum", "draworgan", "oboe",
        "flute"
    };
    static final int DEFAULT_SAMPLE_RATE = 16384;
    static final String NSMF_DIR = setNsmfDir();

    static File inputFile;
    static String file;
    static String metaLine;
    static String[] CHANNELS;
    static Config[] CONFIGS;
    static int[] DURATIONS;
    static Note[][] NOTES;
    static String[][] notes;

    static Dimension[] pixels = new Dimension[3840];
    static byte[][] samplesBuffer;
    static double durationMultipler;
    static MainFrame window;

    // Setting 'nsmf' folder on the desktop as the default path, creating it if does not exist yet
    static String setNsmfDir() {
        String path = System.getProperty("user.home") + "/Desktop/nsmf";
        if (!new File(path).exists()) {
            new File(path).mkdir();
        }
        return path;
    }

    // File validity checking
    static void checkFile() throws FileNotFoundException {
        Scanner fileScanner;
        try {
            fileScanner = new Scanner(inputFile);
            metaLine = fileScanner.nextLine();
            String gconfigLine = fileScanner.nextLine();
            if (!metaLine.startsWith("#META filetype nsmf") || !gconfigLine.startsWith("globalconfig")) {
                throw new IllegalArgumentException("The selected file is not a valid NSMF file.");
            }
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("The selected file is not a valid NSMF file.");
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("Could not find the selected file.");
        }
    }

    // Getting frequency of a given key using formula:
    // f(n) = 2^((n-49)/12) * 440Hz
    // with the A4 key at 440.0Hz
    static double getKeyFrequency(int keyNumber) {
        return keyNumber > 0 ? Math.pow(Math.pow(2, 1.0 / 12), keyNumber - 49) * 440 : 0;
    }

    // Finding the index of a given key notation
    static <T> int findKeyNumber(T key) {
        return indexOf(NOTATIONS, key);
    }

    // Checking if the given array contains the given element
    static boolean arrayContains(final Object[] array, final Object objectToFind) {
        return indexOf(array, objectToFind) != -1;
    }

    // Finding the index of a given element in array
    static int indexOf(final Object[] array, final Object objectToFind) {
        if (array == null) {
            return -1;
        }
        if (objectToFind == null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < array.length; i++) {
                if (objectToFind.equals(array[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    // Generating a sequence of audio samples
    static byte[] createWaveBuffer(int number, int ms, double volume, int samplerate, String instrument) {
        int samples = (int) ((ms * samplerate) / 1000);
        byte[] output = new byte[samples];
        double freq = getKeyFrequency(number);
        double period = (double) samplerate / freq;
        double rad_divBy_period = 2.0 * Math.PI / period;
        if (!instrument.equals("drum") && number >= DRUM_CLICK) {
            throw new UnsupportedOperationException("Notes in a melodic channel can not be drum notes.");
        }
        switch (instrument) {
            case "sinwave":
                for (int i = 0; i < output.length; i++) {
                    double angle = rad_divBy_period * i;
                    double sample = (byte) (Math.sin(angle) * 127);
                    output[i] = (byte) ((byte) (sample) * volume);
                    /*if (i < pixels.length) {
                        pixels[i] = new Dimension(i, (int) output[i]);
                    }*/
                }
                break;
            case "sqwave":
                for (int i = 0; i < output.length; i++) {
                    double angle = rad_divBy_period * i;
                    double sample = Math.sin(angle) * 127;
                    if (sample > 0) {
                        output[i] = (byte) ((byte) 32 * volume);
                    } else if (sample < 0) {
                        output[i] = (byte) ((byte) -32 * volume);
                    } else {
                        output[i] = 0;
                    }
                    /*if (i < pixels.length) {
                        pixels[i] = new Dimension(i, (int) output[i]);
                    }*/
                }
                break;
            case "drum":
                if (number < DRUM_CLICK) {
                    throw new UnsupportedOperationException("Notes in a drum channel can only be drum notes.");
                }
                output[0] = 0;
                for (int i = 1; i < output.length; i++) {
                    double sample = 0;
                    switch (number) {
                        case DRUM_CLICK:
                            sample = 30000.0 * Math.sin(0.75 * Math.sqrt(i)) / i;
                            break;
                        case DRUM_BASS:
                            sample = 250000.0 * Math.sin(0.3 * Math.sqrt(i)) / i;
                            break;
                        case DRUM_SNARE:
                            sample = i < 500
                                    ? 255 * (Math.random() - 0.5)
                                    : 255 * (Math.random() - 0.5) * (500.0 / (6 * i));
                        default:
                            break;
                    }
                    output[i] = (byte) ((byte) (sample) * volume);
                    /*if (i < pixels.length) {
                        pixels[i] = new Dimension(i, (int) output[i]);
                    }*/
                }
                break;
            case "sawtoothwave":
                for (int i = 0; i < output.length; i++) {
                    double sample = (byte) (-(i * freq / 64) % 128) / 3;
                    output[i] = (byte) ((byte) (sample) * volume);
                    /*if (i < pixels.length) {
                        pixels[i] = new Dimension(i, (int) output[i]);
                    }*/
                }
                break;
            case "trianglewave":
                for (int i = 0; i < output.length; i++) {
                    double sample = (byte) (((Math.abs(((i / 8195.0 * freq) % 2) - 1) - 0.5) * 100.0));
                    output[i] = (byte) ((byte) (sample) * volume);
                    /*if (i < pixels.length) {
                        pixels[i] = new Dimension(i, (int) output[i]);
                    }*/
                }
                break;
            case "draworgan":
                for (int i = 0; i < output.length; i++) {
                    double angle = rad_divBy_period * i;
                    double sample = (byte) ((0
                            + Math.sin(angle)
                            + Math.cos(2 * angle)
                            + Math.sin(3 * angle)
                            + Math.cos(4 * angle)
                            - (0.25 * Math.sin(5 * angle))
                            - Math.cos(6 * angle)
                            - (0.075 * Math.sin(8 * angle)))
                            * 20);
                    output[i] = (byte) ((byte) (sample) * volume);
                    /*if (i < pixels.length) {
                        pixels[i] = new Dimension(i, (int) output[i]);
                    }*/
                }
                break;
            case "oboe":
                for (int i = 0; i < output.length; i++) {
                    double angle = rad_divBy_period * i;
                    double attackEnvelope = 1;
                    double waveEnvelope = (1 + (3 * (Math.sin(0.000002 * Math.pow(angle, 2)))
                            / Math.sqrt(2 * angle)));
                    if (i < 1500) {
                        attackEnvelope = i / 1500.0;
                    }
                    double sample = (byte) ((0
                            - Math.sin(angle)
                            + Math.sin(2 * angle)
                            - Math.sin(3 * angle)
                            + Math.sin(4 * angle)
                            + Math.sin(5 * angle)
                            + Math.sin(6 * angle)
                            + Math.sin(7 * angle)
                            + (0.37 * Math.sin(8 * angle)))
                            * attackEnvelope
                            * waveEnvelope
                            * 12);
                    output[i] = (byte) ((byte) (sample) * volume);
                    /*if (i < pixels.length) {
                        pixels[i] = new Dimension(i, (int) output[i]);
                    }*/
                }
                break;
            case "flute":
                for (int i = 0; i < output.length; i++) {
                    double angle = rad_divBy_period * i;
                    double attackEnvelope = 1;
                    double waveEnvelope = (1 + (3 * (Math.sin(0.000002 * Math.pow(angle, 2)))
                            / Math.sqrt(2 * angle)));
                    if (i < 1000) {
                        attackEnvelope = i / 1000.0;
                    }
                    double sample = (byte) ((0
                            - Math.sin(angle)
                            - Math.sin(2 * angle)
                            - (0.185 * Math.sin(3 * angle))
                            + (0.05 * Math.cos(4 * angle)))
                            * attackEnvelope
                            * waveEnvelope
                            * 60);
                    output[i] = (byte) ((byte) (sample) * volume);
                    /*if (i < pixels.length) {
                        pixels[i] = new Dimension(i, (int) output[i]);
                    }*/
                }
            default:
                break;
        }
        //System.out.println(ArrayUtils.toString(output));
        return output;
    }

    static byte[] createEmptyWaveBuffer(int ms) {
        int samples = (int) ((ms * DEFAULT_SAMPLE_RATE) / 1000);
        byte[] output = new byte[samples];
        for (int i = 0; i < output.length; i++) {
            output[i] = 0;
        }
        return output;
    }

    public static void main(String[] args) {
        window = new MainFrame();
    }

    static void play()
            throws LineUnavailableException, FileNotFoundException {
        file = file.replace(metaLine, "")
                .replace("\t", "")
                .replace("\n\n", "\n")
                .replace("\n", " ");

        String gconfig = "";
        Pattern gpattern = Pattern.compile("globalconfig(.*?)endglobalconfig");
        Matcher gmatcher = gpattern.matcher(file);
        while (gmatcher.find()) {
            gconfig = gmatcher.group(1);
        }
        durationMultipler = 1.0 / (Double.parseDouble(gconfig
                .replace(" tempo ", "").replace("% ", "")) / 100.0);

        file = file.replace(gconfig, "")
                .replace(" globalconfigendglobalconfig", "");

        List<String> scannedLoops = new ArrayList<>();
        Pattern loopPattern = Pattern.compile("loop(.*?)endloop");
        Matcher loopMatcher = loopPattern.matcher(file);
        while (loopMatcher.find()) {
            scannedLoops.add(loopMatcher.group(1));
        }
        String[] loops = scannedLoops.toArray(new String[0]);
        for (String loop : loops) {
            if (loop.contains("loop")) {
                throw new UnsupportedOperationException("Nested loops are not allowed.");
            }
            String[] loopContent = loop.split(" ");
            int count = Integer.parseInt(loopContent[1]);
            if (count < 2) {
                throw new IllegalArgumentException("Looping less than 2 times is not allowed.");
            }
            String loopNotes = " ";
            for (int it = 0; it < count; it++) {
                for (int j = 3; j < loopContent.length; j++) {
                    loopNotes += (loopContent[j] + " ");
                }
            }
            file = file.replace(loop, loopNotes)
                    .replace("  ", " ")
                    .replace(" endloop", "")
                    .replace("loop", "");
        }
        file = file.replace("  ", " ");
        //System.out.println(file);
        CHANNELS = file
                .substring(0, file.length() - 1)
                .split("endchannel");
        int channelsCount = CHANNELS.length;

        DURATIONS = new int[channelsCount];
        CONFIGS = new Config[channelsCount];
        samplesBuffer = new byte[channelsCount][];
        for (int i = 0; i < channelsCount; i++) {
            String config = "";
            Pattern pattern = Pattern.compile("config(.*?)endconfig");
            Matcher matcher = pattern.matcher(CHANNELS[i]);
            if (matcher.find()) {
                config = matcher.group(1);
            }
            DURATIONS[i] = 0;
            CONFIGS[i] = new Config(config);
            CHANNELS[i] = CHANNELS[i]
                    .replace(" channel ", "")
                    .replace("channel ", "")
                    .replace(config, "")
                    .replace("configendconfig ", "");
            //System.out.println(CHANNELS[i]);
        }
        int biggestChannelLength = CHANNELS[0].split(" ").length;
        try {
            for (int i = 0; i < channelsCount; i++) {
                biggestChannelLength
                        = Math.max(biggestChannelLength, CHANNELS[i + 1].split(" ").length);
            }
        } catch (Exception e) {
        }
        NOTES = new Note[channelsCount][biggestChannelLength];
        notes = new String[channelsCount][biggestChannelLength];
        for (int i = 0; i < notes.length; i++) {
            notes[i] = CHANNELS[i].split(" ");
            try {
                for (int j = 0; j < notes[i].length; j++) {
                    NOTES[i][j] = new Note(notes[i][j]);
                    DURATIONS[i] += NOTES[i][j].duration;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new UnsupportedOperationException(
                        "Distance between lines can not be bigger than one empty line.");
            }
        }
        for (int i = 0; i < channelsCount; i++) {
            byte[] toneBuffer;
            ArrayList<Byte> buffer = new ArrayList<>();
            for (Note note : NOTES[i]) {
                if (note != null) {
                    if (note.number > 0) {
                        toneBuffer = createWaveBuffer(
                                note.number + CONFIGS[i].transpos,
                                note.duration,
                                CONFIGS[i].volume,
                                CONFIGS[i].samplerate,
                                CONFIGS[i].instrument
                        );
                    } else {
                        toneBuffer = createEmptyWaveBuffer(note.duration);
                    }
                    for (byte sample : toneBuffer) {
                        buffer.add(sample);
                    }
                    //Frame frame = new WaveFrame(pixels, CONFIGS[i].instrument);
                }
            }
            toneBuffer = new byte[buffer.size()];
            Iterator<Byte> iterator = buffer.iterator();
            for (int j = 0; j < toneBuffer.length; j++) {
                toneBuffer[j] = iterator.next();
            }
            samplesBuffer[i] = toneBuffer;

        }
        for (int i = 0; i < channelsCount; i++) {
            new Thread(new PlayChannel(i)).start();
        }
        //new Thread(new PlayChannel(0)).start();
    }

    public static class MainFrame extends JFrame {

        JButton playButton;
        JProgressBar durationBar;
        JLabel infoLabel;

        public MainFrame() {
            setLayout(new GridLayout(3, 1));
            Font font = new Font("Arial", Font.BOLD, 24);
            Font font2 = new Font("Arial", Font.BOLD, 18);
            this.setSize(350, 150);
            this.setTitle("NSMF Player");
            this.setResizable(false);
            this.setVisible(true);
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            playButton = new JButton("Play a song");
            playButton.setFont(font);
            playButton.addActionListener(new PlayEvent());
            add(playButton);

            durationBar = new JProgressBar();
            durationBar.setFont(font);
            durationBar.setStringPainted(true);
            durationBar.setString("");
            add(durationBar);

            infoLabel = new JLabel("Select an NSMF file.");
            infoLabel.setFont(font2);
            add(infoLabel);
        }

        class PlayEvent implements ActionListener {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    infoLabel.setText("Rendering soundwaves...");
                    file = chooseFileToString();
                    checkFile();
                    play();
                    infoLabel.setText("Playing \"" + inputFile.getName().replace(".nsmf", "") + "\"");
                    int longestDuration = Collections.max(
                            Arrays.stream(DURATIONS).boxed().collect(Collectors.toList()));
                    new Thread(new DurationBar(longestDuration)).start();
                } catch (LineUnavailableException | FileNotFoundException ex) {
                }
            }
        }

        class DurationBar implements Runnable {

            int duration;

            public DurationBar(int duration) {
                this.duration = duration;
            }

            public void run() {
                durationBar.setValue(0);
                double stepValue = duration / 100.0;
                try {
                    for (int i = 0; i <= 100; i++) {
                        durationBar.setValue(i);
                        durationBar.setString(
                                (int) (stepValue * i / 1000)
                                + "s / "
                                + duration / 1000
                                + "s");
                        new Robot().delay((int) stepValue);
                    }
                } catch (AWTException ex) {
                }
            }
        }
    }

    public static class WaveFrame extends JFrame {

        public Dimension[] dim;

        public WaveFrame(Dimension[] x, String name) {
            this.dim = x;
            this.setSize(1920, 1000);
            this.setTitle(name + " wave");
            this.setResizable(false);
            this.setVisible(true);
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            for (int i = 0; i < dim.length - 2; i++) {
                try {
                    g.drawLine(
                            dim[i].width / 2,
                            dim[i].height * 2 + 500,
                            dim[i + 1].width / 2,
                            dim[i + 1].height * 2 + 500
                    );
                } catch (Exception e) {
                }
            }
        }
    }

    public static String chooseFileToString() {
        StringBuilder sb = new StringBuilder();
        JFileChooser filechooser = new JFileChooser(NSMF_DIR);
        if (filechooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            inputFile = filechooser.getSelectedFile();
        } else {
            return null;
        }
        Scanner input;
        try {
            input = new Scanner(inputFile);
            while (input.hasNext()) {
                sb.append(input.nextLine());
                sb.append("\n");
            }
            input.close();
        } catch (FileNotFoundException ex) {
            return null;
        }
        return sb.toString();
    }

    static class Note {

        public int duration, number;

        private Note(String rawNote) {
            String[] separated = rawNote.split(",");
            this.number = findKeyNumber(separated[0]);
            this.duration = (int) (Integer.parseInt(separated[1])
                    * durationMultipler);
            if (number == -1) {
                throw new UnsupportedOperationException("Note \"" + NOTATIONS[number] + "\" is not available.");
            }
        }
    }

    static class Config {

        public double volume;
        public int transpos, samplerate;
        public String instrument;

        private Config(String rawConfig) {
            String[] separated = rawConfig
                    .replace("volume=", "")
                    .replace("transpos=", "")
                    .replace("samplerate=", "")
                    .replace("instrument=", "")
                    .split(" ");
            try {
                this.volume = Integer.parseInt(separated[1]) / 100.0;
            } catch (NumberFormatException e) {
                throw new NumberFormatException(
                        "\"" + separated[1] + "\" is an invalid volume value.");
            }
            try {
                this.transpos = Integer.parseInt(separated[2]);
            } catch (NumberFormatException e) {
                throw new NumberFormatException(
                        "\"" + separated[2] + "\" is an invalid transposition value.");
            }
            try {
                if (separated[3].equals("default")) {
                    this.samplerate = DEFAULT_SAMPLE_RATE;
                } else if (separated[3].contains("kHz")) {
                    this.samplerate = (int) Double.parseDouble(separated[3].replace("kHz", "")) * 1024;
                } else if (separated[3].contains("Hz")) {
                    this.samplerate = Integer.parseInt(separated[3].replace("Hz", ""));
                } else {
                    throw new NumberFormatException(
                            "\"" + separated[3] + "\" is an invalid sample rate value.");
                }
            } catch (NumberFormatException e) {
                throw new NumberFormatException(
                        "\"" + separated[3] + "\" is an invalid sample rate value.");
            }
            this.instrument = separated[4];

            if (!arrayContains(INSTRUMENTS, instrument)) {
                throw new UnsupportedOperationException(
                        "Instrument \"" + instrument + "\" is not available.");
            }
            if (instrument.equals("drum") && transpos != 0) {
                throw new IllegalArgumentException(
                        "Transposition of a drum channel can only be 0.");
            }
        }
    }

    static class PlayChannel implements Runnable {

        int channelNumber;

        public PlayChannel(int channelNumber) {
            this.channelNumber = channelNumber;
        }

        public void run() {
            final int sr = CONFIGS[channelNumber].samplerate;
            final AudioFormat af = new AudioFormat(sr, 8, 1, true, true);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
                line.open(af, sr);
                line.start();
                line.write(samplesBuffer[channelNumber], 0, samplesBuffer[channelNumber].length);
                line.drain();
            } catch (LineUnavailableException e) {
            }
        }
    }
}
