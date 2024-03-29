package com.masongulu.formats;

import com.masongulu.ImageMakerGUI;
import com.masongulu.blit.BlitMap;
import com.masongulu.colors.Palette;
import com.masongulu.utils.Utils;

import java.io.File;
import java.io.IOException;

import static com.masongulu.colors.Palette.defaultPalette;

public class FormatBBF extends FormatBase {
    @Override
    public void save(BlitMap[] blitMaps, File file) {
        StringBuilder fileDataStringBuilder = new StringBuilder();
        fileDataStringBuilder.append("BLBFOR1\n").append(blitMaps[0].getWidth()).append("\n")
                .append(blitMaps[0].getHeight()).append("\n")
                .append(blitMaps.length).append("\n")
                .append(System.currentTimeMillis()).append("\n"); // This is supposed to be os.epoch("utc") in lua
        if (blitMaps[0].getPalette().equals(defaultPalette))
            fileDataStringBuilder.append("{\"author\": \"BIMG Generator " + ImageMakerGUI.VERSION + "\"}")
                    .append("\n"); // this is supposed to be a "meta" parameter
        else {
            fileDataStringBuilder.append("{\"palette\":["); // Open meta
            for (BlitMap frame : blitMaps) {
                fileDataStringBuilder.append("{");
                Palette palette = frame.getPalette();
                for (int i = 0; i < 16; i++) {
                    fileDataStringBuilder.append("\"").append(i).append("\":")
                            .append(palette.getColor(i));
                    if (i != 15)
                        fileDataStringBuilder.append(",");
                }
                fileDataStringBuilder.append("}");
                if (frame.getPalette().equals(blitMaps[blitMaps.length - 1].getPalette()))
                    break; // palette is the same as the last palette, should test for a single palette used for each frame
                if (frame != blitMaps[blitMaps.length - 1])
                    fileDataStringBuilder.append(",");
            }
            fileDataStringBuilder.append("],").append("\"author\": \"BIMG Generator " + ImageMakerGUI.VERSION + "\"").append("}\n");
        }
        for (BlitMap frame : blitMaps) {
            for (int line = 0; line < frame.getHeight(); line++) {
                String chars = frame.getCharacter(line);
                String FGChars = frame.getFG(line);
                String BGChars = frame.getBG(line);
                for (int ch = 0; ch < frame.getWidth(); ch++) {
                    int FG = Character.digit(FGChars.charAt(ch), 16);
                    int BG = Character.digit(BGChars.charAt(ch), 16);
                    fileDataStringBuilder.append(chars.charAt(ch));
                    fileDataStringBuilder.append((char) ((FG << 4) + BG));
                }
            }
        }
        try {
            Utils.writeToFile(file, Utils.stringToInt(fileDataStringBuilder.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "bbf";
    }
}
