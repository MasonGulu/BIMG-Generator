package com.shrekshellraiser;

import com.shrekshellraiser.dithers.DitherFloydSteinberg;
import com.shrekshellraiser.dithers.DitherNone;
import com.shrekshellraiser.dithers.DitherOrdered;
import com.shrekshellraiser.dithers.IDither;
import com.shrekshellraiser.formats.BBF;
import com.shrekshellraiser.formats.BIMG;
import com.shrekshellraiser.formats.NFP;
import com.shrekshellraiser.modes.*;
import com.shrekshellraiser.palettes.Color;
import com.shrekshellraiser.palettes.DefaultPalette;
import com.shrekshellraiser.palettes.Palette;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ImageMaker {
    public static final Palette defaultPalette = DefaultPalette.defaultPalette;
    static Palette palette = defaultPalette;
    static IM_MODE mode = IM_MODE.LD;

    // default CC palette
    public static void main(String[] args) {
        boolean showHelp = false;
        boolean savePostImage = false;
        double secondsPerFrame = 0.2;
        boolean uncapResolution = false;
        boolean wipeFrames = false;
        boolean autoSingle = false;
        IDither dither = new DitherNone();
        IM_FILETYPE filetype = IM_FILETYPE.BIMG;
        String postImagePath = "";
        CommandLine commandLine;
        Option option_postImageFile = Option.builder("post")
                .required(false)
                .desc("Output processed com.shrekshellraiser.image to path")
                .hasArg(true)
                .build();
        Option option_doDither = Option.builder("d")
                .required(false)
                .desc("Do Floyd-Steinberg dithering")
                .longOpt("dither")
                .hasArg(false)
                .build();
        Option option_doOrderedDither = Option.builder("ordered")
                .required(false)
                .desc("Do ordered dithering")
                .valueSeparator(',')
                .hasArgs()
                .build();
        Option option_highDensity = Option.builder("hd")
                .required(false)
                .desc("High density")
                .hasArg(false)
                .build();
        Option option_customPalette = Option.builder("p")
                .required(false)
                .desc("Comma separated list of palette colors")
                .longOpt("palette")
                .hasArgs()
                .valueSeparator(',')
                .build();
        Option option_autoPalette = Option.builder("auto")
                .required(false)
                .desc("Automatically generate palette")
                .hasArg(false)
                .build();
        Option option_secondsPerFrame = Option.builder("spf")
                .required(false)
                .desc("Seconds per frame")
                .hasArg(true)
                .build();
        Option option_uncapResolution = Option.builder()
                .required(false)
                .desc("Uncap the resolution")
                .longOpt("uncapresolution")
                .hasArg(false)
                .build();
        Option option_bbf = Option.builder("bbf")
                .required(false)
                .desc("Save output in bbf format")
                .hasArg(false)
                .build();
        Option option_nfp = Option.builder("nfp")
                .required(false)
                .desc("Save output in nfp format")
                .hasArg(false)
                .build();
        Option option_wipeFrames = Option.builder()
                .required(false)
                .desc("Empty each frame of gifs")
                .longOpt("wipeframes")
                .hasArg(false)
                .build();
        Option option_autoSingle = Option.builder()
                .required(false)
                .desc("Automatically generate a palette for the first frame, and use the same for all others")
                .longOpt("autosingle")
                .hasArg(false)
                .build();
        Options options = new Options();
        CommandLineParser parser = new DefaultParser();

        options.addOption(option_postImageFile);
        options.addOption(option_doDither);
        options.addOption(option_doOrderedDither);
        options.addOption(option_highDensity);
        options.addOption(option_customPalette);
        options.addOption(option_autoPalette);
        options.addOption(option_secondsPerFrame);
        options.addOption(option_uncapResolution);
        options.addOption(option_bbf);
        options.addOption(option_nfp);
        options.addOption(option_wipeFrames);
        options.addOption(option_autoSingle);

        try {
            commandLine = parser.parse(options, args);

            if (commandLine.hasOption("post")) {
                savePostImage = true;
                postImagePath = commandLine.getOptionValue("post");
            }

            if (commandLine.hasOption("d"))
                dither = new DitherFloydSteinberg();
            else if (commandLine.hasOption("ordered")) {
                try {
                    int thresholdMapSize = Integer.decode(commandLine.getOptionValues("ordered")[0]);
                    double colorSpread = Double.parseDouble(commandLine.getOptionValues("ordered")[1]);
                    dither = new DitherOrdered(thresholdMapSize, colorSpread);
                } catch (NumberFormatException e) {
                    System.out.println("Please provide the threshold map size and color spread. Example usage: ");
                    System.out.println("-ordered 4,50");
                    return;
                }
            }

            uncapResolution = (commandLine.hasOption(option_uncapResolution));

            if (commandLine.hasOption("hd")) {
                mode = IM_MODE.HD;
            }

            if (commandLine.hasOption("auto") || commandLine.hasOption(option_autoSingle)) {
                mode = switch (mode) {
                    case HD -> IM_MODE.HD_AUTO;
                    case LD -> IM_MODE.LD_AUTO;
                    case HD_AUTO, LD_AUTO -> null; // should be impossible to reach
                };
            } else if (commandLine.hasOption("p")) {
                try {
                    String[] paletteColors = commandLine.getOptionValues("p");
                    Color[] colors = new Color[paletteColors.length];
                    for (int index = 0; index < paletteColors.length; index++) {
                        colors[index] = new Color(Integer.decode(paletteColors[index]));
                    }
                    palette = new Palette(colors);
                } catch (NumberFormatException e) {
                    System.out.println("Incorrectly formatted palette. Example usage: ");
                    System.out.println("-p=1,2,0xFF0000");
                    return;
                }
            }

            if (commandLine.hasOption("spf"))
                secondsPerFrame = Double.parseDouble(commandLine.getOptionValue("spf"));
            args = commandLine.getArgs(); // reset args to contain JUST the needed information

            if (commandLine.hasOption(option_bbf))
                filetype = IM_FILETYPE.BBF;
            else if (commandLine.hasOption(option_nfp))
                filetype = IM_FILETYPE.NFP;

            if (commandLine.hasOption(option_wipeFrames))
                wipeFrames = true;


            if (commandLine.hasOption(option_autoSingle))
                autoSingle = true;

        } catch (ParseException exception) {
            showHelp = true;
        }
        if (args.length < 2) {
            showHelp = true;
        } else {
            try {
                BufferedImage[] imageArr;
                if (Objects.equals(FilenameUtils.getExtension(args[0]), "gif")) {
                    // terrible check for a gif file
                    imageArr = GifReader.openGif(new File(args[0]), wipeFrames);
                } else {
                    imageArr = new BufferedImage[]{ImageIO.read(new File(args[0]))};
                }
                IMode[] im = new IMode[imageArr.length];
                Palette singlePalette = null;
                for (int i = 0; i < imageArr.length; i++) {
                    BufferedImage inputImage = imageArr[i];
                    if (!uncapResolution) {
                        final int MAX_WIDTH_HIGH = 102;
                        final int MAX_HEIGHT_HIGH = 57;
                        final int MAX_WIDTH_LOW = 51;
                        final int MAX_HEIGHT_LOW = 19;
                        if (inputImage.getWidth() > ((mode == IM_MODE.HD || mode == IM_MODE.HD_AUTO) ? MAX_WIDTH_HIGH
                                : MAX_WIDTH_LOW)) {
                            double scale = ((mode == IM_MODE.HD || mode == IM_MODE.HD_AUTO) ? MAX_WIDTH_HIGH
                                    : MAX_WIDTH_LOW) / (double) inputImage.getWidth();
                            System.out.println("Image is too wide, resizing! Was " + inputImage.getWidth() + " by " + inputImage.getHeight());
                            inputImage = Mode.scaleImage(inputImage, scale, scale);
                        }
                        if (inputImage.getHeight() > ((mode == IM_MODE.HD || mode == IM_MODE.HD_AUTO) ? MAX_HEIGHT_HIGH
                                : MAX_HEIGHT_LOW)) {
                            double scale = ((mode == IM_MODE.HD || mode == IM_MODE.HD_AUTO) ? MAX_HEIGHT_HIGH
                                    : MAX_HEIGHT_LOW) / (double) inputImage.getHeight();
                            System.out.println("Image is too tall, resizing! Was " + inputImage.getWidth() + " by " + inputImage.getHeight());
                            inputImage = Mode.scaleImage(inputImage, scale, scale);
                        }
                        System.out.println("Final resolution is " + inputImage.getWidth() + " by "
                                + inputImage.getHeight());
                    }
                    long startTime = System.nanoTime();
                    if (!autoSingle || i == 0) {
                        im[i] = switch (mode) {
                            case HD -> new ModeHighDensity(inputImage, palette, dither);
                            case LD -> new ModeLowDensity(inputImage, palette, dither);
                            case HD_AUTO -> new ModeHighDensity(inputImage, dither);
                            case LD_AUTO -> new ModeLowDensity(inputImage, dither);
                        };
                    } else {
                        im[i] = switch (mode) {
                            case HD, HD_AUTO -> new ModeHighDensity(inputImage, singlePalette, dither);
                            case LD, LD_AUTO -> new ModeLowDensity(inputImage, singlePalette, dither);
                        };
                    }
                    if (i == 0 && autoSingle) {
                        singlePalette = im[0].getPalette();
                    }
                    long endTime = System.nanoTime();
                    System.out.println("Quantized com.shrekshellraiser.image in " + (endTime - startTime) / 1000000.0f + "ms.");
                    if (savePostImage) {
                        startTime = System.nanoTime();
                        if (imageArr.length > 1) {
                            ImageIO.write(im[i].getImage(), FilenameUtils.getExtension(postImagePath),
                                    new File(insertBeforeFileEx(postImagePath, String.valueOf(i))));
                        } else {
                            ImageIO.write(im[i].getImage(), FilenameUtils.getExtension(postImagePath),
                                    new File(postImagePath));
                        }
                        endTime = System.nanoTime();
                        System.out.println("Wrote post com.shrekshellraiser.image in " + (endTime - startTime) / 1000000.0f + "ms.");
                    }
                }
                long startTime = System.nanoTime();
                long endTime;
                switch (filetype) {
                    case BIMG -> {
                        BIMG bimg = new BIMG(im);
                        if (imageArr.length > 1)
                            bimg.writeKeyValuePair("secondsPerFrame", secondsPerFrame);
                        bimg.save(args[1]);
                        endTime = System.nanoTime();
                        System.out.println("Wrote bimg in " + (endTime - startTime) / 1000000.0f + "ms.");
                    }
                    case BBF -> {
                        BBF bbf = new BBF(im);
                        bbf.save(args[1]);
                        endTime = System.nanoTime();
                        System.out.println("Wrote bbf in " + (endTime - startTime) / 1000000.0f + "ms.");
                    }
                    case NFP -> {
                        NFP nfp = new NFP(im);
                        nfp.save(args[1]);
                        endTime = System.nanoTime();
                        System.out.println("Wrote nfp in " + (endTime - startTime) / 1000000.0f + "ms.");
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (showHelp) {
            String header = "Convert an com.shrekshellraiser.image into an bimg file.\n\n";
            String footer = """
                    """;

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("BIMG <input> <output>",
                    header, options, footer, true);
        }
    }

    static String insertBeforeFileEx(String filename, String insert) {
        String modFilename = FilenameUtils.removeExtension(filename) + insert;
        if (!Objects.equals(FilenameUtils.getExtension(filename), ""))
            modFilename += "." + FilenameUtils.getExtension(filename);
        return modFilename;
    }

    enum IM_MODE {
        HD,
        LD,
        HD_AUTO,
        LD_AUTO
    }

    enum IM_FILETYPE {
        BIMG,
        BBF,
        NFP
    }


}
